package org.example.springboot.model;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.config.AIModelProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 模型选择器
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    //配置
    private  final AIModelProperties properties;
    //熔断器
    private  final ModelHealthStore  healthStore;

    /**
     * 入口方法：选择聊天模型
     * @param deepthinking 是否深度思考
     * @return 返回的可用模型
     */
    public List<ModelTarget> selectChatModel(boolean deepthinking){
        AIModelProperties.ModelGroup chatGroup = properties.getChat();
        if(chatGroup == null){
            return List.of();
        }
        String firstModelId = resolveFirstChoiceModel(chatGroup,deepthinking);

        return selectCandidates(chatGroup,firstModelId,deepthinking);

    }

    /**
     * 入口方法：选择嵌入模型
     * @return 返回的可用模型
     */
    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(properties.getEmbedding());
    }

    /**
     * 入口方法：选择重排序模型
     * @return 返回的可用模型
     */
    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(properties.getRerank());
    }


    /**
     *  重载方法，选择候选模型
     * @param modelGroup 模型组，包含一系列候选模型
     * @return  返回的可用模型
     */
    public List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup modelGroup){
        if(modelGroup == null){
            return List.of();
        }

        return selectCandidates(modelGroup,modelGroup.getDefaultModel(),false);


    }

    /**
     * 候选模型主方法，主要用来排序模型
     * @param modelGroup 模型组
     * @param firstChoiceModelId 首选模型id
     * @param deepthinking 是否深度思考
     * @return 候选模型完整信息
     */
    public List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup modelGroup,String firstChoiceModelId,boolean deepthinking){
        if(modelGroup == null||modelGroup.getCandidates() == null){
            return List.of();
        }

        List<AIModelProperties.ModelCandidate> candidates = modelGroup.getCandidates();
        List<AIModelProperties.ModelCandidate> modelCandidateList = candidates.stream()
                .filter(c -> c != null)  //不要空的
                .filter(c -> c.getEnabled()) //不要没启用的
                .filter(c -> !deepthinking || (deepthinking && c.getSupportsThinking()))  //如果是深度思考的，则选择支持深度思考的。如果不需要深度思考，那么此筛选跳过
                .sorted(
                        Comparator.comparing(
                                        (AIModelProperties.ModelCandidate c) -> !resolveId(c).equals(firstChoiceModelId)  //首选模型排第一
                                )
                                .thenComparing(AIModelProperties.ModelCandidate::getPriority, Comparator.nullsLast(Integer::compareTo)) //其次根据模型优先级排序
                                .thenComparing(AIModelProperties.ModelCandidate::getId, Comparator.nullsLast(String::compareTo))  //如果优先级相同。那么根据模型id排序
                ).toList();
        if(deepthinking&&modelCandidateList.isEmpty()){
            log.warn("深度思考模式没有可用候选模型");
        }
        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();

        //构建完整的模型候选信息
        return modelCandidateList.stream().map(
                candidate ->
                        buildModelTarget(candidate, providers)
        ).filter(Objects::nonNull).toList();


    }

    /**
     * 根据某个模型组确定首选模型
     * 如果开了深度思考，那么选择的是对应模型组的深度思考模型
     * 如果没开或者没匹配到深度思考模型，则返回对应模型组的默认模型
     * @param group 对应模型组
     * @param deepThinking 是否深度思考
     * @return  模型id
     */
    private String resolveFirstChoiceModel(AIModelProperties.ModelGroup group, boolean deepThinking) {
        if (deepThinking) {
            String deepModel = group.getDeepThinkingModel();
            if (StringUtils.isNotBlank(deepModel)) {
                return deepModel;
            }
        }
        return group.getDefaultModel();
    }

    /**
     * 构建完整的模型信息
     * @param candidate 单一模型候选信息
     * @param providers 所有供应商
     * @return 模型完整信息
     */
    public ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, Map<String,AIModelProperties.ProviderConfig> providers){
        String modelId = resolveId(candidate);

        //检查熔断状态
        if(healthStore.isUnavailable(modelId)){
            return null;
        }

        //根据供应商名称获取供应商信息
        AIModelProperties.ProviderConfig providerConfig = providers.get(candidate.getProvider());
        if(providerConfig == null){
            log.warn("Provider配置缺失: provider={}, modelId={}",
                    candidate.getProvider(), modelId);
            return null;
        }
        return new ModelTarget(modelId,candidate,providerConfig);

    }


    /**
     * 辅助方法，模型id解析：如果配置了 id 字段，直接用它；如果没配，自动合成 "provider::model" 格式。
     * @param candidate 候选模型
     * @return 模型id
     */
    public String resolveId(AIModelProperties.ModelCandidate  candidate){
        if(candidate == null){
            return null;
        }
        if(candidate.getId()!=null)return candidate.getId();

        return String.format("%s::%s",candidate.getProvider(),candidate.getModel());
    }


}
