package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签响应DTO
 */
@Data
@Schema(description = "标签响应DTO")
public class TagResponseDTO {
    
    @Schema(description = "标签ID")
    private Long id;
    
    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签文字颜色")
    private String textColor;

    @Schema(description = "文章数量")
    private Integer articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

