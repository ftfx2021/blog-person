package org.example.springboot.model;

import org.example.springboot.config.AIModelProperties;

//调用目标
//它把候选配置（模型名、优先级、维度等）
// 和供应商配置（URL、API Key、端点路径等）打包在一起，
// 作为一次模型调用的完整上下文传递给供应商客户端
public record ModelTarget(
        String id,
        AIModelProperties.ModelCandidate candidate,
        AIModelProperties.ProviderConfig provider
) {

}
