package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_category")
@Schema(description = "商品分类实体类")
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID（0表示一级分类）")
    private Long parentId;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1到50个字符之间")
    private String categoryName;

    @Schema(description = "分类层级（1:一级, 2:二级）")
    private Integer level;

    @Schema(description = "排序号（数字越小越靠前）")
    private Integer sortOrder;

    @Schema(description = "状态（0:禁用, 1:启用）")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
