package org.example.springboot.agent.learn.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.Category;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.CategoryMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习版：文章关键词搜索工具
 * 优先 RAG，空结果再回退 MySQL 模糊搜索
 */
@Slf4j
@Component
public class ArticleSearchTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;

    private final KnowledgeBaseService knowledgeBaseService;
    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ArticleSearchTool(KnowledgeBaseService knowledgeBaseService,
                             ArticleMapper articleMapper,
                             CategoryMapper categoryMapper,
                             UserMapper userMapper,
                             ObjectMapper objectMapper) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据关键词搜索文章（优先 RAG，为空则回退 MySQL）
     */
    @Tool(
            name = "searchArticlesByKeyword",
            description = "根据关键词搜索文章基础信息（标题、ID、摘要等）。优先使用RAG语义检索，若无结果则回退MySQL模糊搜索。",
            returnDirect = false
    )
    public String searchArticlesByKeyword(
            @ToolParam(description = "搜索关键词，例如：Spring AI") String keyword,
            @ToolParam(description = "返回数量，默认5，范围1-20", required = false) Integer limit) {

        if (!StringUtils.hasText(keyword)) {
            return error("keyword 不能为空");
        }

        int finalLimit = normalizeLimit(limit);
        String query = keyword.trim();
        String ragError = null;

        try {
            List<Article> ragArticles = searchByRag(query, finalLimit);
            if (!ragArticles.isEmpty()) {
                return success("rag", query, ragArticles, ragError);
            }
        } catch (Exception e) {
            ragError = e.getMessage();
            log.warn("RAG检索失败，回退MySQL搜索: keyword={}, error={}", query, ragError);
        }

        List<Article> mysqlArticles = searchByMysql(query, finalLimit);
        return success("mysql", query, mysqlArticles, ragError);
    }

    private List<Article> searchByRag(String keyword, int limit) {
        List<Document> docs = knowledgeBaseService.search(keyword, limit);
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        Set<String> articleIdOrder = new LinkedHashSet<>();
        for (Document doc : docs) {
            if (doc.getMetadata() == null) {
                continue;
            }
            Object articleId = doc.getMetadata().get("articleId");
            if (articleId != null && StringUtils.hasText(articleId.toString())) {
                articleIdOrder.add(articleId.toString());
            }
        }
        if (articleIdOrder.isEmpty()) {
            return List.of();
        }

        List<Article> articles = articleMapper.selectBatchIds(articleIdOrder);
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        Map<String, Article> articleMap = articles.stream()
                .filter(a -> a != null && Integer.valueOf(1).equals(a.getStatus()))
                .collect(Collectors.toMap(Article::getId, a -> a, (a, b) -> a));

        List<Article> ordered = new ArrayList<>();
        for (String articleId : articleIdOrder) {
            Article article = articleMap.get(articleId);
            if (article != null) {
                ordered.add(article);
            }
            if (ordered.size() >= limit) {
                break;
            }
        }
        return ordered;
    }

    private List<Article> searchByMysql(String keyword, int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1)
                .and(w -> w.like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword)
                        .or().like(Article::getContent, keyword))
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT " + limit);
        return articleMapper.selectList(queryWrapper);
    }

    private String success(String source, String keyword, List<Article> articles, String ragError) {
        Map<Long, String> categoryNameMap = loadCategoryNameMap(articles);
        Map<Long, String> authorNameMap = loadAuthorNameMap(articles);

        String itemSourceLabel = "rag".equals(source) ? "向量库" : "MySQL";
        List<Map<String, Object>> items = new ArrayList<>();
        for (Article article : articles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", article.getId());
            item.put("title", article.getTitle());
            item.put("summary", article.getSummary());
            item.put("source", itemSourceLabel);
            item.put("categoryId", article.getCategoryId());
            item.put("categoryName", categoryNameMap.get(article.getCategoryId()));
            item.put("authorId", article.getUserId());
            item.put("authorName", authorNameMap.get(article.getUserId()));
            item.put("coverImage", article.getCoverImage());
            item.put("viewCount", article.getViewCount());
            item.put("createTime", article.getCreateTime());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("source", source);
        result.put("sourceLabel", "rag".equals(source) ? "向量库" : "MySQL");
        result.put("keyword", keyword);
        result.put("count", items.size());
        result.put("items", items);
        if (StringUtils.hasText(ragError)) {
            result.put("ragError", ragError);
        }
        return toJson(result);
    }

    private Map<Long, String> loadCategoryNameMap(List<Article> articles) {
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        return categories.stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, String> loadAuthorNameMap(List<Article> articles) {
        Set<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
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
