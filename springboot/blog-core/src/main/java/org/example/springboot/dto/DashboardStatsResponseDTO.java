package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仪表盘统计数据响应DTO
 */
@Data
@Schema(description = "仪表盘统计数据响应DTO")
public class DashboardStatsResponseDTO {
    
    @Schema(description = "文章总数")
    private Long articleCount;
    
    @Schema(description = "评论总数")
    private Long commentCount;
    
    @Schema(description = "用户总数")
    private Long userCount;
    
    @Schema(description = "总访问量")
    private Long viewCount;
}
