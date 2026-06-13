package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Data
@TableName("user_collect")
@Schema(description = "用户收藏实体类")
public class UserCollect {
    @TableId(type = IdType.AUTO)
    @Schema(description = "收藏ID")
    private Long id;
    
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;
    
    @NotBlank(message = "文章ID不能为空")
    @Schema(description = "文章ID")
    private String articleId;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
} 