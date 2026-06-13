package org.example.springboot.agent.learn.subagent;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.agent.learn.AgentWorkflowEventListener;
import org.example.springboot.agent.learn.WorkflowContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主AGENT编排器
 */
@Slf4j
@Component
public class MasterAgentOrchestrator {

    private static final String MODE = "MASTER_SUBAGENT";

    private final List<SubAgent> subAgents;

    public MasterAgentOrchestrator(List<SubAgent> subAgents) {
        List<SubAgent> agents = subAgents == null ? new ArrayList<>() : new ArrayList<>(subAgents);
        AnnotationAwareOrderComparator.sort(agents);
        this.subAgents = agents;
    }

    public void execute(WorkflowContext context,
                        AgentWorkflowEventListener listener,
                        StepExecutionGateway gateway) {
        AgentWorkflowEventListener eventListener = listener == null ? AgentWorkflowEventListener.NO_OP : listener;
        List<Map<String, Object>> decisions = new ArrayList<>();
        context.setOrchestrationMode(MODE);
        context.setOrchestrationDecisions(decisions);

        for (SubAgent subAgent : subAgents) {
            String subAgentCode = subAgent.code();
            SubAgentDecision decision = safeDecision(subAgent, context);
            Map<String, Object> trace = createTrace(subAgentCode, decision);
            decisions.add(trace);

            eventListener.onSubAgentDecision(new AgentWorkflowEventListener.SubAgentDecisionEvent(
                    subAgentCode,
                    decision.decision(),
                    decision.reason()
            ));

            if ("SKIP".equalsIgnoreCase(decision.decision())) {
                trace.put("status", "SKIPPED");
                trace.put("costMs", 0L);
                eventListener.onSubAgentSkip(new AgentWorkflowEventListener.SubAgentSkipEvent(
                        subAgentCode,
                        decision.reason()
                ));
                continue;
            }

            long start = System.currentTimeMillis();
            eventListener.onSubAgentStart(new AgentWorkflowEventListener.SubAgentStartEvent(subAgentCode));
            try {
                subAgent.execute(context, gateway);
                trace.put("status", "SUCCESS");
                trace.put("costMs", System.currentTimeMillis() - start);
                eventListener.onSubAgentFinish(new AgentWorkflowEventListener.SubAgentFinishEvent(
                        subAgentCode,
                        "SUCCESS",
                        (Long) trace.get("costMs"),
                        null
                ));
            } catch (Exception e) {
                trace.put("status", "FAILED");
                trace.put("costMs", System.currentTimeMillis() - start);
                trace.put("errorMessage", resolveErrorMessage(e));
                try {
                    subAgent.onFailure(context, e, gateway);
                    trace.put("status", "FALLBACK");
                    eventListener.onSubAgentFinish(new AgentWorkflowEventListener.SubAgentFinishEvent(
                            subAgentCode,
                            "FALLBACK",
                            (Long) trace.get("costMs"),
                            resolveErrorMessage(e)
                    ));
                } catch (Exception fallbackException) {
                    trace.put("errorMessage", resolveErrorMessage(fallbackException));
                    eventListener.onSubAgentFinish(new AgentWorkflowEventListener.SubAgentFinishEvent(
                            subAgentCode,
                            "FAILED",
                            (Long) trace.get("costMs"),
                            resolveErrorMessage(fallbackException)
                    ));
                    log.error("子AGENT执行失败且兜底失败: subAgent={}, error={}", subAgentCode, resolveErrorMessage(fallbackException), fallbackException);
                }
            }
        }
    }

    private SubAgentDecision safeDecision(SubAgent subAgent, WorkflowContext context) {
        try {
            SubAgentDecision decision = subAgent.shouldRun(context);
            if (decision == null) {
                return SubAgentDecision.run("未返回决策，按默认执行");
            }
            return decision;
        } catch (Exception e) {
            log.warn("子AGENT决策失败，默认执行: subAgent={}, error={}", subAgent.code(), resolveErrorMessage(e));
            return SubAgentDecision.run("决策异常，默认执行");
        }
    }

    private Map<String, Object> createTrace(String subAgentCode, SubAgentDecision decision) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("subAgentCode", subAgentCode);
        trace.put("decision", decision.decision());
        trace.put("reason", decision.reason());
        trace.put("status", "PENDING");
        trace.put("costMs", 0L);
        trace.put("errorMessage", "");
        return trace;
    }

    private String resolveErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return e == null ? "未知异常" : e.getClass().getSimpleName();
        }
        return e.getMessage();
    }
}
