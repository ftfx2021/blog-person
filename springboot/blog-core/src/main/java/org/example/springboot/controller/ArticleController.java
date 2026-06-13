package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.dto.ArticleAiGenerationDTO;
import org.example.springboot.dto.ArticleCollectUserResponseDTO;
import org.example.springboot.dto.ArticleCreateDTO;
import org.example.springboot.dto.ArticleLikeUserResponseDTO;
import org.example.springboot.dto.ArticlePasswordDTO;
import org.example.springboot.dto.ArticlePasswordResultDTO;
import org.example.springboot.dto.ArticleQueryDTO;
import org.example.springboot.dto.ArticleResponseDTO;
import org.example.springboot.dto.ArticleUpdateDTO;
import org.example.springboot.dto.VectorMetadataDTO;
import org.example.springboot.dto.KnowledgeBaseStatsDTO;
import org.example.springboot.AiService.ArticleAiGenerationService;
import org.example.springboot.AiService.StructOutPut;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.example.springboot.entity.Article;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.service.ArticleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "文章管理接口")
@RestController
@RequestMapping("/article")
public class ArticleController {
    @Resource
    private ArticleService articleService;
    
    @Resource
    private ArticleAiGenerationService articleAiGenerationService;
    
    @Resource
    private KnowledgeBaseService knowledgeBaseService;
    
    @Operation(summary = "分页查询文章")
    @GetMapping("/page")
    public Result<Page<ArticleResponseDTO>> getArticlesByPage(ArticleQueryDTO queryDTO) {
        Page<ArticleResponseDTO> page = articleService.getArticlesByPage(queryDTO);
        return Result.success(page);
    }
    
    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public Result<Article> getArticleById(
            @PathVariable String id,
            @Parameter(description = "访问密码（可选，仅密码保护文章需要）") @RequestParam(required = false) String password) {
        Article article = articleService.getArticleById(id, password);
        // 更新浏览量
        articleService.updateArticleViewCount(id);
        return Result.success(article);
    }
    
    @Operation(summary = "获取推荐文章")
    @GetMapping("/recommend")
    public Result<List<ArticleResponseDTO>> getRecommendArticles(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<ArticleResponseDTO> articles = articleService.getRecommendArticles(limit);
        return Result.success(articles);
    }
    
    @Operation(summary = "获取热门文章")
    @GetMapping("/hot")
    public Result<List<ArticleResponseDTO>> getHotArticles(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<ArticleResponseDTO> articles = articleService.getHotArticles(limit);
        return Result.success(articles);
    }
    
    @Operation(summary = "新增文章")
    @PostMapping
    public Result<?> addArticle(@RequestBody ArticleCreateDTO createDTO) {
        articleService.addArticle(createDTO);
        return Result.success();
    }
    
    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public Result<?> updateArticle(@PathVariable String id, @RequestBody ArticleUpdateDTO updateDTO) {
        articleService.updateArticle(id, updateDTO);
        return Result.success();
    }
    
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<?> deleteArticle(@PathVariable String id) {
        articleService.deleteArticle(id);
        return Result.success();
    }
    
    @Operation(summary = "修改文章状态")
    @PutMapping("/{id}/status")
    public Result<?> updateArticleStatus(
            @PathVariable String id,
            @RequestParam Integer status) {
        articleService.updateArticleStatus(id, status);
        return Result.success();
    }
    
    @Operation(summary = "根据分类ID获取文章列表")
    @GetMapping("/category/{categoryId}")
    public Result<Page<Article>> getArticlesByCategoryId(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Article> page = articleService.getArticlesByCategoryId(categoryId, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "根据标签ID获取文章列表")
    @GetMapping("/tag/{tagId}")
    public Result<Page<Article>> getArticlesByTagId(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Article> page = articleService.getArticlesByTagId(tagId, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "搜索文章")
    @GetMapping("/search")
    public Result<Page<Article>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Article> page = articleService.searchArticles(keyword, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取文章点赞用户列表")
    @GetMapping("/{id}/like/users")
    public Result<Page<ArticleLikeUserResponseDTO>> getArticleLikeUsers(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<ArticleLikeUserResponseDTO> page = articleService.getArticleLikeUsers(id, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取文章收藏用户列表")
    @GetMapping("/{id}/collect/users")
    public Result<Page<ArticleCollectUserResponseDTO>> getArticleCollectUsers(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<ArticleCollectUserResponseDTO> page = articleService.getArticleCollectUsers(id, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取最近发布的文章")
    @GetMapping("/recent")
    public Result<List<ArticleResponseDTO>> getRecentArticles(@RequestParam(defaultValue = "5") Integer size) {
//        LOGGER.info("获取最近发布的文章, size={}", size);
        List<ArticleResponseDTO> articles = articleService.getRecentArticles(size);
        return Result.success(articles);
    }
    
    // ========== AI智能生成相关接口 ==========
    
    @Operation(summary = "AI智能生成文章标题")
    @PostMapping("/ai/generate-title")
    public Result<StructOutPut.ArticleTitleResult> generateArticleTitle(@RequestBody ArticleAiGenerationDTO dto) {
        StructOutPut.ArticleTitleResult result = articleAiGenerationService.generateArticleTitle(
            dto.getContent(), dto.getCurrentTitle());
        return Result.success(result);
    }
    
    @Operation(summary = "AI智能生成文章摘要")
    @PostMapping("/ai/generate-summary")
    public Result<StructOutPut.ArticleSummaryResult> generateArticleSummary(@RequestBody ArticleAiGenerationDTO dto) {
        StructOutPut.ArticleSummaryResult result = articleAiGenerationService.generateArticleSummary(
            dto.getContent(), dto.getTargetSummaryLength());
        return Result.success(result);
    }
    
    @Operation(summary = "AI智能生成文章大纲")
    @PostMapping("/ai/generate-outline")
    public Result<StructOutPut.ArticleOutlineResult> generateArticleOutline(@RequestBody ArticleAiGenerationDTO dto) {
        StructOutPut.ArticleOutlineResult result = articleAiGenerationService.generateArticleOutline(
            dto.getContent(), dto.getIncludeReadingTimeEstimation());
        return Result.success(result);
    }
    
    @Operation(summary = "AI智能推荐文章标签")
    @PostMapping("/ai/recommend-tags")
    public Result<StructOutPut.TagRecommendationResult> recommendArticleTags(@RequestBody ArticleAiGenerationDTO dto) {
        String title = dto.getCurrentTitle() != null ? dto.getCurrentTitle() : "无标题";
        StructOutPut.TagRecommendationResult result = articleAiGenerationService.recommendTags(
            dto.getContent(), title);
        return Result.success(result);
    }
    
    @Operation(summary = "AI智能推荐文章分类")
    @PostMapping("/ai/recommend-category")
    public Result<StructOutPut.CategoryRecommendationResult> recommendArticleCategory(@RequestBody ArticleAiGenerationDTO dto) {
        String title = dto.getCurrentTitle() != null ? dto.getCurrentTitle() : "无标题";
        StructOutPut.CategoryRecommendationResult result = articleAiGenerationService.recommendCategory(
            dto.getContent(), title);
        return Result.success(result);
    }
    
    // ========== 文章密码保护相关接口 ==========
    
    @Operation(summary = "设置文章密码保护")
    @PostMapping("/{id}/password")
    public Result<ArticlePasswordResultDTO> setArticlePassword(
            @PathVariable String id,
            @RequestBody ArticlePasswordDTO dto) {
        String password = articleService.setArticlePassword(id, dto.getEnablePassword(), dto.getExpireTime());
        if (dto.getEnablePassword()) {
            return Result.success(ArticlePasswordResultDTO.enabled(password, dto.getExpireTime()));
        } else {
            return Result.success(ArticlePasswordResultDTO.disabled());
        }
    }
    
    @Operation(summary = "检查文章是否需要密码访问")
    @GetMapping("/{id}/password/required")
    public Result<Boolean> isPasswordRequired(@PathVariable String id) {
        boolean required = articleService.isPasswordRequired(id);
        return Result.success(required);
    }
    
    @Operation(summary = "获取文章密码（仅管理员）")
    @GetMapping("/{id}/password")
    public Result<ArticlePasswordResultDTO> getArticlePassword(@PathVariable String id) {
        ArticlePasswordResultDTO result = articleService.getArticlePassword(id);
        return Result.success(result);
    }
    
    @Operation(summary = "重新生成文章密码")
    @PostMapping("/{id}/password/regenerate")
    public Result<ArticlePasswordResultDTO> regenerateArticlePassword(@PathVariable String id) {
        String password = articleService.regenerateArticlePassword(id);
        // 获取文章的过期时间（管理员操作，无需密码）
        Article article = articleService.getArticleById(id, null);
        return Result.success(ArticlePasswordResultDTO.enabled(password, article.getPasswordExpireTime()));
    }
    
    // ========== 向量化相关接口 ==========
    
    @Operation(summary = "获取文章向量元数据")
    @GetMapping("/{id}/vector-metadata")
    public Result<List<VectorMetadataDTO>> getArticleVectorMetadata(@PathVariable String id) {
        // 验证文章是否存在
        try {
            Article article = articleService.getArticleById(id, null);
            if (article == null) {
                return Result.error("404", "文章不存在");
            }
        } catch (ServiceException e) {
            if (e.getMessage().contains("不存在")) {
                return Result.error("404", "文章不存在");
            }
            throw e;
        }
        
        // 调用KnowledgeBaseService获取向量元数据
        try {
            List<VectorMetadataDTO> metadata = knowledgeBaseService.getArticleVectorMetadata(id);
            return Result.success(metadata);
        } catch (ServiceException e) {
            // 处理Milvus不可用的情况
            if (e.getCode() != null && e.getCode().equals("503")) {
                return Result.error("503", e.getMessage());
            }
            throw e;
        }
    }
    
    @Operation(summary = "获取知识库统计信息")
    @GetMapping("/knowledge-base/stats")
    public Result<KnowledgeBaseStatsDTO> getKnowledgeBaseStats() {
        try {
            KnowledgeBaseStatsDTO stats = knowledgeBaseService.getStats();
            return Result.success(stats);
        } catch (ServiceException e) {
            return Result.error(e.getCode() != null ? e.getCode() : "500", e.getMessage());
        }
    }
} 