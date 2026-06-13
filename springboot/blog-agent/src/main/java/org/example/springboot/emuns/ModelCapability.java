package org.example.springboot.emuns;


import lombok.Data;
import lombok.Getter;

@Getter
public enum ModelCapability {
    CHAT("chat"),
    EMBEDDED("embedded"),
    IMAGE_GEN("imageGen"),
    RERANK("rerank"),;
    private String displayName;
    private ModelCapability() {
    }
    private ModelCapability(String displayName) {
        this.displayName = displayName;
    }
}
