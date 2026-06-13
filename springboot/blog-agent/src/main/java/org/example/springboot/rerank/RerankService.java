package org.example.springboot.rerank;

import org.example.springboot.framework.RetrievedChunk;

public interface RerankService {
    /**
     * 对候选文档进行精排，按相关度重新排序，返回前 topN 条
     *
     * @param query      用户问题
     * @param candidates 向量检索召回的候选文档
     * @param topN       最终保留的条数
     * @return 精排后的前 topN 条文档
     */
    List<RetrievedChunk> retrieveChunks(String query,List<RetrievedChunk> candidates,int topN);

}
