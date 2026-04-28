package org.example.springboot.service;

import java.util.List;
// 向量化接口
//EmbeddingService 支持单条 embed() 和批量 embedBatch()，
// 还提供 dimension() 方法返回向量维度，用于向量库 Schema 定义。
public interface EmbeddingService {
    List<Float> embed(String text);
    List<List<Float>> embedBatch(List<String> texts);
    int dimension();
}
