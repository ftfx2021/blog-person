package org.example.springboot.agent.learn.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习版：知识库问答工具
 */
@Slf4j
@Component
public class KnowledgeQaTool {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 10;
    private static final int SOURCE_PREVIEW_MAX_LENGTH = 200;

    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    public KnowledgeQaTool(KnowledgeBaseService knowledgeBaseService, ObjectMapper objectMapper) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = objectMapper;
    }

    @Tool(
            name = "askKnowledgeBase",
            description = "基于知识库问答，返回答案和来源列表。适用于常规知识问答任务。",
            returnDirect = false
    )
    public String askKnowledgeBase(
            @ToolParam(description = "用户问题") String question,
            @ToolParam(description = "检索数量，默认5，范围1-10", required = false) Integer topK) {

        if (!StringUtils.hasText(question)) {
            return error("question 不能为空");
        }
        int finalTopK = normalizeTopK(topK);

        try {
            List<Document> docs = knowledgeBaseService.search(question.trim(), finalTopK);
            String answer = knowledgeBaseService.askWithDocs(question.trim(), docs);

            List<Map<String, Object>> sources = new ArrayList<>();
            if (docs != null) {
                for (Document doc : docs) {
                    Map<String, Object> source = new LinkedHashMap<>();
                    Map<String, Object> metadata = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
                    source.put("source", metadata.getOrDefault("source", "未知"));
                    source.put("articleId", metadata.getOrDefault("articleId", ""));
                    source.put("score", doc.getScore() == null ? 0.0 : doc.getScore());
                    source.put("content", truncate(doc.getText(), SOURCE_PREVIEW_MAX_LENGTH));
                    sources.add(source);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("question", question.trim());
            result.put("topK", finalTopK);
            result.put("answer", answer);
            result.put("sourceCount", sources.size());
            result.put("sources", sources);
            return toJson(result);
        } catch (Exception e) {
            log.error("askKnowledgeBase 调用失败: {}", e.getMessage(), e);
            return error("知识库问答失败: " + e.getMessage());
        }
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

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return toJson(result);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return "{\"success\":false,\"message\":\"序列化失败\"}";
        }
    }
}

