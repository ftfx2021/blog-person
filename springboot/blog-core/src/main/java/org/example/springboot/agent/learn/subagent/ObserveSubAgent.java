package org.example.springboot.agent.learn.subagent;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.WorkflowContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 子AGENT：观察与收敛
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class ObserveSubAgent implements SubAgent {

    public static final String CODE = "OBSERVE_SUB_AGENT";
    public static final String STEP_CODE = "OBSERVE_AND_ITERATE";

    @Value("${agent.learn.orchestration.observe-skip-enabled:true}")
    private boolean observeSkipEnabled;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public SubAgentDecision shouldRun(WorkflowContext context) {
        if (!observeSkipEnabled) {
            return SubAgentDecision.run("观察跳过已关闭，执行观察收敛");
        }
        String status = String.valueOf(context.getAction().getOrDefault("status", "FAILED"));
        int answerLength = context.getAnswer() == null ? 0 : context.getAnswer().length();
        boolean noMissingInputs = isMissingInputsEmpty(context);
        if ("SUCCESS".equalsIgnoreCase(status) && answerLength >= 80 && noMissingInputs) {
            return SubAgentDecision.skip("执行成功且回答充分，跳过观察收敛");
        }
        return SubAgentDecision.run("执行结果仍需观察收敛");
    }

    @Override
    public void execute(WorkflowContext context, StepExecutionGateway gateway) throws Exception {
        gateway.executeStep(STEP_CODE, context);
    }

    @Override
    public void onFailure(WorkflowContext context, Exception exception, StepExecutionGateway gateway) {
        List<String> observations = context.getObservations() == null ? new ArrayList<>() : new ArrayList<>(context.getObservations());
        observations.add("观察阶段失败，保留当前答案与动作上下文: " + resolveMessage(exception));
        context.setObservations(observations);
    }

    @SuppressWarnings("unchecked")
    private boolean isMissingInputsEmpty(WorkflowContext context) {
        Object missingInputs = context.getAction().get("missingInputs");
        if (missingInputs == null) {
            return true;
        }
        if (missingInputs instanceof List<?> list) {
            return list.isEmpty();
        }
        return true;
    }

    private String resolveMessage(Exception e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return e == null ? "未知异常" : e.getClass().getSimpleName();
        }
        return e.getMessage();
    }
}
