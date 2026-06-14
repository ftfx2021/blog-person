package org.example.springboot.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;


/*针对 OpenAI 兼容接口优化的 data-only SSE 消费器
在 Spring Boot 开发中，看到方法参数里传 Gson、ObjectMapper 或 RestTemplate，
通常都是为了 复用单例对象、提高性能 以及 保证全局配置统一。
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class OpenAIStyleSseParser {

    private static final String DATA_PREFIX = "data:";

    private static  final String DONE_MARKER = "[DONE]";

    /**
     * 流式响应的标准路径是 choices[0].delta.content
     * 在大模型的流式返回（SSE）中，数据是一行一行推过来的。实际从网络层收到的一行数据（也就是你的 line 变量）长这样：
     * data: {"choices":[{"delta":{"content":" 你好，这是空格"}}]} \r\n
     * SSE 协议层：开头的 data: 和结尾的回车换行 \r\n。
     * JSON 结构层：外面的大括号 {} 和里面的键名 "choices", "delta", "content"。
     * 数据值层：" 你好，这是空格" —— 模型真正生成的空格，是被双引号 "" 紧紧包裹住的！
     * @param line 原始输出
     * @param gson gson Gson 对象的创建是一个重量级操作。它在初始化时需要扫描类信息、建立缓存、配置序列化策略等
     * 如果你在 parseLine 内部写 Gson gson = new Gson()，那么每回答一个问题，系统就要创建并销毁 1000 个 Gson 对象。
     * 这会产生大量的内存碎片，导致频繁的 GC（垃圾回收），进而导致系统卡顿、响应变慢。
     * 在外部（比如 Spring 的 Service 里）注入一个单例的 Gson 注入进来，这 1000 次调用共享同一个实例，几乎没有额外开销。
     * @param reasoningEnabled 是否启用推理
     * @return ParsedEvent
     */
    static ParsedEvent parseLine(String line, Gson gson, boolean reasoningEnabled){
        //null 或空白行直接返回 ParsedEvent.empty()
        if(line==null || line.isBlank())
        {
            return ParsedEvent.empty();
        }
        //trim() 在这里是为了清理 HTTP 网络传输中产生的不可见字符（如 \r\n) 不影响模型原始输出
        String payload = line.trim();
        //剥离data:前缀
        if(payload.startsWith(DATA_PREFIX)){
            payload = payload.substring(DATA_PREFIX.length()).trim();
        }
        //识别[DONE]标记 ：OpenAI 协议约定的流结束标记，返回 ParsedEvent.done()
        if(DONE_MARKER.equalsIgnoreCase(payload)){
            return ParsedEvent.done();
        }
        //正式开始解析JSON ：把 payload 解析为 JsonObject，提取 choices[0]
        JsonObject object = gson.fromJson(payload, JsonObject.class);
        //拿choices
        JsonArray choices = object.getAsJsonArray("choices");
        if(choices==null || choices.isEmpty()){
            return ParsedEvent.empty();
        }
        //提取字段 ：从 choice[0] 中提取 content（增量内容）和 reasoning_content（思考内容，如果启用），检查 finish_reason 判断是否完成
        JsonObject choice0 = choices.get(0).getAsJsonObject();
        String content = extractText(choice0,"content");

        String reasoning = reasoningEnabled ? extractText(choice0,"reasoning_content") : null;
        /*
        模型停止生成 token 的原因。如果模型达到了自然停止点或提供了停止序列，则为 stop；
        如果达到了请求中指定的全局最大 token 数，则为 length；
        如果由于我们的内容过滤器标记而省略了内容，则为 content_filter；
        如果模型调用了工具，则为 tool_calls；如果模型调用了函数（已弃用），则为 function_call。
         */
        boolean completed = hasFinishReason(choice0);
        return new ParsedEvent(content, reasoning, completed);


    }

    /**
     * 提取choice0中的信息
     * @param choice  choice0
     * @param fieldName choice0中的JSON key
     * @return 提取后的内容
     */
    private static String extractText(JsonObject choice, String fieldName) {
        if(choice==null){
            return null;
        }
        //路径一：从 delta 中提取（流式标准路径）
        if(choice.has("delta")&&choice.get("delta").isJsonObject()){
            JsonObject delta = choice.get("delta").getAsJsonObject();
            if(delta.has(fieldName)){
                JsonElement value = delta.get(fieldName);
                if(value!=null&&!value.isJsonNull()){
                    return value.getAsString();
                }
            }
        }
        //路径二：从 message 中提取（兼容非标准行为）
        if(choice.has("message")&&choice.get("message").isJsonObject()){
            JsonObject message = choice.get("message").getAsJsonObject();
            if(message.has(fieldName)){
                JsonElement value = message.get(fieldName);
                if(value!=null&&!value.isJsonNull()){
                    return value.getAsString();
                }
            }
        }
        return null;
    }

    /**
     * 判断是否有结束标志
     * @param choice choice0负载
     * @return 是否有结束标志
     */
    private static boolean hasFinishReason(JsonObject choice){
        if(choice==null||!choice.has("finish_reason")){
            return false;

        }
        JsonElement finishReason = choice.get("finish_reason");
        return finishReason!=null&&!finishReason.isJsonNull();
    }

    /**
     * 定义 ParsedEvent（解析事件对象）是出于 代码质量、业务抽象和调用便利性 的考虑。如果不定义它，你的代码会变得非常难以维护。
     * @param content 普通文本
     * @param reasoning 思考过程
     * @param completed 结束标志
     */
    public record ParsedEvent(String content,String reasoning,boolean completed){
        static ParsedEvent empty(){
            return new ParsedEvent(null,null,false);
        }
        static ParsedEvent done(){
            return new ParsedEvent(null,null,true);
        }
        boolean hasContent(){
            return content != null&&!content.isEmpty();
        }
        boolean hasReasoning(){
            return reasoning != null&&!reasoning.isEmpty();
        }
    }


}
