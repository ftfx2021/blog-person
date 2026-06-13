package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客配置响应DTO
 */
@Data
@Schema(description = "博客配置响应DTO")
public class BlogConfigResponseDTO {
    
    @Schema(description = "配置ID")
    private Long id;
    
    @Schema(description = "配置键")
    private String configKey;
    
    @Schema(description = "配置值")
    private String configValue;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
