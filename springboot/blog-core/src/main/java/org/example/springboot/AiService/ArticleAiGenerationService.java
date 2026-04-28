package org.example.springboot.AiService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 文章智能生成AI服务
 * 基于Spring AI实现的文章内容智能分析和生成系统
 * 提供标题生成、摘要生成、大纲生成、标签推荐、分类推荐等功能
 */
@Slf4j
@Service
public class ArticleAiGenerationService {

    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Resource
    private Tools tools;

    /**
     * 智能生成文章标题
     * 
     * @param content 文章内容
     * @param currentTitle 当前标题（可选）
     * @return 标题生成结果
     */
    public StructOutPut.ArticleTitleResult generateArticleTitle(String content, String currentTitle) {
        log.info("开始智能生成文章标题");
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(PromptManage.ARTICLE_TITLE_GENERATION_PROMPT)
            ));

            String userInput = "请为以下文章内容生成标题：\n\n" + content;
            if (currentTitle != null && !currentTitle.trim().isEmpty()) {
                userInput += "\n\n当前标题：" + currentTitle;
            }
            
            StructOutPut.ArticleTitleResult result = chatClient
                .prompt(prompt)
                .user(userInput)
                .call()
                .entity(StructOutPut.ArticleTitleResult.class);

            log.info("文章标题生成完成: 生成{}个候选标题，推荐索引={}", 
                result.titles().size(), result.recommendedIndex());
            return result;

        } catch (Exception e) {
            log.error("文章标题生成失败: {}", e.getMessage(), e);
            return getDefaultTitleResult();
        }
    }

    /**
     * 智能生成文章摘要
     * 
     * @param content 文章内容
     * @param targetLength 目标长度（可选）
     * @return 摘要生成结果
     */
    public StructOutPut.ArticleSummaryResult generateArticleSummary(String content, Integer targetLength) {
        log.info("开始智能生成文章摘要");
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(PromptManage.ARTICLE_SUMMARY_GENERATION_PROMPT)
            ));

            String userInput = "请为以下文章内容生成摘要：\n\n" + content;
            if (targetLength != null && targetLength > 0) {
                userInput += "\n\n目标摘要长度：" + targetLength + "字";
            }

            StructOutPut.ArticleSummaryResult result = chatClient
                .prompt(prompt)
                .user(userInput)
                .call()
                .entity(StructOutPut.ArticleSummaryResult.class);

            log.info("文章摘要生成完成: 摘要字数={}, 关键要点数={}", 
                result.wordCount(), result.keyPoints().size());
            return result;

        } catch (Exception e) {
            log.error("文章摘要生成失败: {}", e.getMessage(), e);
            return getDefaultSummaryResult();
        }
    }

    /**
     * 智能生成文章大纲
     * 
     * @param content 文章内容
     * @param includeEstimation 是否包含阅读时间估算
     * @return 大纲生成结果
     */
    public StructOutPut.ArticleOutlineResult generateArticleOutline(String content, Boolean includeEstimation) {
        log.info("开始智能生成文章大纲");
        
        try {
            String userInput = String.format(
                "%s\n\n请为以下文章内容生成大纲：\n\n%s", 
                PromptManage.ARTICLE_OUTLINE_GENERATION_PROMPT, content);
            
            if (includeEstimation != null && includeEstimation) {
                userInput += "\n\n请包含阅读时间估算。";
            }

            StructOutPut.ArticleOutlineResult result = chatClient
                .prompt()
                .user(userInput)
                .call()
                .entity(StructOutPut.ArticleOutlineResult.class);

            log.info("文章大纲生成完成: 大纲为={}",
                result.outline());
            return result;

        } catch (Exception e) {
            log.error("文章大纲生成失败: {}", e.getMessage(), e);
            return getDefaultOutlineResult();
        }
    }

    /**
     * 智能推荐文章标签
     * 
     * @param content 文章内容
     * @param title 文章标题
     * @return 标签推荐结果
     */
    public StructOutPut.TagRecommendationResult recommendTags(String content, String title) {
        log.info("开始智能推荐文章标签");
        
        try {
            String userInput = String.format(
                "%s\n\n请为以下文章推荐标签：\n\n标题：%s\n\n内容：%s", 
                PromptManage.TAG_RECOMMENDATION_PROMPT, title, content);

            StructOutPut.TagRecommendationResult result = chatClient
                .prompt()
                .user(userInput)
                .tools(tools)  // 添加工具支持，AI可以调用getAllTags
                .call()
                .entity(StructOutPut.TagRecommendationResult.class);

            log.info("文章标签推荐完成: 推荐{}个标签，置信度={}, 主要类别={}", 
                result.tags().size(), result.confidence(), result.category());
            return result;

        } catch (Exception e) {
            log.error("文章标签推荐失败: {}", e.getMessage(), e);
            return getDefaultTagResult();
        }
    }

    /**
     * 智能推荐文章分类
     * 
     * @param content 文章内容
     * @param title 文章标题
     * @return 分类推荐结果
     */
    public StructOutPut.CategoryRecommendationResult recommendCategory(String content, String title) {
        log.info("开始智能推荐文章分类");
        
        try {
            String userInput = String.format(
                "%s\n\n请为以下文章推荐分类：\n\n标题：%s\n\n内容：%s", 
                PromptManage.CATEGORY_RECOMMENDATION_PROMPT, title, content);

            StructOutPut.CategoryRecommendationResult result = chatClient
                .prompt()
                .user(userInput)
                .tools(tools)  // 添加工具支持，AI可以调用getAllCategories
                .call()
                .entity(StructOutPut.CategoryRecommendationResult.class);

            log.info("文章分类推荐完成: 推荐{}个分类，主要分类={}, 置信度={}", 
                result.categories().size(), result.primaryCategory(), result.confidence());
            return result;

        } catch (Exception e) {
            log.error("文章分类推荐失败: {}", e.getMessage(), e);
            return getDefaultCategoryResult();
        }
    }


    // 默认返回值方法
    private StructOutPut.ArticleTitleResult getDefaultTitleResult() {
        return new StructOutPut.ArticleTitleResult(
            List.of("默认标题"), 0, "AI生成失败，返回默认标题"
        );
    }

    private StructOutPut.ArticleSummaryResult getDefaultSummaryResult() {
        return new StructOutPut.ArticleSummaryResult(
            "暂无摘要", List.of("暂无要点"), 0
        );
    }

    private StructOutPut.ArticleOutlineResult getDefaultOutlineResult() {
        List<StructOutPut.OutlineItem> defaultOutline = List.of(
            new StructOutPut.OutlineItem(1, "概述", "heading-1"),
            new StructOutPut.OutlineItem(2, "背景介绍", "heading-2"),
            new StructOutPut.OutlineItem(2, "主要内容", "heading-3"),
            new StructOutPut.OutlineItem(3, "核心要点", "heading-4"),
            new StructOutPut.OutlineItem(1, "总结", "heading-5")
        );
        return new StructOutPut.ArticleOutlineResult(defaultOutline, 2, 3);
    }

    private StructOutPut.TagRecommendationResult getDefaultTagResult() {
        return new StructOutPut.TagRecommendationResult(
            List.of("默认"), 0.0, "未分类", "AI推荐失败，返回默认标签"
        );
    }

    private StructOutPut.CategoryRecommendationResult getDefaultCategoryResult() {
        StructOutPut.CategoryMatch defaultMatch = new StructOutPut.CategoryMatch(
            "默认分类", 0.0, "AI推荐失败"
        );
        return new StructOutPut.CategoryRecommendationResult(
            List.of(defaultMatch), "默认分类", 0.0, "AI推荐失败，返回默认分类"
        );
    }

}
