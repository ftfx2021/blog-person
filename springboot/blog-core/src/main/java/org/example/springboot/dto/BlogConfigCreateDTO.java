package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 博客配置创建DTO
 */
@Data
@Schema(description = "博客配置创建DTO")
public class BlogConfigCreateDTO {
    
    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键", example = "site_name")
    private String configKey;
    
    @Schema(description = "配置值", example = "我的博客")
    private String configValue;
}
