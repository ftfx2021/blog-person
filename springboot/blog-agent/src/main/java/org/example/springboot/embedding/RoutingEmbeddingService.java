package org.example.springboot.embedding;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.exception.RemoteException;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.model.ModelTarget;
import org.example.springboot.rerank.RerankClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoutingEmbeddingService implements EmbeddingService{
    private final ModelSelector modelSelector;
    private final ModelRoutingExecutor modelRoutingExecutor;
    private final Map<String,EmbeddingClient> clientsByProvider;


    public RoutingEmbeddingService(ModelSelector modelSelector, ModelRoutingExecutor modelRoutingExecutor,List<EmbeddingClient> embeddingClients) {
        this.modelSelector = modelSelector;
        this.modelRoutingExecutor = modelRoutingExecutor;

        this.clientsByProvider = embeddingClients.stream()
                .collect(Collectors
                        .toMap(EmbeddingClient::provider, Function.identity()));
    }

    @Override
    public List<Float> embed(String text) {
        return modelRoutingExecutor.executeWithFallback(
                ModelCapability.EMBEDDED,
                modelSelector.selectEmbeddingCandidates(),
                this::resolveClient,
                (client, target) ->
                    client.embed(text,target)




        );
    }

    @Override
    public List<Float> embed(String text, String modelId) {
        return modelRoutingExecutor.executeWithFallback(
                ModelCapability.EMBEDDED,
                List.of(resolveTarget(modelId)),
                this::resolveClient,
                (client, target) ->client.embed(text,target)
        );
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        return modelRoutingExecutor.executeWithFallback(
                ModelCapability.EMBEDDED,
                modelSelector.selectEmbeddingCandidates(),
                this::resolveClient,
                (client, target) -> client.embedBatch(texts,target));
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, String modelId) {
        return modelRoutingExecutor.executeWithFallback(
                ModelCapability.EMBEDDED,
                List.of(resolveTarget(modelId)),
                this::resolveClient,
                (client, target) -> client.embedBatch(texts,target)
        );
    }

    /**
     * 解析、提取客户端
     * @param target 模型目标
     * @return 客户端信息
     */
    private EmbeddingClient resolveClient(ModelTarget target) {
        EmbeddingClient client = clientsByProvider.get(target.candidate().getProvider());
        if (client == null) {
            log.warn("{} 提供商客户端缺失: provider：{}，modelId：{}",
                    ModelCapability.EMBEDDED.getDisplayName(), target.candidate().getProvider(), target.id());
        }
        return client;
    }

    private ModelTarget resolveTarget(String modelId) {
       if(StringUtils.isEmpty(modelId)) {
           throw new RemoteException("Embedding 模型ID不能为空");
       }
       return modelSelector.selectEmbeddingCandidates()
               .stream()
               .filter(target -> target.id().equals(modelId))
               .findFirst()
               .orElseThrow(() -> new RemoteException("Embedding 模型不可用: " + modelId));
    }
}
