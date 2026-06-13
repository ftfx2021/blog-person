package org.example.springboot.agent.learn.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.LearningAgentReasoningService;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.WorkflowStep;
import org.example.springboot.agent.learn.tools.WebSearchTool;
import org.example.springboot.dto.LearnAgentAskResponse;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤5：观察迭代（Observe and Iterate）
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class ObserveIterateStep implements WorkflowStep {

    private static final int SOURCE_PREVIEW_MAX_LENGTH = 200;
    private static final String DEFAULT_ANSWER = "任务已执行，但未产生可读回答，请补充约束后重试。";

    private final WebSearchTool webSearchTool;
    private final LearningAgentReasoningService reasoningService;
    private final ObjectMapper objectMapper;

    @Override
    public String stepCode() {
        return "OBSERVE_AND_ITERATE";
    }

    @Override
    public void execute(WorkflowContext context) {
        observeAndMaybeIterate(context);

        LearnAgentAskResponse response = new LearnAgentAskResponse();
        response.setAnswer(StringUtils.hasText(context.getAnswer()) ? context.getAnswer() : DEFAULT_ANSWER);
        response.setSources(toSources(context.getDocs()));
        response.setExecutionContext(buildExecutionContext(context));
        context.setResponse(response);
    }

    private void observeAndMaybeIterate(WorkflowContext context) {
        LearningAgentReasoningService.ObserveDecision decision =
                reasoningService.inferObservation(
                        context.getMission(),
                        context.getScene(),
                        context.getPlan(),
                        context.getAction(),
                        context.getAnswer()
                );

        List<String> observations = new ArrayList<>();
        if (decision != null && decision.observations() != null && !decision.observations().isEmpty()) {
            observations.addAll(decision.observations());
        } else {
            observations.add("已完成本轮观察。");
        }

        if (decision != null && decision.needIteration() && StringUtils.hasText(decision.followUpInstruction())) {
            observations.add("模型建议追加一轮动作执行：" + decision.followUpInstruction());
            String fallback = webSearchTool.webSearch(context.getQuestion(), Math.min(context.getTopK(), 5));
            context.getAction().put("iterationSuggestion", decision.followUpInstruction());
            context.getAction().put("iterationProbe", parseJsonToMap(fallback));
        }

        if (decision != null && StringUtils.hasText(decision.answerRefinement())) {
            context.setAnswer(decision.answerRefinement().trim());
        }

        if (!StringUtils.hasText(context.getAnswer())) {
            observations.add("当前回答为空，返回默认提示并保留执行上下文。");
        }

        context.setObservations(observations);
    }

    private Map<String, Object> buildExecutionContext(WorkflowContext context) {
        Map<String, Object> executionContext = new LinkedHashMap<>();
        executionContext.put("mission", context.getMission());
        executionContext.put("scene", context.getScene());
        executionContext.put("plan", context.getPlan());
        executionContext.put("action", context.getAction());
        executionContext.put("observations", context.getObservations());
        return executionContext;
    }

    private List<LearnAgentAskResponse.SourceItem> toSources(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        return docs.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
                    return new LearnAgentAskResponse.SourceItem(
                            String.valueOf(metadata.getOrDefault("source", "未知")),
                            truncate(doc.getText(), SOURCE_PREVIEW_MAX_LENGTH),
                            doc.getScore() != null ? doc.getScore() : 0.0
                    );
                })
                .toList();
    }

    private String truncate(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", json);
            fallback.put("parseError", e.getMessage());
            return fallback;
        }
    }
}
