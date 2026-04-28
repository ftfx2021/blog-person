package org.example.springboot.agent.learn.steps;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.LearningAgentReasoningService;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.WorkflowStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 步骤3：思考规划（Think It Through）
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class ThinkItThroughStep implements WorkflowStep {

    private final LearningAgentReasoningService reasoningService;

    @Override
    public String stepCode() {
        return "THINK_IT_THROUGH";
    }

    @Override
    public void execute(WorkflowContext context) {
        LearningAgentReasoningService.PlanDecision decision =
                reasoningService.generatePlan(context.getMission(), context.getScene(), context.getQuestion());

        if (decision != null && decision.plan() != null && !decision.plan().isEmpty()) {
            context.setPlan(new ArrayList<>(decision.plan()));
            context.getMission().put("planSource", "spring-ai");
            if (decision.rationale() != null && !decision.rationale().isBlank()) {
                context.getMission().put("planRationale", decision.rationale());
            }
            return;
        }

        List<String> plan = new ArrayList<>();
        plan.add("复核任务目标与缺失输入，明确本轮执行边界。");
        plan.add("结合环境扫描结果选择最相关工具并准备参数。");
        plan.add("执行工具调用并收集可验证结果。");
        plan.add("检查结果完整性，必要时触发一次补充动作。");

        context.setPlan(plan);
        context.getMission().put("planSource", "rule-fallback");
    }
}
