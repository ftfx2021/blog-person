package org.example.springboot.rerank;


import lombok.extern.slf4j.Slf4j;

import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.framework.RetrievedChunk;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.model.ModelTarget;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class RoutingRerankService implements RerankService{
    private final ModelSelector modelSelector;
    private final ModelRoutingExecutor executor;
    private final Map<String,RerankClient> rerankClientByProvider;

    public RoutingRerankService(ModelSelector modelSelector, ModelRoutingExecutor executor, List<RerankClient> clientList) {
        this.modelSelector = modelSelector;
        this.executor = executor;
        rerankClientByProvider=clientList.stream()
                .collect(Collectors
                        .toMap(RerankClient::provider, Function.identity()));
    }


    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN) {
        return executor.executeWithFallback(
                ModelCapability.RERANK,
                modelSelector.selectRerankCandidates(),
               this::resolveClient,
                (client,target)->client.rerank(query,candidates,topN,target)
        );
    }

    /**
     * 解析、提取客户端
     * @param target 模型目标
     * @return 客户端信息
     */
    private RerankClient resolveClient(ModelTarget target) {
        RerankClient client = rerankClientByProvider.get(target.candidate().getProvider());
        if (client == null) {
            log.warn("{} 提供商客户端缺失: provider：{}，modelId：{}",
                    ModelCapability.RERANK, target.candidate().getProvider(), target.id());
        }
        return client;
    }
}
