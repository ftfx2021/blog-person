package org.example.springboot.agent.learn.steps;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.LearnToolRegistry;
import org.example.springboot.agent.learn.LearningAgentReasoningService;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.WorkflowStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤2：环境扫描（Scan the Scene）
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ScanSceneStep implements WorkflowStep {

    private final LearnToolRegistry toolRegistry;
    private final LearningAgentReasoningService reasoningService;

    @Override
    public String stepCode() {
        return "SCAN_SCENE";
    }

    @Override
    public void execute(WorkflowContext context) {
        List<String> history = context.getHistory() == null ? List.of() : context.getHistory();
        List<Map<String, Object>> allTools = toolRegistry.listTools();

        LearningAgentReasoningService.SceneDecision sceneDecision =
                reasoningService.inferScene(context.getMission(), history, allTools);

        List<String> relatedHistory;
        List<Map<String, Object>> relevantTools;
        String sceneSummary;

        if (sceneDecision != null) {
            relatedHistory = sceneDecision.relatedHistory() == null ? List.of() : sceneDecision.relatedHistory();
            sceneSummary = StringUtils.hasText(sceneDecision.sceneSummary()) ? sceneDecision.sceneSummary() : "已完成环境扫描";
            relevantTools = pickToolsByNames(allTools, sceneDecision.relevantToolNames());
            if (relevantTools.isEmpty()) {
                relevantTools = allTools.stream().limit(6).toList();
            }
        } else {
            relatedHistory = history.stream().filter(StringUtils::hasText).map(String::trim).limit(10).toList();
            relevantTools = allTools.stream().limit(6).toList();
            sceneSummary = "模型环境扫描失败，使用默认上下文摘要。";
        }

        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("relatedHistory", relatedHistory);
        scene.put("allTools", allTools);
        scene.put("relevantTools", relevantTools);
        scene.put("sceneSummary", sceneSummary);
        scene.put("sceneSource", sceneDecision != null ? "spring-ai" : "fallback");
        scene.put("historyCount", history.size());
        context.setScene(scene);
    }

    private List<Map<String, Object>> pickToolsByNames(List<Map<String, Object>> allTools, List<String> names) {
        if (allTools == null || allTools.isEmpty() || names == null || names.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> tool : allTools) {
            String name = String.valueOf(tool.getOrDefault("name", ""));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            for (String candidate : names) {
                if (StringUtils.hasText(candidate) && name.equalsIgnoreCase(candidate.trim())) {
                    matched.add(tool);
                    break;
                }
            }
        }
        return matched;
    }
}
