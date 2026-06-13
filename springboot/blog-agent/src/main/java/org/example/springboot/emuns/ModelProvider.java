package org.example.springboot.emuns;

import lombok.Getter;

@Getter
public enum ModelProvider {
    OLLAMA("ollama","ollama"),
    SILICONFLOW("硅基流动","siliconflow"),
    DEEPSEEK("深度求索","deepseek"),
    /**
     * 阿里云百炼大模型平台
     */
    BAI_LIAN("阿里百炼","bailian");
    final String name;
    final String id;
    ModelProvider(String name, String id) {
        this.name = this.name();
        this.id = this.name();
    }

}
