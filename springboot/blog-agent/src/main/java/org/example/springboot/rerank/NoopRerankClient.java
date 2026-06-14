package org.example.springboot.rerank;

import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.framework.RetrievedChunk;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
public class NoopRerankClient implements RerankClient{
    /*
        不是所有部署环境都有 Rerank 模型可用。
        Rerank 是公有云 API 服务（百炼的 gte-rerank），需要付费。
        如果用户没有买百炼的 Rerank 服务，或者在本地开发环境（只有 Ollama）跑项目，没有 Rerank 模型。
        这时候系统应该怎么办？如果直接报错“没有可用的 Rerank 模型”，用户连问答功能都用不了——而 Rerank 只是检索阶段的一个优化环节，不是核心必需。
        没有 Rerank，用粗排（向量检索）的结果截断一下也能用，只是精度差一些。
     */
    @Override
    public String provider() {
        return ModelProvider.NOOP.getId();
    }

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidate, Integer topN, ModelTarget target) {
        if(candidate==null|candidate.isEmpty()){
            return List.of();
        }
        if(topN<=0||candidate.size()<=topN){
            return candidate;
        }
        return candidate.stream().limit(topN).collect(Collectors.toList());
    }
}
