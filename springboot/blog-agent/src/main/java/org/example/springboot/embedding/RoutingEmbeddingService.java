package org.example.springboot.embedding;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.exception.RemoteException;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.model.ModelTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                (client, target) -> client.embedBatch(texts,target))
        ;
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

    private EmbeddingClient resolveClient(ModelTarget target) {
        return clientsByProvider.get(target.candidate().getProvider());
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
