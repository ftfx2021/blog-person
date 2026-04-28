package org.example.springboot.agent.learn.subagent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.tools.KnowledgeQaTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子AGENT：动作执行与工具调用
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class ExecutionSubAgent implements SubAgent {

    public static final String CODE = "EXECUTION_SUB_AGENT";
    public static final String STEP_CODE = "TAKE_ACTION";

    @Value("${agent.learn.orchestration.execution-fallback-enabled:true}")
    private boolean fallbackEnabled;

    private final KnowledgeQaTool knowledgeQaTool;
    private final ObjectMapper objectMapper;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public SubAgentDecision shouldRun(WorkflowContext context) {
        return SubAgentDecision.run("执行层始终执行");
    }

    @Override
    public void execute(WorkflowContext context, StepExecutionGateway gateway) throws Exception {
        gateway.executeStep(STEP_CODE, context);

        String status = String.valueOf(context.getAction().getOrDefault("status", "FAILED"));
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return;
        }
        if (!fallbackEnabled) {
            throw new RuntimeException("执行层失败且降级已关闭");
        }
        boolean fallbackSuccess = runKnowledgeFallback(context, "执行层状态非SUCCESS，触发知识库降级");
        if (!fallbackSuccess) {
            throw new RuntimeException("执行层失败且知识库降级失败");
        }
    }

    @Override
    public void onFailure(WorkflowContext context, Exception exception, StepExecutionGateway gateway) throws Exception {
        if (!fallbackEnabled) {
            markFailure(context, "执行层失败且降级关闭: " + resolveMessage(exception));
            return;
        }
        boolean fallbackSuccess = runKnowledgeFallback(context, "执行异常，触发知识库降级: " + resolveMessage(exception));
        if (!fallbackSuccess) {
            markFailure(context, "执行层异常且知识库降级失败: " + resolveMessage(exception));
            throw exception;
        }
    }

    private boolean runKnowledgeFallback(WorkflowContext context, String reason) {
        try {
            String raw = knowledgeQaTool.askKnowledgeBase(context.getQuestion(), context.getTopK());
            Map<String, Object> fallback = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
            boolean success = Boolean.TRUE.equals(fallback.get("success"));
            context.getAction().put("fallbackReason", reason);
            context.getAction().put("fallbackSource", "askKnowledgeBase");
            context.getAction().put("fallbackResult", fallback);
            if (!success) {
                context.getAction().put("status", "FAILED");
                context.getAction().put("fallbackError", String.valueOf(fallback.getOrDefault("message", "fallback failed")));
                return false;
            }

            context.getAction().put("status", "SUCCESS");
            context.getAction().put("usedTools", List.of("askKnowledgeBase"));
            context.getAction().put("missingInputs", List.of());
            if (StringUtils.hasText(String.valueOf(fallback.get("answer")))) {
                context.setAnswer(String.valueOf(fallback.get("answer")));
            }
            return true;
        } catch (Exception e) {
            context.getAction().put("status", "FAILED");
            context.getAction().put("fallbackError", resolveMessage(e));
            log.warn("执行层知识库降级失败: {}", resolveMessage(e));
            return false;
        }
    }

    private void markFailure(WorkflowContext context, String message) {
        if (context.getAction() == null || context.getAction().isEmpty()) {
            context.setAction(new LinkedHashMap<>());
        }
        context.getAction().put("status", "FAILED");
        context.getAction().put("failureReason", message);
        if (!StringUtils.hasText(context.getAnswer())) {
            context.setAnswer("执行失败：" + message);
        }
    }

    private String resolveMessage(Exception e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return e == null ? "未知异常" : e.getClass().getSimpleName();
        }
        return e.getMessage();
    }
}
