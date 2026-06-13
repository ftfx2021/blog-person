package org.example.springboot.agent.learn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 学习版推理服务：用 Spring AI 做任务识别与计划生成
 */
@Slf4j
@Service
public class LearningAgentReasoningService {

    private static final Set<String> ALLOWED_INTENTS = Set.of(
            "KNOWLEDGE_QA",
            "ARTICLE_SEARCH",
            "ARTICLE_VECTORIZATION",
            "WEB_SEARCH",
            "WEB_PAGE_PARSE",
            "PDF_PARSE"
    );

    private final ChatClient chatClient;

    public LearningAgentReasoningService(@Qualifier("open-ai") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 用模型识别任务意图
     */
    public MissionDecision inferMission(String question, List<String> history) {
        try {
            String historyBlock = buildHistoryBlock(history);
            MissionDecision decision = chatClient.prompt()
                    .system("""
                            你是任务识别智能体。请根据用户问题和历史对话，输出结构化任务识别结果。
                            你只能从以下 intentType 中选择一个：
                            KNOWLEDGE_QA, ARTICLE_SEARCH, ARTICLE_VECTORIZATION, WEB_SEARCH, WEB_PAGE_PARSE, PDF_PARSE
                            返回字段：
                            - intentType: 上述枚举之一
                            - objective: 任务目标简述
                            - keywords: 3-8个关键词
                            - missingInputs: 缺失的关键输入参数列表
                            - taskSummary: 一句任务摘要
                            只输出结构化结果，不要解释。
                            """)
                    .user("用户问题:\n" + question + "\n\n历史对话:\n" + historyBlock)
                    .call()
                    .entity(MissionDecision.class);

            if (decision == null) {
                return null;
            }
            String intentType = normalizeIntent(decision.intentType());
            List<String> keywords = sanitizeKeywords(decision.keywords());
            List<String> missingInputs = sanitizeKeywords(decision.missingInputs());
            String objective = StringUtils.hasText(decision.objective()) ? decision.objective() : question;
            String summary = StringUtils.hasText(decision.taskSummary()) ? decision.taskSummary() : "已完成任务识别";
            return new MissionDecision(intentType, objective, keywords, missingInputs, summary);
        } catch (Exception e) {
            log.warn("模型任务识别失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 用模型生成执行计划
     */
    public PlanDecision generatePlan(Map<String, Object> mission, Map<String, Object> scene, String question) {
        try {
            String missionText = toSafeText(mission);
            String sceneText = compactScene(scene);
            PlanDecision decision = chatClient.prompt()
                    .system("""
                            你是任务规划智能体。请根据任务识别与环境扫描结果生成可执行计划。
                            返回字段：
                            - plan: 4-6条可执行步骤，每条一句话
                            - rationale: 计划理由（简短）
                            要求：步骤必须可落地，优先使用已扫描到的相关工具。
                            只输出结构化结果，不要解释。
                            """)
                    .user("用户问题:\n" + question + "\n\n任务识别:\n" + missionText + "\n\n环境扫描:\n" + sceneText)
                    .call()
                    .entity(PlanDecision.class);

            if (decision == null || decision.plan() == null || decision.plan().isEmpty()) {
                return null;
            }
            List<String> plan = decision.plan().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .limit(8)
                    .toList();
            if (plan.isEmpty()) {
                return null;
            }
            return new PlanDecision(plan, StringUtils.hasText(decision.rationale()) ? decision.rationale() : "");
        } catch (Exception e) {
            log.warn("模型计划生成失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 用模型完成环境扫描：历史摘要 + 工具筛选
     */
    public SceneDecision inferScene(Map<String, Object> mission, List<String> history, List<Map<String, Object>> allTools) {
        try {
            String missionText = toSafeText(mission);
            String historyText = buildHistoryBlock(history);
            String toolsText = compactToolDefinitions(allTools);
            SceneDecision decision = chatClient.prompt()
                    .system("""
                            你是环境扫描智能体。请根据任务信息，从历史对话和工具列表中提炼与任务相关的上下文。
                            返回字段：
                            - relatedHistory: 与任务直接相关的历史信息列表（最多10条）
                            - relevantToolNames: 建议优先使用的工具名列表（最多8个）
                            - sceneSummary: 环境扫描结论摘要
                            只输出结构化结果，不要解释。
                            """)
                    .user("任务信息:\n" + missionText + "\n\n历史对话:\n" + historyText + "\n\n工具列表:\n" + toolsText)
                    .call()
                    .entity(SceneDecision.class);

            if (decision == null) {
                return null;
            }
            return new SceneDecision(
                    sanitizeKeywords(decision.relatedHistory()),
                    sanitizeKeywords(decision.relevantToolNames()),
                    StringUtils.hasText(decision.sceneSummary()) ? decision.sceneSummary() : "已完成环境扫描"
            );
        } catch (Exception e) {
            log.warn("模型环境扫描失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 用模型完成观察迭代决策
     */
    public ObserveDecision inferObservation(Map<String, Object> mission,
                                            Map<String, Object> scene,
                                            List<String> plan,
                                            Map<String, Object> action,
                                            String currentAnswer) {
        try {
            ObserveDecision decision = chatClient.prompt()
                    .system("""
                            你是观察迭代智能体。请根据任务执行结果判断是否需要追加一轮动作。
                            返回字段：
                            - observations: 观察结论列表
                            - needIteration: 是否需要继续迭代（true/false）
                            - followUpInstruction: 若需要迭代，给执行层的一句追加指令
                            - answerRefinement: 对当前回答的修正文本（可为空）
                            只输出结构化结果，不要解释。
                            """)
                    .user("mission:\n" + toSafeText(mission) +
                            "\n\nscene:\n" + toSafeText(scene) +
                            "\n\nplan:\n" + String.valueOf(plan) +
                            "\n\naction:\n" + toSafeText(action) +
                            "\n\ncurrentAnswer:\n" + (currentAnswer == null ? "" : currentAnswer))
                    .call()
                    .entity(ObserveDecision.class);

            if (decision == null) {
                return null;
            }
            return new ObserveDecision(
                    sanitizeKeywords(decision.observations()),
                    Boolean.TRUE.equals(decision.needIteration()),
                    decision.followUpInstruction(),
                    decision.answerRefinement()
            );
        } catch (Exception e) {
            log.warn("模型观察迭代失败: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeIntent(String intentType) {
        if (!StringUtils.hasText(intentType)) {
            return "KNOWLEDGE_QA";
        }
        String normalized = intentType.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_INTENTS.contains(normalized) ? normalized : "KNOWLEDGE_QA";
    }

    private List<String> sanitizeKeywords(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : input) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String value = item.trim();
            if (!result.contains(value)) {
                result.add(value);
            }
            if (result.size() >= 10) {
                break;
            }
        }
        return result;
    }

    private String buildHistoryBlock(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "(无历史对话)";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(history.size(), 15);
        for (int i = 0; i < limit; i++) {
            sb.append(i + 1).append(". ").append(history.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String compactScene(Map<String, Object> scene) {
        if (scene == null || scene.isEmpty()) {
            return "{}";
        }
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("relatedHistory", scene.getOrDefault("relatedHistory", List.of()));
        Object relevantTools = scene.get("relevantTools");
        if (relevantTools instanceof List<?> list) {
            List<String> toolNames = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    if (name != null && StringUtils.hasText(name.toString())) {
                        toolNames.add(name.toString());
                    }
                }
                if (toolNames.size() >= 10) {
                    break;
                }
            }
            compact.put("relevantTools", toolNames);
        } else {
            compact.put("relevantTools", List.of());
        }
        return compact.toString();
    }

    private String toSafeText(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.toString();
    }

    private String compactToolDefinitions(List<Map<String, Object>> tools) {
        if (tools == null || tools.isEmpty()) {
            return "[]";
        }
        List<Map<String, Object>> compact = new ArrayList<>();
        int limit = Math.min(20, tools.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> tool = tools.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.get("name"));
            item.put("description", tool.get("description"));
            item.put("parameters", tool.get("parameters"));
            compact.add(item);
        }
        return compact.toString();
    }

    public record MissionDecision(
            String intentType,
            String objective,
            List<String> keywords,
            List<String> missingInputs,
            String taskSummary
    ) {
    }

    public record PlanDecision(
            List<String> plan,
            String rationale
    ) {
    }

    public record SceneDecision(
            List<String> relatedHistory,
            List<String> relevantToolNames,
            String sceneSummary
    ) {
    }

    public record ObserveDecision(
            List<String> observations,
            Boolean needIteration,
            String followUpInstruction,
            String answerRefinement
    ) {
    }
}
