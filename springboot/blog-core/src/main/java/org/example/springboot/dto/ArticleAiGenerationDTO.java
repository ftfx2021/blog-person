package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章AI生成请求DTO
 */
@Data
@Schema(description = "文章AI生成请求")
public class ArticleAiGenerationDTO {

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容", required = true)
    private String content;

    @Schema(description = "当前标题（可选）")
    private String currentTitle;

    @Schema(description = "目标摘要长度（可选）")
    private Integer targetSummaryLength;

    @Schema(description = "是否包含阅读时间估算")
    private Boolean includeReadingTimeEstimation = true;

    @Schema(description = "生成类型：title,summary,outline,tags,category,all")
    private String generationType = "all";
}


