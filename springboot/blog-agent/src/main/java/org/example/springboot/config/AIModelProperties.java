package org.example.springboot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai")
//YAML 和 Java 类是一一对应的
public class AIModelProperties {
    //<供应商名称，供应商配置>
    private Map<String, ProviderConfig> providers = new HashMap<>();
    //聊天模型组
    private ModelGroup chat = new ModelGroup();
    //嵌入模型组
    private ModelGroup embedding = new ModelGroup();
    //重排序模型组
    private ModelGroup rerank = new ModelGroup();
    //熔断策略
    private Selection selection = new Selection();
    //流式输出配置
    private Stream stream = new Stream();

    @Data
    //模型供应商配置
    public static class ProviderConfig {
        private String url;
        private String apiKey;
        //模型端点配置，如chat模型的端点是/v1/chat/completions
        private Map<String, String> endpoints = new HashMap<>();
    }
    //模型组
    @Data
    public static class ModelGroup {
        //默认模型id
        private String defaultModel;
        //默认深度思考模型
        private String deepThinkingModel;
        //全部后端模型
        private List<ModelCandidate> candidates = new ArrayList<>();
    }
    //全部模型
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
    //熔断策略
    @Data
    public static class Selection {
        //失败次数达到的阈值，大于等于此阈值则熔断
        private Integer failureThreshold = 2;
        //熔断持续时间
        private Long openDurationMs = 30000L;
    }

    @Data
    public static class Stream {
        private Integer messageChunkSize = 5;
    }




}
