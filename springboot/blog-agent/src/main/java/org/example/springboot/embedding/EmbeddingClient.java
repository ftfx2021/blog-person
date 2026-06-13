package org.example.springboot.embedding;

import org.example.springboot.model.ModelTarget;

import java.util.List;

public interface EmbeddingClient {
    /**
     * 获取嵌入服务提供商名称
     *
     * @return 提供商标识字符串
     */
    String provider();

    /**
     * 将单个文本转换为嵌入向量
     *
     * @param text   待嵌入的文本内容
     * @param target 目标模型配置（上层只需传入id，但是client层肯定要传入完整的模型配置）
     * @return 文本的向量表示，以浮点数列表形式返回
     */
    List<Float> embed(String  text, ModelTarget target);

    List<List<Float>> embedBatch(List<String> texts, ModelTarget target);


}
