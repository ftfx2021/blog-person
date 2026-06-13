package org.example.springboot.rerank;

import org.example.springboot.framework.RetrievedChunk;
import org.example.springboot.model.ModelTarget;

import java.util.List;

/**
 * 重排序供应商接口
 */
public interface RerankClient {
    String provider();

    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidate, Integer topN, ModelTarget target);

}
