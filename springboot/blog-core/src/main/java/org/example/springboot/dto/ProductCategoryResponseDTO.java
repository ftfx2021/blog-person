package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "商品分类响应DTO")
public class ProductCategoryResponseDTO {
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID（0表示一级分类）")
    private Long parentId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类层级（1:一级, 2:二级）")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态（0:禁用, 1:启用）")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "商品数量")
    private Integer productCount;

    @Schema(description = "子分类列表")
    private List<ProductCategoryResponseDTO> children;
}
