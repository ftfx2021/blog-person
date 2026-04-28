package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 网站发展历程更新DTO
 */
@Data
@Schema(description = "网站发展历程更新DTO")
public class SiteTimelineUpdateDTO {
    
    @Schema(description = "事件ID")
    private Long id;
    
    @NotBlank(message = "事件标题不能为空")
    @Size(max = 100, message = "事件标题长度不能超过100个字符")
    @Schema(description = "事件标题", example = "网站正式上线")
    private String title;
    
    @Schema(description = "事件详细描述", example = "经过数月的开发，网站正式上线运营")
    private String content;
    
    @NotNull(message = "事件日期不能为空")
    @Schema(description = "事件日期", example = "2025-01-01")
    private LocalDate eventDate;
    
    @Size(max = 50, message = "图标类名长度不能超过50个字符")
    @Schema(description = "图标类名(Font Awesome)", example = "fa-rocket")
    private String icon;
    
    @Size(max = 20, message = "颜色长度不能超过20个字符")
    @Schema(description = "时间线节点颜色", example = "#409EFF")
    private String color;
    
    @Schema(description = "排序号", example = "1")
    private Integer orderNum;
    
    @Schema(description = "状态(0:隐藏,1:显示)", example = "1")
    private Integer status;
}
