package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_config")
@Schema(description = "博客配置实体类")
public class BlogConfig {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long id;
    
    @Schema(description = "配置键")
    private String configKey;
    
    @Schema(description = "配置值")
    private String configValue;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 