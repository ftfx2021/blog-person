一、基本使用
1、实现一个Tool
public class DateTimeTools {

    @Tool(description = "获取当前时间")

    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "根据当前时间来设置闹铃，使用 ISO-8601 格式",name = "闹铃工具")
    void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

    @Tool(
            name = "getWeather",
            description = "根据城市名返回当天天气，如北京、上海",
            returnDirect = true
    )
    public String getWeather(String city) {
        return city + " 今天晴，25℃";
    }

}

字段	作用	不写会怎样	最佳实践
name	函数名，模型靠它识别工具	默认 = 方法名；同 class 里不能重名	起一个语义化、唯一的英文名，如 getWeather
description	函数说明，告诉模型“啥时候用、怎么用”	默认 = 方法名；极易导致模型用错或不用	用 1-2 句话写清用途、参数含义、边界条件
returnDirect	工具结果是否直接返回给用户（不再给模型润色）	默认 false，即先回模型再回用户	需要立即展示原始数据（如查余额）时设为 true
resultConverter	把工具返回的 Java 对象 → 字符串 的转换器	默认用 DefaultToolCallResultConverter	返回复杂对象时自定义 ToolCallResultConverter
解释returnDirect：
如果为true，模型不加任何修饰，比如问上海的天气，结果是：
<think>
好的，用户问的是“上海的天气”，我需要使用getWeather工具来获取天气信息。首先，确认用户提供的城市名是上海，然后调用getWeather函数，参数是city: "上海"。不需要其他工具，因为用户没有提到时间或闹铃相关的内容。直接返回天气数据即可。
</think>
"上海 今天晴，25℃"
如果为false
<think>
好的，用户现在问的是“上海的天气”。我需要先看看有哪些可用的工具。根据提供的工具列表，有一个getWeather函数，它需要城市名作为参数。用户明确提到了上海，所以应该调用这个函数，参数是城市“上海”。不需要其他工具，因为用户没有提到时间或闹铃相关的内容。确认参数是否正确，城市名是字符串类型，符合要求。所以直接调用getWeather，参数为{"city": "上海"}。
</think>

<think>
好的，用户之前询问了上海的天气，我调用了getWeather函数，现在得到了响应“上海 今天晴，25℃”。接下来我需要把结果用自然的中文告诉他。首先，确认信息是否正确，城市名是上海，天气晴，温度25摄氏度。然后，组织语言，保持简洁明了。可以加上一些友好的提示，比如注意防晒或者穿衣建议。不过用户没有特别提到需要建议，可能只需直接回复天气情况。检查是否有其他需求，用户可能只是想知道天气，所以保持回答简洁。最后，确保用词准确，比如“晴”和“25℃”是否正确显示。确认无误后，回复用户。
</think>

上海今天天气晴朗，气温25℃，适合外出活动，记得做好防晒哦！
2、调用Tool
    @GetMapping(value = "/with-memory", produces = "text/plain;charset=utf-8")
    public Flux<String> withMemory(@RequestParam String msg,
                                   @RequestParam(defaultValue = "default") String cid) {

        return chatClient.prompt()
                .user(msg)
                .tools(new DateTimeTools())
                .stream()
                .content();
    }

二、进阶使用
1、resultConverter
案例：
（1）实现转换器
public class WeatherResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        if (!(result instanceof DateTimeTools.Weather w)) {
            return result.toString();
        }
        return String.format(
            "城市：%s，温度：%.1f℃，湿度：%d%%，风速：%.1f m/s",
            w.city(), w.temperature(), w.humidity(), w.windSpeed());
    }
}
（2）Tool（不使用转换器）
    @Tool(
            name = "getWeather",
            description = "根据城市名查询实时天气，返回温度、湿度、风速",
            returnDirect = true        
//            resultConverter = WeatherResultConverter.class // 👈 指定转换器
    )
    public Weather getWeather(String city) {
        // 模拟返回
        return new Weather(city, 27.5, 65, 3.2);
    }
输出：
<think>
好的，用户问的是“北京的天气”，我需要调用获取天气的函数。首先，确认用户需要的是北京的实时天气情况，包括温度、湿度和风速。查看可用的工具，发现有一个getWeather函数，参数是城市名。所以，我需要调用这个函数，参数是北京。然后，确保参数正确，城市名是字符串类型，没有其他必填项。直接调用即可，不需要其他工具。最后，把结果返回给用户。
</think>

{"city":"北京","temperature":27.5,"humidity":65,"windSpeed":3.2}
（3）Tool（使用转换器）
   @Tool(
            name = "getWeather",
            description = "根据城市名查询实时天气，返回温度、湿度、风速",
            returnDirect = true,         // 仍让模型润色
          resultConverter = WeatherResultConverter.class // 👈 指定转换器
    )
    public Weather getWeather(String city) {
        // 模拟返回
        return new Weather(city, 27.5, 65, 3.2);
    }
输出：
<think>
好的，用户现在问的是“北京的天气”，我需要处理这个查询。首先，用户想要知道北京的天气情况，所以应该使用获取天气的工具。查看可用的工具，有一个getWeather函数，参数是城市名，所以需要调用这个函数，并传入北京作为城市参数。接下来，确认参数是否正确，城市名是字符串类型，符合要求。然后，不需要使用其他工具，比如闹钟或获取当前时间，因为用户的需求明确是天气。所以直接调用getWeather，参数为{"city": "北京"}。确保JSON格式正确，没有语法错误。最后，生成对应的tool_call结构。
</think>

城市：北京，温度：27.5℃，湿度：65%，风速：3.2 m/s
2、@Tool + @ToolParam：给模型看的“函数说明书”

public enum Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
@Tool(description = "把英文星期转换成中文",returnDirect = true)
    public String weekdayToChinese(
            @ToolParam(description = "英文星期枚举,必须是大写英文星期单词,严格匹配，否则拒绝使用该工具") Weekday weekday) {
        System.out.println("调用工具" + weekday);
        return Map.of(
                Weekday.MONDAY, "星期一",
                Weekday.TUESDAY, "星期二",
                Weekday.WEDNESDAY,"星期三",
                Weekday.THURSDAY,"星期四",
                 Weekday.FRIDAY, "星期五",
                Weekday.SATURDAY,"星期六",
                Weekday.SUNDAY, "星期日"
        ).get(weekday);
    }
输出：
<think>
好的，用户让我把“sunday1”转成中文。首先，我需要看看有没有合适的工具。提供的工具里有一个是weekdayToChinese，用来把英文星期转成中文，参数是weekday，要求必须是大写的英文星期单词，而且严格匹配枚举里的值。

用户给的输入是“sunday1”，看起来像是拼写错误或者格式不对。正确的英文星期应该是像MONDAY、TUESDAY这样的大写形式，而且后面没有数字。用户可能误写成了小写或者加了数字，比如“sunday1”可能应该是“SUNDAY”。

这时候，我需要判断是否符合工具的使用条件。根据工具的要求，必须是大写的英文单词，且严格匹配枚举中的值。而“sunday1”既不是大写，又包含数字，不符合条件。所以不能使用这个工具来转换。

接下来，我应该考虑如何回应用户。可能需要指出输入不符合要求，并给出正确的格式示例。比如，正确的输入应该是“SUNDAY”，然后工具才会返回对应的中文“星期日”。同时，用户可能有拼写错误，或者对工具的使用方法不清楚，需要明确说明要求。

另外，检查是否有其他工具可能适用，但其他工具比如getCurrentDateTime、getWeather、闹铃工具都不涉及星期转换，所以只能使用weekdayToChinese。但因为输入不符合条件，所以无法调用该工具，必须拒绝使用，并提示用户正确输入。
</think>

<sunday1> 不符合要求，英文星期必须是严格的大写枚举值（如 SUNDAY），且不能包含数字或其它字符。正确的输入应为 "SUNDAY"，对应的中文是 "星期日"。
3、怎样给工具（@Tool 方法）的参数写说明书
● ● description：参数的描述，模型可以利用它来更好地理解如何使用该参数。例如，参数应该采用什么格式、允许哪些值等。
● required：参数是必需的还是可选的。默认情况下，所有参数都被视为必需的。
    public enum Unit { DAYS, WEEKS, MONTHS }
    @Tool(description = "在当前日期上加/减时间，默认加 1 天")
    public String addDate(
            @ToolParam(description = "步长，正数加、负数减") int step,
            @ToolParam(description = "单位，默认 DAYS", required = false) Optional<String> unitStr) {

        Unit unit = unitStr
                .map(String::toUpperCase)
                .map(Unit::valueOf)
                .orElse(Unit.DAYS);

        LocalDate result = switch (unit) {
            case DAYS   -> LocalDate.now().plusDays(step);
            case WEEKS  -> LocalDate.now().plusWeeks(step);
            case MONTHS -> LocalDate.now().plusMonths(step);
        };
        return result.format(DateTimeFormatter.ISO_DATE);
    }
4、MethodToolCallback：不用 @Tool 注解，也能把任意 Java 方法变成 AI 可调用的工具
（1）基本使用
class DateTimeTools {

    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}

@Bean
public ChatClient chatClient(OllamaAlibabaChatModel ollamaChatModel) {
//注意ReflectionUtils在org.springframework.util.ReflectionUtils;包里
Method method= ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()

.toolDefinition(ToolDefinitions.builder(method)
                //                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .description("获取当前时间")
                .name("获取当前时间")
                .build())
.toolMethod(method)
.toolObject(new DateTimeTools())


.build();
return ChatClient.builder(ollamaChatModel)
.defaultAdvisors(
    new SimpleLoggerAdvisor()

)
.defaultToolCallbacks(toolCallback)
.build();


}
（2）静态方法
class DateTimeTools {

 static   String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}

@Bean
public ChatClient chatClient(OllamaAlibabaChatModel ollamaChatModel) {
//注意ReflectionUtils在org.springframework.util.ReflectionUtils;包里
Method method= ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()

.toolDefinition(ToolDefinitions.builder(method)
                //                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .description("获取当前时间")
                .name("获取当前时间")
                .build())
.toolMethod(method)
// .toolObject(new DateTimeTools()) 无需.toolObject


.build();
return ChatClient.builder(ollamaChatModel)
.defaultAdvisors(
    new SimpleLoggerAdvisor()

)
.defaultToolCallbacks(toolCallback)
.build();


}
（3）添加到ChatModel或者特定的聊天请求
    @GetMapping(value = "/with-memory", produces = "text/plain;charset=utf-8")
    public Flux<String> withMemory(@RequestParam String msg,
                                   @RequestParam(defaultValue = "default") String cid) {


        Method method= ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
        ToolCallback toolCallback = MethodToolCallback.builder()

                .toolDefinition(ToolDefinitions.builder(method)
//                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                        .description("获取当前时间")
                        .name("获取当前时间")
                        .build())
                .toolMethod(method)
                .toolObject(new DateTimeTools())
                .build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder().toolCallbacks(toolCallback).build();
        Prompt prompt = new Prompt("现在几点", chatOptions);
        return chatClient.prompt(prompt)
                .user(msg)
                .stream()
                .content();
    }
}
    @GetMapping(value = "/with-memory", produces = "text/plain;charset=utf-8")
    public Flux<String> withMemory(@RequestParam String msg,
                                   @RequestParam(defaultValue = "default") String cid) {


        Method method= ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
        ToolCallback toolCallback = MethodToolCallback.builder()

                .toolDefinition(ToolDefinitions.builder(method)
//                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                        .description("获取当前时间")
                        .name("获取当前时间")
                        .build())
                .toolMethod(method)
                .toolObject(new DateTimeTools())
                .build();

        return chatClient.prompt()
                .user(msg)
                .toolCallbacks(toolCallback)
                .stream()
                .content();
    }
}
5、工具上下文
@Component
public class DateTimeTools {
    @Resource
    ResourceCategoryService resourceCategoryService;

    @Tool(description = "根据资源分类名称获取资源分类(无参数)")
    public ResourceCategory getByName(ToolContext context) {
        String name = (String) context.getContext().get("name");
        System.out.println("分类名称:"+name);
        return resourceCategoryService.getByName(name);

    }
}

    @Autowired
    DateTimeTools dateTimeTools; //用 Spring 注入的 Bean
    @GetMapping(value = "/with-memory", produces = "text/plain;charset=utf-8")
    public Flux<String> withMemory(@RequestParam String msg,
                                   @RequestParam(defaultValue = "default") String cid) {


        return chatClient.prompt()
                .user(msg)
//                .tools(new DateTimeTools())//不要new，否则注入时机早于 Spring 容器完成初始化-》ResourceCategoryService为空
                .tools(dateTimeTools)
                .toolContext(Map.of("name","Vue.js"))
                .stream()
                .content();
    }

6、用户控制的工具执行
 @GetMapping(value = "/with-memory", produces = "text/plain;charset=utf-8")
    public Flux<String> withMemory(@RequestParam String msg,
                                   @RequestParam(defaultValue = "default") String cid) {
        Method method= ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
        ToolCallback toolCallback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinitions.builder(method)
                        .description("获取时间")
                        .name("获取时间")
                        .build())
                .toolMethod(method)
                .toolObject(new DateTimeTools()).build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder().toolCallbacks(toolCallback).internalToolExecutionEnabled(false).build();
        Prompt prompt = Prompt.builder().chatOptions(chatOptions).messages(new UserMessage("你好，获取一下当前时间")).build();

        return Flux.defer(()->{
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            if(!chatResponse.hasToolCalls()){
                return Flux.just(chatResponse.getResult().getOutput().getText());
            }
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            Prompt prompt1 = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
            chatResponse=chatClient.prompt(prompt1).call().chatResponse();
            return Flux.just(chatResponse.getResult().getOutput().getText());


        });
    }
}

● 设 internalToolExecutionEnabled=false 以后，框架只“发现工具”，不负责“执行工具”。
● 想让它真正跑起来，就要在代码里显式调用 toolCallingManager.executeToolCalls(...) 或者自己反射执行。