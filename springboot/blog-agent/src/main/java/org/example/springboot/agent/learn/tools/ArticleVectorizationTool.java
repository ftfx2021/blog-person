package org.example.springboot.agent.learn.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.example.springboot.config.rabbitmq.ArticleRabbitMqConfig;
import org.example.springboot.entity.Article;
import org.example.springboot.enums.ArticleVectorizedStatus;
import org.example.springboot.mapper.ArticleMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学习版：文章向量化工具
 */
@Slf4j
@Component
public class ArticleVectorizationTool {

    private final ArticleMapper articleMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ArticleVectorizationTool(ArticleMapper articleMapper,
                                    KnowledgeBaseService knowledgeBaseService,
                                    RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper) {
        this.articleMapper = articleMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 对文章执行向量化，支持同步/异步两种模式
     */
    @Tool(
            name = "vectorizeArticle",
            description = "对指定文章执行向量化。mode 可选 async 或 sync：async 表示加入消息队列后台执行，sync 表示立即执行并返回分片数。",
            returnDirect = false
    )
    public String vectorizeArticle(
            @ToolParam(description = "文章ID") String articleId,
            @ToolParam(description = "执行模式：async(默认) 或 sync", required = false) String mode) {

        if (!StringUtils.hasText(articleId)) {
            return error("articleId 不能为空");
        }

        Article article = articleMapper.selectById(articleId.trim());
        if (article == null) {
            return error("文章不存在: " + articleId);
        }

        String normalizedMode = normalizeMode(mode);
        try {
            if ("sync".equals(normalizedMode)) {
                return doSync(article.getId());
            }
            return doAsync(article);
        } catch (Exception e) {
            log.error("文章向量化工具执行失败: articleId={}, mode={}, error={}", articleId, normalizedMode, e.getMessage(), e);
            return error("文章向量化失败: " + e.getMessage());
        }
    }

    /**
     * 查询文章向量化状态
     */
    @Tool(
            name = "getArticleVectorizeStatus",
            description = "根据文章ID查询向量化状态，返回状态码与最近错误信息。",
            returnDirect = false
    )
    public String getArticleVectorizeStatus(
            @ToolParam(description = "文章ID") String articleId) {

        if (!StringUtils.hasText(articleId)) {
            return error("articleId 不能为空");
        }
        Article article = articleMapper.selectById(articleId.trim());
        if (article == null) {
            return error("文章不存在: " + articleId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("articleId", article.getId());
        result.put("title", article.getTitle());
        result.put("vectorizedStatus", article.getVectorizedStatus());
        result.put("vectorizedErrorReason", article.getVectorizedErrorReason());
        result.put("lastVectorizedSuccessTime", article.getLastVectorizedSuccessTime());
        result.put("vectorizedProcessAt", article.getVectorizedProcessAt());
        result.put("vectorizedRetryCount", article.getVectorizedRetryCount());
        return toJson(result);
    }

    private String doAsync(Article article) {
        article.setVectorizedStatus(ArticleVectorizedStatus.NO.getCode());
        article.setVectorizedProcessAt(null);
        article.setLastVectorizedSuccessTime(null);
        article.setVectorizedRetryCount(0);
        article.setVectorizedErrorReason(null);
        articleMapper.updateById(article);

        rabbitTemplate.convertAndSend(
                ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                ArticleRabbitMqConfig.ARTICLE_VECTORIZED_ROUTING_KEY,
                article
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("mode", "async");
        result.put("articleId", article.getId());
        result.put("title", article.getTitle());
        result.put("message", "已加入向量化队列");
        result.put("routingKey", ArticleRabbitMqConfig.ARTICLE_VECTORIZED_ROUTING_KEY);
        return toJson(result);
    }

    private String doSync(String articleId) {
        int chunkSize = knowledgeBaseService.syncArticle(articleId);
        Article latest = articleMapper.selectById(articleId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("mode", "sync");
        result.put("articleId", articleId);
        result.put("chunkSize", chunkSize);
        result.put("vectorizedStatus", latest == null ? null : latest.getVectorizedStatus());
        result.put("vectorizedErrorReason", latest == null ? null : latest.getVectorizedErrorReason());
        result.put("message", chunkSize > 0 ? "同步向量化完成" : "同步向量化执行结束（可能被跳过或失败，请查看状态）");
        return toJson(result);
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "async";
        }
        String m = mode.trim().toLowerCase();
        return "sync".equals(m) ? "sync" : "async";
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
