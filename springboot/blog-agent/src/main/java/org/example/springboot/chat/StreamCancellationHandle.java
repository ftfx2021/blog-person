package org.example.springboot.chat;

/**
 * 取消机制
 */
public interface StreamCancellationHandle {
    /**
     * 取消当前流式推理任务
     * 调用后应立即尝试取消底层模型生成过程
     */
    void cancel();
}
