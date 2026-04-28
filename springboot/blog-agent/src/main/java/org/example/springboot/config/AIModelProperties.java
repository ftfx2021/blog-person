package org.example.springboot.config;


import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConditionalOnProperty(prefix = "ai")
//YAML 和 Java 类是一一对应的
public class AIModelProperties {
    //<供应商，模型配置>
    private Map<String, ProviderConfig> providers = new HashMap<>();
    private ModelGroup chat = new ModelGroup();
    private ModelGroup embedding = new ModelGroup();
    private ModelGroup rerank = new ModelGroup();
    private Selection selection = new Selection();
    private Stream stream = new Stream();

    @Data
    public static class ProviderConfig {
        private String url;
        private String apiKey;
        private Map<String, String> endpoints = new HashMap<>();
    }
    @Data
    public static class ModelGroup {
        private String defaultModel;
        private String deepThinkingModel;
        private List<ModelCandidate> candidates = new ArrayList<>();
    }
    @Data
    public static class ModelCandidate {
        private String id;
        private String provider;
        private String model;
        private String url;
        private Integer dimension;
        private Integer priority = 100;
        private Boolean enabled = true;
        private Boolean supportsThinking = false;
    }
    @Data
    public static class Selection {
        private Integer failureThreshold = 2;
        private Long openDurationMs = 30000L;
    }

    @Data
    public static class Stream {
        private Integer messageChunkSize = 5;
    }




}
