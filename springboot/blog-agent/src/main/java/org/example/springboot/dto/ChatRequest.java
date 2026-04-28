package org.example.springboot.dto;


import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data

public class ChatRequest {

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Boolean thinking;
    private Boolean enableTools;
}