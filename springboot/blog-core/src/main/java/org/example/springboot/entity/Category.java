package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
@Schema(description = "文章分类实体类")
public class Category {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1到50个字符之间")
    private String name;

    @Schema(description = "分类描述")
    @Size(max = 200, message = "分类描述不能超过200个字符")
    private String description;

    @Schema(description = "分类主色调(十六进制颜色值)")
    private String mainColor;

    @Schema(description = "父分类ID(0表示顶级分类)")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer orderNum;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    // 非数据库字段
    @TableField(exist = false)
    @Schema(description = "子分类列表")
    private List<Category> children;
    
    @TableField(exist = false)
    @Schema(description = "文章数量")
    private Integer articleCount;
} 