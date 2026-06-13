package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 友情链接响应DTO
 */
@Data
@Schema(description = "友情链接响应DTO")
public class FriendLinkResponseDTO {
    
    @Schema(description = "链接ID")
    private Long id;
    
    @Schema(description = "链接名称")
    private String name;
    
    @Schema(description = "链接地址")
    private String url;
    
    @Schema(description = "链接logo")
    private String logo;
    
    @Schema(description = "链接描述")
    private String description;
    
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


