package org.example.springboot.agent.learn;

/**
 * 学习版任务流步骤
 */
public interface WorkflowStep {

    /**
     * 步骤编码
     */
    String stepCode();

    /**
     * 执行步骤
     */
    void execute(WorkflowContext context);
}

