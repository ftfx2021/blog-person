package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_link")
@Schema(description = "友情链接实体类")
public class FriendLink {
    
    @TableId(type = IdType.AUTO)
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
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 