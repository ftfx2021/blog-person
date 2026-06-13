package org.example.springboot.config;

import io.modelcontextprotocol.client.McpClient;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class ChatClientConfig {


    /**
     * 配置ChatMemory - 内存存储的会话记忆
     *
     * @return ChatMemory 内存存储的会话记忆实例
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(30) // 窗口最大消息数目，保留最近30条消息
                .build();
    }




    @Bean("open-ai")
    //硅基流动
    public ChatClient openAIChatClient(OpenAiChatModel openAiChatModel){
        return ChatClient.builder(openAiChatModel).defaultSystem(
            "你是一个专业的AI助手。重要规则：" +
            "1. 当要求返回JSON格式时，必须只返回有效的JSON对象，不添加任何解释文字、markdown标记或其他内容。" +
            "2. 确保JSON语法正确：字段名用双引号，字符串值用双引号，数组和对象格式正确。" +
            "3. 使用提供的工具获取数据时，严格按照指定的数据结构返回结果。"
        ).build();
    }


}
