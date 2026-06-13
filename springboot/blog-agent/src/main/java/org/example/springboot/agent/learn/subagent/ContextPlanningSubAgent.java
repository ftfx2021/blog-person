package org.example.springboot.agent.learn.subagent;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.LearnToolRegistry;
import org.example.springboot.agent.learn.WorkflowContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子AGENT：环境扫描 + 计划生成
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ContextPlanningSubAgent implements SubAgent {

    public static final String CODE = "CONTEXT_PLANNING_SUB_AGENT";
    public static final String SCAN_STEP_CODE = "SCAN_SCENE";
    public static final String PLAN_STEP_CODE = "THINK_IT_THROUGH";

    @Value("${agent.learn.orchestration.fast-path-enabled:true}")
    private boolean fastPathEnabled;

    private final LearnToolRegistry toolRegistry;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public SubAgentDecision shouldRun(WorkflowContext context) {
        if (!fastPathEnabled) {
            return SubAgentDecision.run("极速直答已关闭，执行环境与计划");
        }
        String intentType = String.valueOf(context.getMission().getOrDefault("intentType", "KNOWLEDGE_QA"));
        boolean noAttachments = context.getAttachments() == null || context.getAttachments().isEmpty();
        boolean noHistory = context.getHistory() == null || context.getHistory().isEmpty();
        int questionLength = context.getQuestion() == null ? 0 : context.getQuestion().trim().length();
        boolean fastPath = "KNOWLEDGE_QA".equalsIgnoreCase(intentType)
                && noAttachments
                && noHistory
                && questionLength <= 60;
        if (fastPath) {
            return SubAgentDecision.skip("命中极速直答条件，跳过环境扫描与规划");
        }
        return SubAgentDecision.run("未命中极速直答条件，执行环境与计划");
    }

    @Override
    public void execute(WorkflowContext context, StepExecutionGateway gateway) throws Exception {
        gateway.executeStep(SCAN_STEP_CODE, context);
        gateway.executeStep(PLAN_STEP_CODE, context);
    }

    @Override
    public void onFailure(WorkflowContext context, Exception exception, StepExecutionGateway gateway) {
        List<Map<String, Object>> allTools = toolRegistry.listTools();
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("relatedHistory", context.getHistory() == null ? List.of() : context.getHistory());
        scene.put("allTools", allTools);
        scene.put("relevantTools", allTools.stream().limit(6).toList());
        scene.put("sceneSummary", "环境扫描失败，主AGENT使用兜底上下文。");
        scene.put("sceneSource", "master-fallback");
        scene.put("historyCount", context.getHistory() == null ? 0 : context.getHistory().size());
        context.setScene(scene);

        List<String> plan = new ArrayList<>();
        plan.add("复核任务目标与缺失输入，明确本轮执行边界。");
        plan.add("选择最相关工具并准备参数。");
        plan.add("执行工具调用并收集结果。");
        plan.add("整理回答并返回。");
        context.setPlan(plan);
        context.getMission().put("planSource", "master-fallback");
    }
}
