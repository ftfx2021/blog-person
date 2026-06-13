package org.example.springboot.agent.learn;

/**
 * 学习版任务流事件监听器
 */
public interface AgentWorkflowEventListener {

    AgentWorkflowEventListener NO_OP = new AgentWorkflowEventListener() {
    };

    default void onStepStart(StepStartEvent event) {
    }

    default void onStepFinish(StepFinishEvent event) {
    }

    default void onToolStart(ToolStartEvent event) {
    }

    default void onToolFinish(ToolFinishEvent event) {
    }

    default void onSubAgentDecision(SubAgentDecisionEvent event) {
    }

    default void onSubAgentStart(SubAgentStartEvent event) {
    }

    default void onSubAgentFinish(SubAgentFinishEvent event) {
    }

    default void onSubAgentSkip(SubAgentSkipEvent event) {
    }

    /**
     * 步骤开始事件
     */
    record StepStartEvent(
            String stepCode,
            String inputSummary,
            int stepIndex,
            int totalSteps
    ) {
    }

    /**
     * 步骤完成事件
     */
    record StepFinishEvent(
            String stepCode,
            String status,
            String inputSummary,
            String outputSummary,
            Long costMs,
            String errorMessage,
            int stepIndex,
            int totalSteps
    ) {
    }

    /**
     * 工具开始事件
     */
    record ToolStartEvent(
            String toolName,
            String callId,
            String argsPreview
    ) {
    }

    /**
     * 工具结束事件
     */
    record ToolFinishEvent(
            String toolName,
            String callId,
            String status,
            Long costMs,
            String outputPreview,
            String errorMessage
    ) {
    }

    /**
     * 子AGENT决策事件
     */
    record SubAgentDecisionEvent(
            String subAgentCode,
            String decision,
            String reason
    ) {
    }

    /**
     * 子AGENT开始事件
     */
    record SubAgentStartEvent(
            String subAgentCode
    ) {
    }

    /**
     * 子AGENT完成事件
     */
    record SubAgentFinishEvent(
            String subAgentCode,
            String status,
            Long costMs,
            String errorMessage
    ) {
    }

    /**
     * 子AGENT跳过事件
     */
    record SubAgentSkipEvent(
            String subAgentCode,
            String reason
    ) {
    }
}
