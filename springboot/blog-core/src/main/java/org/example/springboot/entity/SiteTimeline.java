package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 网站发展历程实体类
 */
@Data
@TableName("site_timeline")
@Schema(description = "网站发展历程实体类")
public class SiteTimeline {
    
    @TableId(type = IdType.AUTO)
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
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
