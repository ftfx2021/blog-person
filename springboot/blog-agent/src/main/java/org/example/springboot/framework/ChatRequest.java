package org.example.springboot.framework;


import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
/*
 *适用于Chat Completions API
 */
public class ChatRequest {
    /**
     * 使用示例：
     * ChatRequest request = ChatRequest.builder()
     *         .messages(List.of(
     *                 ChatMessage.system("你是一个电商客服助手，请根据提供的知识回答用户问题。"),
     *                 ChatMessage.user("AirPods Pro 2 的保修期是多久？")
     *         ))
     *         .temperature(0.1)
     *         .build();
     */
    @Builder.Default
    //：保住你在声明变量时赋的初始值，不让它被 Builder 模式“吃掉”变成 null。
    //如果为空返回的是[]而不是null
    private List<ChatMessage> messages = new ArrayList<>();

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Boolean thinking;
    private Boolean enableTools;
}