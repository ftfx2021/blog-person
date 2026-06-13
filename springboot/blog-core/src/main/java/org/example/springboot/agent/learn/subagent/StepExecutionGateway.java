package org.example.springboot.agent.learn.subagent;

import org.example.springboot.agent.learn.WorkflowContext;

/**
 * 子AGENT调用底层步骤网关
 */
public interface StepExecutionGateway {

    void executeStep(String stepCode, WorkflowContext context) throws Exception;
}
