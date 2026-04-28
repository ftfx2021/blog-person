package org.example.springboot.selector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.model.ModelHealthStore;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    private  final AIModelProperties properties;
    private  final ModelHealthStore  healthStore;

    public List<ModelTarget> selectChatModel(boolean deepthinking){
        AIModelProperties.ModelGroup chatGroups = properties.getChat();
        if(chatGroups == null){
            return List.of();
        }

        return selectCandidates(properties.getRerank(),chatGroups.getDefaultModel(),deepthinking);

    }

    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(properties.getEmbedding());
    }

    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(properties.getRerank());
    }



    public List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup modelGroup){
        if(modelGroup == null){
            return List.of();
        }

        return selectCandidates(modelGroup,modelGroup.getDefaultModel(),false);


    }

    public List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup modelGroup,String defaultModelId,boolean deepthinking){
        if(modelGroup == null||modelGroup.getCandidates() == null){
            return List.of();
        }

        List<AIModelProperties.ModelCandidate> candidates = modelGroup.getCandidates();
        List<AIModelProperties.ModelCandidate> modelCandidateList = candidates.stream()
                .filter(c -> c != null)
                .filter(c -> c.getEnabled())
                .filter(c -> !deepthinking || (deepthinking && c.getSupportsThinking()))
                .sorted(
                        Comparator.comparing(
                                        (AIModelProperties.ModelCandidate c) -> !resolveId(c).equals(defaultModelId)
                                )
                                .thenComparing(AIModelProperties.ModelCandidate::getPriority, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(AIModelProperties.ModelCandidate::getId, Comparator.nullsLast(String::compareTo))
                ).collect(Collectors.toList());
        if(deepthinking&&modelCandidateList.isEmpty()){
            log.warn("深度思考模式没有可用候选模型");
        }
        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();

        return modelCandidateList.stream().map(
                candidate ->
                        buildModelTarget(candidate, providers)
        ).filter(candidate -> candidate == null).toList();


    }

    public ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, Map<String,AIModelProperties.ProviderConfig> providers){
        String modelId = resolveId(candidate);

        //TODO:检查熔断状态

        AIModelProperties.ProviderConfig providerConfig = providers.get(candidate.getProvider());
        if(providerConfig == null){
            log.warn("Provider配置缺失: provider={}, modelId={}",
                    candidate.getProvider(), modelId);
            return null;
        }
        return new ModelTarget(modelId,candidate,providerConfig);

    }



    public String resolveId(AIModelProperties.ModelCandidate  candidate){
        if(candidate == null){
            return null;
        }
        if(candidate.getId()!=null)return candidate.getId();

        return String.format("%s::%s",candidate.getPriority(),candidate.getModel());
    }


}
