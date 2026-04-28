package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag")
@Schema(description = "标签实体类")
public class Tag {
    @TableId(type = IdType.AUTO)
    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 20, message = "标签名称长度必须在1到20个字符之间")
    private String name;

    @Schema(description = "标签颜色")
    @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$", message = "标签颜色必须是有效的十六进制颜色值")
    private String color;

    @Schema(description = "标签文字颜色")
    @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$", message = "标签文字颜色必须是有效的十六进制颜色值")
    private String textColor;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    @Schema(description = "文章数量")
    private Integer articleCount;
} 