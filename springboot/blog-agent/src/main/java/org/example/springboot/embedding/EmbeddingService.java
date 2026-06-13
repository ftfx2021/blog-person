package org.example.springboot.embedding;

import java.util.List;

public interface EmbeddingService {

    /**
     * 不指定模型向量化
     * @param text 待向量化的文本
     * @return 文本的向量表示
     */
    List<Float> embed(String text);

    /**
     * 指定模型向量化
     * @param text 待向量化的文本
     * @param modelId 模型id
     * @return 文本的向量表示
     */
    List<Float> embed(String  text,String modelId);

    /**
     * 批量将多个文本转换为嵌入向量
     *
     * @param texts  待嵌入的文本列表
     * @return 文本向量列表，每个文本对应一个向量（浮点数列表）
     */
    List<List<Float>> embedBatch(List<String> texts);


    /**
     * 批量将多个文本转换为嵌入向量
     *
     * @param texts  待嵌入的文本列表
     * @param modelId 目标模型id
     * @return 文本向量列表，每个文本对应一个向量（浮点数列表）
     */
    List<List<Float>> embedBatch(List<String> texts,String modelId);
}
