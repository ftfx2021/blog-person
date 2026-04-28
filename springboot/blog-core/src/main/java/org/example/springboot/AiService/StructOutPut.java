package org.example.springboot.AiService;

import java.util.List;

public class StructOutPut {

    /**
     * 文章标题生成结果
     * 
     * @param titles 生成的标题列表，包含3-5个候选标题
     * @param recommendedIndex 推荐的标题索引（0-based）
     * @param reason 推荐理由
     */
    public record ArticleTitleResult(
        List<String> titles,
        Integer recommendedIndex,
        String reason
    ) {}

    /**
     * 文章摘要生成结果
     * 
     * @param summary 生成的文章摘要
     * @param keyPoints 关键要点列表
     * @param wordCount 摘要字数
     */
    public record ArticleSummaryResult(
        String summary,
        List<String> keyPoints,
        Integer wordCount
    ) {}

    /**
     * 文章大纲生成结果
     * 
     * @param outline 结构化大纲，扁平化列表
     * @param totalSections 总章节数
     * @param estimatedReadingTime 预计阅读时间（分钟）
     */
    public record ArticleOutlineResult(
        List<OutlineItem> outline,
        Integer totalSections,
        Integer estimatedReadingTime
    ) {}

    /**
     * 大纲项目（包含锚点ID）
     * 
     * @param level 层级（1-4对应h1-h4）
     * @param title 标题
     * @param anchorId 锚点ID（可选，用于精确定位）
     */
    public record OutlineItem(
        Integer level,
        String title,
        String anchorId
    ) {}

    /**
     * 标签推荐结果
     * 
     * @param tags 推荐的标签列表
     * @param confidence 推荐置信度（0-1）
     * @param category 推荐的主要类别
     * @param reason 推荐理由
     */
    public record TagRecommendationResult(
        List<String> tags,
        Double confidence,
        String category,
        String reason
    ) {}

    /**
     * 分类推荐结果
     * 
     * @param categories 推荐的分类列表，按匹配度排序
     * @param primaryCategory 主要推荐分类
     * @param confidence 推荐置信度（0-1）
     * @param reason 推荐理由
     */
    public record CategoryRecommendationResult(
        List<CategoryMatch> categories,
        String primaryCategory,
        Double confidence,
        String reason
    ) {}

    /**
     * 分类匹配结果
     * 
     * @param name 分类名称
     * @param score 匹配分数（0-1）
     * @param reason 匹配理由
     */
    public record CategoryMatch(
        String name,
        Double score,
        String reason
    ) {}

}
