package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
@Schema(description = "评论实体类")
public class Comment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "评论ID")
    private Long id;
    
    @Schema(description = "评论内容")
    private String content;
    
    @Schema(description = "文章ID")
    private String articleId;
    
    @Schema(description = "评论用户ID")
    private Long userId;
    
    @Schema(description = "父评论ID(0表示顶级评论)")
    private Long parentId;
    
    @Schema(description = "回复用户ID(0表示不是回复)")
    private Long toUserId;
    
    @Schema(description = "状态(0:待审核,1:已通过,2:已拒绝)")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 