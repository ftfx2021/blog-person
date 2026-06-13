package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新句子 DTO
 */
@Data
@Schema(description = "更新句子请求DTO")
public class MySentenceUpdateDTO {

    @NotBlank(message = "句子内容不能为空")
    @Schema(description = "句子内容")
    private String sentenceContent;

    @Schema(description = "关键词，多个用逗号隔开")
    private String keywords;

    @Schema(description = "是否放到首页：0-否，1-是")
    private Integer isHomepage;
}
