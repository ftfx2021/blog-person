package org.example.springboot.agent.learn.steps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.agent.learn.LearnToolRegistry;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.WorkflowStep;
import org.example.springboot.dto.LearnAgentAttachmentDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤4：执行动作（Take Action）
 * 通过 Spring AI 工具注册器统一注册并调用工具，不写死业务路由规则
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class TakeActionStep implements WorkflowStep {

    private static final int MAX_ITERATIONS = 2;

    @Qualifier("open-ai")
    private final ChatClient chatClient;
    private final LearnToolRegistry toolRegistry;

    @Override
    public String stepCode() {
        return "TAKE_ACTION";
    }

    @Override
    public void execute(WorkflowContext context) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("actionSource", "spring-ai-tool-calling");
        action.put("status", "FAILED");

        List<Map<String, Object>> attachmentToolSources = buildAttachmentToolSources(context);
        context.setAttachmentToolSources(attachmentToolSources);
        action.put("attachmentCount", attachmentToolSources.size());
        action.put("attachments", attachmentToolSources);

        List<String> candidateToolNames = selectToolsFromScene(context);
        ensureAttachmentTools(candidateToolNames, attachmentToolSources);
        if (candidateToolNames.isEmpty()) {
            action.put("message", "未发现可用工具");
            context.setAnswer("当前没有可用工具可执行任务。");
            context.setAction(action);
            return;
        }

        String runtimeInstruction = "";
        ActionExecutionResult finalResult = null;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            ActionExecutionResult result = executeOneRound(context, candidateToolNames, runtimeInstruction);
            if (result == null) {
                runtimeInstruction = "上一轮结果为空，请至少调用一个工具并给出结构化输出。";
                continue;
            }
            finalResult = result;
            String status = normalizeStatus(result.status());
            if (!"RETRY".equals(status)) {
                break;
            }
            runtimeInstruction = StringUtils.hasText(result.nextStep())
                    ? result.nextStep()
                    : "请继续下一轮动作执行，并基于上一轮结果补全任务。";
        }

        if (finalResult == null) {
            action.put("message", "模型执行失败，未返回有效动作结果");
            context.setAnswer("执行失败：未能获得有效动作结果，请重试或补充更具体的指令。");
            context.setAction(action);
            return;
        }

        String status = normalizeStatus(finalResult.status());
        action.put("status", status);
        action.put("candidateTools", candidateToolNames);
        action.put("usedTools", finalResult.usedTools() == null ? List.of() : finalResult.usedTools());
        action.put("toolOutputs", finalResult.toolOutputs() == null ? Map.of() : finalResult.toolOutputs());
        action.put("observations", finalResult.observations() == null ? List.of() : finalResult.observations());
        action.put("missingInputs", finalResult.missingInputs() == null ? List.of() : finalResult.missingInputs());
        action.put("nextStep", finalResult.nextStep());

        if (StringUtils.hasText(finalResult.answer())) {
            context.setAnswer(finalResult.answer().trim());
        } else if ("NEED_INPUT".equals(status) && finalResult.missingInputs() != null && !finalResult.missingInputs().isEmpty()) {
            context.setAnswer("执行需要补充参数：" + finalResult.missingInputs());
        } else if (!StringUtils.hasText(context.getAnswer())) {
            context.setAnswer("动作已执行，但未生成可读结果，请补充约束后重试。");
        }

        context.setAction(action);
    }

    private ActionExecutionResult executeOneRound(WorkflowContext context, List<String> candidateToolNames, String runtimeInstruction) {
        try {
            return chatClient.prompt()
                    .system("""
                            你是任务执行智能体。你的职责是：
                            1) 根据 mission/scene/plan 决策是否调用工具；
                            2) 必须优先通过工具获取事实，不要凭空编造；
                            3) 当参数不足时返回 NEED_INPUT 并明确缺失项；
                            4) 当需要继续下一轮执行时返回 RETRY；
                            5) 若 attachments 不为空，优先调用 parsePdf 工具读取附件；
                            6) 最终返回结构化结果。
                            
                            输出字段要求：
                            - status: SUCCESS | NEED_INPUT | FAILED | RETRY
                            - answer: 给用户的自然语言回复
                            - usedTools: 已调用工具名列表
                            - toolOutputs: 工具结果摘要对象
                            - observations: 本轮观察结论列表
                            - missingInputs: 缺失参数列表
                            - nextStep: 下一轮执行指令（可选）
                            """)
                    .user(buildActionPrompt(context, candidateToolNames, runtimeInstruction))
                    .toolCallbacks(toolRegistry.providerByNames(candidateToolNames, context.getToolExecutionListener()))
                    .call()
                    .entity(ActionExecutionResult.class);
        } catch (Exception e) {
            log.warn("执行动作失败: {}", e.getMessage());
            return null;
        }
    }

    private String buildActionPrompt(WorkflowContext context, List<String> candidateToolNames, String runtimeInstruction) {
        return """
                mission:
                %s
                
                scene:
                %s
                
                plan:
                %s
                
                question:
                %s
                
                history:
                %s

                attachments:
                %s
                
                candidateTools:
                %s
                
                runtimeInstruction:
                %s
                """.formatted(
                String.valueOf(context.getMission()),
                String.valueOf(context.getScene()),
                String.valueOf(context.getPlan()),
                String.valueOf(context.getQuestion()),
                String.valueOf(context.getHistory()),
                String.valueOf(context.getAttachmentToolSources()),
                String.valueOf(candidateToolNames),
                StringUtils.hasText(runtimeInstruction) ? runtimeInstruction : "(无)"
        );
    }

    private List<String> selectToolsFromScene(WorkflowContext context) {
        List<String> names = new ArrayList<>();
        if (context.getScene() != null) {
            Object relevant = context.getScene().get("relevantTools");
            if (relevant instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Object name = map.get("name");
                        if (name != null && StringUtils.hasText(name.toString())) {
                            names.add(name.toString().trim());
                        }
                    }
                }
            }
        }
        return toolRegistry.normalizeToolNames(names);
    }

    private void ensureAttachmentTools(List<String> candidateToolNames, List<Map<String, Object>> attachmentToolSources) {
        if (attachmentToolSources == null || attachmentToolSources.isEmpty()) {
            return;
        }
        if (!candidateToolNames.contains("parsePdf")) {
            candidateToolNames.add(0, "parsePdf");
        }
    }

    private List<Map<String, Object>> buildAttachmentToolSources(WorkflowContext context) {
        if (context.getAttachments() == null || context.getAttachments().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        for (LearnAgentAttachmentDTO attachment : context.getAttachments()) {
            if (attachment == null || !isPdfAttachment(attachment)) {
                continue;
            }
            String normalizedSource = normalizeAttachmentSource(attachment.getFileUrl());
            if (!StringUtils.hasText(normalizedSource)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileName", attachment.getFileName());
            item.put("mimeType", attachment.getMimeType());
            item.put("fileUrl", attachment.getFileUrl());
            item.put("toolSource", normalizedSource);
            item.put("toolHint", "请优先调用 parsePdf 并把 source 设为 toolSource");
            sources.add(item);
        }
        return sources;
    }

    static boolean isPdfAttachment(LearnAgentAttachmentDTO attachment) {
        if (attachment == null) {
            return false;
        }
        if (StringUtils.hasText(attachment.getMimeType()) && attachment.getMimeType().toLowerCase().contains("pdf")) {
            return true;
        }
        if (StringUtils.hasText(attachment.getFileName()) && attachment.getFileName().toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return StringUtils.hasText(attachment.getFileUrl()) && attachment.getFileUrl().toLowerCase().endsWith(".pdf");
    }

    static String normalizeAttachmentSource(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return "";
        }
        String trimmed = fileUrl.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/files/")) {
            return trimmed.substring(1);
        }
        if (trimmed.startsWith("files/")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "FAILED";
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "SUCCESS", "NEED_INPUT", "FAILED", "RETRY" -> normalized;
            default -> "FAILED";
        };
    }

    public record ActionExecutionResult(
            String status,
            String answer,
            List<String> usedTools,
            Map<String, Object> toolOutputs,
            List<String> observations,
            List<String> missingInputs,
            String nextStep
    ) {
    }
}
