package org.example.springboot.dto;

import lombok.Data;

@Data
public class ChatMessage {

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT;
    }

    private Role role;
    private String content;
    private String thinkingContent;
    private Integer thinkingDuration;

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }
}