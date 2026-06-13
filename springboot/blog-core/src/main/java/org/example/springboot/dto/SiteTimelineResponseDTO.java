package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 网站发展历程响应DTO
 */
@Data
@Schema(description = "网站发展历程响应DTO")
public class SiteTimelineResponseDTO {
    
    @Schema(description = "事件ID")
    private Long id;
    
    @Schema(description = "事件标题")
    private String title;
    
    @Schema(description = "事件详细描述")
    private String content;
    
    @Schema(description = "事件日期")
    private LocalDate eventDate;
    
    @Schema(description = "图标类名(Font Awesome)")
    private String icon;
    
    @Schema(description = "时间线节点颜色")
    private String color;
    
    @Schema(description = "排序号")
    private Integer orderNum;
    
    @Schema(description = "状态(0:隐藏,1:显示)")
    private Integer status;
    
    @Schema(description = "状态描述")
    private String statusDesc;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "显示" : "隐藏";
    }
}
