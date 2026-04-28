package org.example.springboot.agent.learn;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.agent.learn.subagent.MasterAgentOrchestrator;
import org.example.springboot.agent.learn.subagent.StepExecutionGateway;
import org.example.springboot.dto.LearnAgentAttachmentDTO;
import org.example.springboot.dto.LearnAgentAskResponse;
import org.example.springboot.dto.StepTraceDTO;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 学习版通用智能体任务流服务
 */
@Slf4j
@Service
public class LearningAgentWorkflowService {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 10;

    private final Map<String, WorkflowStep> workflowStepByCode;
    private final MasterAgentOrchestrator masterAgentOrchestrator;
    private final int totalSteps;

    public LearningAgentWorkflowService(List<WorkflowStep> workflowSteps,
                                        MasterAgentOrchestrator masterAgentOrchestrator) {
        List<WorkflowStep> sortedSteps = new ArrayList<>(workflowSteps);
        AnnotationAwareOrderComparator.sort(sortedSteps);
        this.workflowStepByCode = new LinkedHashMap<>();
        for (WorkflowStep step : sortedSteps) {
            this.workflowStepByCode.put(step.stepCode(), step);
        }
        this.totalSteps = this.workflowStepByCode.size();
        this.masterAgentOrchestrator = masterAgentOrchestrator;
    }

    /**
     * 执行学习版任务流
     */
    public LearnAgentAskResponse execute(String question, Integer topK) {
        return execute(question, topK, null, null, AgentWorkflowEventListener.NO_OP);
    }

    /**
     * 执行学习版任务流（包含历史对话）
     */
    public LearnAgentAskResponse execute(String question, Integer topK, List<String> history) {
        return execute(question, topK, history, null, AgentWorkflowEventListener.NO_OP);
    }

    /**
     * 执行学习版任务流（包含历史对话与附件）
     */
    public LearnAgentAskResponse execute(String question,
                                         Integer topK,
                                         List<String> history,
                                         List<LearnAgentAttachmentDTO> attachments) {
        return execute(question, topK, history, attachments, AgentWorkflowEventListener.NO_OP);
    }

    /**
     * 执行学习版任务流（事件回调版）
     */
    public LearnAgentAskResponse execute(String question,
                                         Integer topK,
                                         List<String> history,
                                         List<LearnAgentAttachmentDTO> attachments,
                                         AgentWorkflowEventListener listener) {
        WorkflowContext context = new WorkflowContext();
        context.setQuestion(question);
        context.setTopK(normalizeTopK(topK));
        context.setHistory(history == null ? new ArrayList<>() : history);
        context.setAttachments(attachments == null ? new ArrayList<>() : attachments);
        context.setStartTime(System.currentTimeMillis());
        context.setToolExecutionListener(buildToolExecutionListener(listener));

        AgentWorkflowEventListener eventListener = listener == null ? AgentWorkflowEventListener.NO_OP : listener;
        AtomicInteger stepIndex = new AtomicInteger(0);
        StepExecutionGateway stepExecutionGateway = (stepCode, ctx) -> {
            WorkflowStep step = workflowStepByCode.get(stepCode);
            if (step == null) {
                throw new IllegalArgumentException("未找到步骤: " + stepCode);
            }
            executeStep(step, ctx, stepIndex.incrementAndGet(), totalSteps, eventListener);
        };
        masterAgentOrchestrator.execute(context, eventListener, stepExecutionGateway);

        return buildFinalResponse(context);
    }

    private void executeStep(WorkflowStep step,
                             WorkflowContext context,
                             int stepIndex,
                             int totalSteps,
                             AgentWorkflowEventListener listener) {
        long stepStart = System.currentTimeMillis();
        StepTraceDTO trace = new StepTraceDTO();
        trace.setStepCode(step.stepCode());
        trace.setInputSummary(buildInputSummary(step.stepCode(), context));
        listener.onStepStart(new AgentWorkflowEventListener.StepStartEvent(
                trace.getStepCode(),
                trace.getInputSummary(),
                stepIndex,
                totalSteps
        ));

        try {
            step.execute(context);
            trace.setStatus("SUCCESS");
            trace.setOutputSummary(buildOutputSummary(step.stepCode(), context));
            trace.setCostMs(System.currentTimeMillis() - stepStart);
            context.getStepTraces().add(trace);
            listener.onStepFinish(new AgentWorkflowEventListener.StepFinishEvent(
                    trace.getStepCode(),
                    trace.getStatus(),
                    trace.getInputSummary(),
                    trace.getOutputSummary(),
                    trace.getCostMs(),
                    trace.getErrorMessage(),
                    stepIndex,
                    totalSteps
            ));
        } catch (Exception e) {
            trace.setStatus("FAILED");
            trace.setOutputSummary("步骤执行失败");
            trace.setErrorMessage(resolveErrorMessage(e));
            trace.setCostMs(System.currentTimeMillis() - stepStart);
            context.getStepTraces().add(trace);
            listener.onStepFinish(new AgentWorkflowEventListener.StepFinishEvent(
                    trace.getStepCode(),
                    trace.getStatus(),
                    trace.getInputSummary(),
                    trace.getOutputSummary(),
                    trace.getCostMs(),
                    trace.getErrorMessage(),
                    stepIndex,
                    totalSteps
            ));

            LearnAgentAskResponse partialResponse = buildFinalResponse(context);
            String errorMessage = "任务流步骤执行失败: " + step.stepCode();
            log.error("{}，原因: {}", errorMessage, resolveErrorMessage(e), e);
            throw new LearningAgentWorkflowException(errorMessage, partialResponse, e);
        }
    }

    private LearnAgentAskResponse buildFinalResponse(WorkflowContext context) {
        LearnAgentAskResponse response = context.getResponse();
        if (response == null) {
            response = new LearnAgentAskResponse();
            response.setAnswer(StringUtils.hasText(context.getAnswer()) ? context.getAnswer() : "抱歉，在知识库中没有找到相关信息。");
            response.setSources(new ArrayList<>());
        }
        Map<String, Object> executionContext = response.getExecutionContext();
        if (executionContext == null || executionContext.isEmpty()) {
            executionContext = new LinkedHashMap<>();
        } else {
            executionContext = new LinkedHashMap<>(executionContext);
        }
        executionContext.putIfAbsent("mission", context.getMission());
        executionContext.putIfAbsent("scene", context.getScene());
        executionContext.putIfAbsent("plan", context.getPlan());
        executionContext.putIfAbsent("action", context.getAction());
        executionContext.putIfAbsent("observations", context.getObservations());
        executionContext.put("orchestration", Map.of(
                "mode", context.getOrchestrationMode() == null ? "MASTER_SUBAGENT" : context.getOrchestrationMode(),
                "decisions", context.getOrchestrationDecisions() == null ? List.of() : context.getOrchestrationDecisions()
        ));
        response.setExecutionContext(executionContext);
        response.setSteps(new ArrayList<>(context.getStepTraces()));
        response.setElapsedMs(System.currentTimeMillis() - context.getStartTime());
        context.setResponse(response);
        return response;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        if (topK < MIN_TOP_K) {
            return MIN_TOP_K;
        }
        if (topK > MAX_TOP_K) {
            return MAX_TOP_K;
        }
        return topK;
    }

    private String buildInputSummary(String stepCode, WorkflowContext context) {
        return switch (stepCode) {
            case "GET_MISSION" -> String.format("questionLength=%d, historyCount=%d",
                    context.getQuestion() == null ? 0 : context.getQuestion().length(),
                    context.getHistory() == null ? 0 : context.getHistory().size());
            case "SCAN_SCENE" -> String.format("intentType=%s, keywords=%s",
                    String.valueOf(context.getMission().getOrDefault("intentType", "UNKNOWN")),
                    String.valueOf(context.getMission().getOrDefault("keywords", List.of())));
            case "THINK_IT_THROUGH" -> String.format("intentType=%s, relevantTools=%d",
                    String.valueOf(context.getMission().getOrDefault("intentType", "UNKNOWN")),
                    countList(context.getScene().get("relevantTools")));
            case "TAKE_ACTION" -> String.format("intentType=%s, topK=%d",
                    String.valueOf(context.getMission().getOrDefault("intentType", "UNKNOWN")),
                    context.getTopK());
            case "OBSERVE_AND_ITERATE" -> String.format("docs=%d, answerLength=%d",
                    context.getDocs() == null ? 0 : context.getDocs().size(),
                    context.getAnswer() == null ? 0 : context.getAnswer().length());
            default -> "N/A";
        };
    }

    private String buildOutputSummary(String stepCode, WorkflowContext context) {
        return switch (stepCode) {
            case "GET_MISSION" -> String.format("intentType=%s",
                    String.valueOf(context.getMission().getOrDefault("intentType", "UNKNOWN")));
            case "SCAN_SCENE" -> String.format("relatedHistory=%d, tools=%d, relevantTools=%d",
                    countList(context.getScene().get("relatedHistory")),
                    countList(context.getScene().get("allTools")),
                    countList(context.getScene().get("relevantTools")));
            case "THINK_IT_THROUGH" -> String.format("planSteps=%d", context.getPlan() == null ? 0 : context.getPlan().size());
            case "TAKE_ACTION" -> String.format("actionStatus=%s, answerLength=%d",
                    String.valueOf(context.getAction().getOrDefault("status", "UNKNOWN")),
                    context.getAnswer() == null ? 0 : context.getAnswer().length());
            case "OBSERVE_AND_ITERATE" -> String.format("sources=%d, observations=%d",
                    context.getResponse() == null || context.getResponse().getSources() == null
                            ? 0 : context.getResponse().getSources().size(),
                    context.getObservations() == null ? 0 : context.getObservations().size());
            default -> "N/A";
        };
    }

    @SuppressWarnings("unchecked")
    private int countList(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return 0;
    }

    private String resolveErrorMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private LearnToolRegistry.ToolExecutionListener buildToolExecutionListener(AgentWorkflowEventListener listener) {
        AgentWorkflowEventListener finalListener = listener == null ? AgentWorkflowEventListener.NO_OP : listener;
        return new LearnToolRegistry.ToolExecutionListener() {
            @Override
            public void onToolStart(String toolName, String callId, String argsPreview) {
                finalListener.onToolStart(new AgentWorkflowEventListener.ToolStartEvent(toolName, callId, argsPreview));
            }

            @Override
            public void onToolFinish(String toolName, String callId, String status, Long costMs, String outputPreview, String errorMessage) {
                finalListener.onToolFinish(new AgentWorkflowEventListener.ToolFinishEvent(
                        toolName, callId, status, costMs, outputPreview, errorMessage
                ));
            }
        };
    }
}
