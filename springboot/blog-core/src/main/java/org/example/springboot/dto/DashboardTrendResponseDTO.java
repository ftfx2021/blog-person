package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仪表盘趋势数据响应DTO
 */
@Data
@Schema(description = "仪表盘趋势数据响应DTO")
public class DashboardTrendResponseDTO {
    
    @Schema(description = "日期", example = "2024-01-01")
    private String date;
    
    @Schema(description = "数量")
    private Long count;
}
