package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 句子响应 DTO
 */
@Data
@Schema(description = "句子响应DTO")
public class MySentenceResponseDTO {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "句子内容")
    private String sentenceContent;

    @Schema(description = "关键词，多个用逗号隔开")
    private String keywords;

    @Schema(description = "是否放到首页：0-否，1-是")
    private Integer isHomepage;
}
