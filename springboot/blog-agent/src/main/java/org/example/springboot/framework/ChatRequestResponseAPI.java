package org.example.springboot.framework;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
/*
 *适用于ResponseAPIAPI
 * 使用input取代messages
 * 返回值通过 response.output_text
 * 具体参考https://developers.openai.com/api/reference/resources/responses/methods/create
 */
public class ChatRequestResponseAPI {
    /**
     *     model="Qwen3-30B-A3B-Instruct-2507-FP8",
     *     input=[
     *         {"role": "system", "content": "你是一个有帮助的助手。"},
     *         {"role": "user", "content": "你好！"}
     *     ]
     */
    @Builder.Default
    //：保住你在声明变量时赋的初始值，不让它被 Builder 模式“吃掉”变成 null。
    //如果为空返回的是[]而不是null
    private List<ChatMessage> input = new ArrayList<>();  //使用input取代messages

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Boolean thinking;
    private Boolean enableTools;
}
