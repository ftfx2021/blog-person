package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 仪表盘趋势查询DTO
 */
@Data
@Schema(description = "仪表盘趋势查询DTO")
public class DashboardTrendQueryDTO {
    
    @Schema(description = "查询天数", example = "7")
    @Min(value = 1, message = "查询天数不能小于1")
    @Max(value = 365, message = "查询天数不能超过365")
    private Integer days = 7;
}
