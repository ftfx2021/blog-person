package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商品分类更新DTO")
public class ProductCategoryUpdateDTO {
    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long id;

    @Schema(description = "父分类ID（0表示一级分类）")
    private Long parentId;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1到50个字符之间")
    private String categoryName;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态（0:禁用, 1:启用）")
    private Integer status;
}
