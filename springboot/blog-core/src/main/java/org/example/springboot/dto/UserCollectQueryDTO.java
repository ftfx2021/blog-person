package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 用户收藏查询DTO
 */
@Data
@Schema(description = "用户收藏查询DTO")
public class UserCollectQueryDTO {
    
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "当前页码", example = "1")
    private Integer currentPage;
    
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "10")
    private Integer size;
    
    @Schema(description = "用户ID")
    private Long userId;
}

