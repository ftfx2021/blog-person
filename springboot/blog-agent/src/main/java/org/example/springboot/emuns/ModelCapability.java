package org.example.springboot.emuns;


import lombok.Data;
import lombok.Getter;

@Getter
public enum ModelCapability {
    CHAT("chat", "chat"),
    EMBEDDED("embedded", "embedding"),
    IMAGE_GEN("imageGen", "image-gen"),
    RERANK("rerank", "rerank");

    private final String displayName;
    private final String endpointKey;

    ModelCapability(String displayName, String endpointKey) {
        this.displayName = displayName;
        this.endpointKey = endpointKey;
    }
}
