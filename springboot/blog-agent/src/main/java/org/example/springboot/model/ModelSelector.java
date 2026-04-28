package org.example.springboot.model;

import org.example.springboot.config.AIModelProperties;

//模型选择器，职责是从配置中选出当前可用的候选模型列表。
public record ModelSelector(
        String  id,
        AIModelProperties.ModelCandidate candidate,
        AIModelProperties.ProviderConfig provider

) {


}

