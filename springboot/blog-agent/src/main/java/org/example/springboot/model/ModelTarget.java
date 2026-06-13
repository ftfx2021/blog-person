package org.example.springboot.model;

import org.example.springboot.config.AIModelProperties;

/**
 *调用目标  - 一次完整调用的所用的模型配置信息
 * @param id 模型id
 * @param candidate 对应模型的信息
 * @param provider 对应模型的供应商
 */

public record ModelTarget(
        String id,
        AIModelProperties.ModelCandidate candidate,
        AIModelProperties.ProviderConfig provider
) {

}
