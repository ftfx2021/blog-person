package org.example.springboot.agent.learn;

import lombok.Getter;
import org.example.springboot.dto.LearnAgentAskResponse;

/**
 * 学习版任务流异常（携带部分执行结果）
 */
@Getter
public class LearningAgentWorkflowException extends RuntimeException {

    private final LearnAgentAskResponse partialResponse;

    public LearningAgentWorkflowException(String message, LearnAgentAskResponse partialResponse, Throwable cause) {
        super(message, cause);
        this.partialResponse = partialResponse;
    }
}

