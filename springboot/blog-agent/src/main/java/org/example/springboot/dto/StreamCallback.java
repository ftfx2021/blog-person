package org.example.springboot.dto;

public interface StreamCallback {

    /**
     * 接收一次增量内容（Delta Token 或部分片段）
     */
    void onContent(String content);

    /**
     * 接收思考过程增量内容（如果模型支持）
     * 默认空实现，未支持思考的场景可以忽略
     */
    default void onThinking(String content) {
    }

    /**
     * 整个推送流程结束（全部内容推送完毕）
     */
    void onComplete();

    /**
     * 流式推送过程中出现异常
     */
    void onError(Throwable error);

}
