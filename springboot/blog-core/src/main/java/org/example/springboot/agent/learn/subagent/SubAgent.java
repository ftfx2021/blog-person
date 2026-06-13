package org.example.springboot.agent.learn.subagent;

import org.example.springboot.agent.learn.WorkflowContext;

/**
 * 子AGENT抽象
 */
public interface SubAgent {

    /**
     * 子AGENT编码
     */
    String code();

    /**
     * 是否执行该子AGENT
     */
    SubAgentDecision shouldRun(WorkflowContext context);

    /**
     * 执行子AGENT
     */
    void execute(WorkflowContext context, StepExecutionGateway gateway) throws Exception;

    /**
     * 执行失败兜底
     */
    void onFailure(WorkflowContext context, Exception exception, StepExecutionGateway gateway) throws Exception;
}
