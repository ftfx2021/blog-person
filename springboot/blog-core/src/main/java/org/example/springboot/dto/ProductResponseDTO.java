package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "商品响应DTO")
public class ProductResponseDTO {
    @Schema(description = "商品ID")
    private String id;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品简介")
    private String productDesc;

    @Schema(description = "商品详情（富文本）")
    private String productDetail;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "封面图URL")
    private String coverImageUrl;

    @Schema(description = "价格（元）")
    private BigDecimal price;

    @Schema(description = "下载链接")
    private String downloadLink;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "销售数量")
    private Integer saleCount;

    @Schema(description = "状态（0:下架, 1:上架）")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "演示图片列表")
    private List<ProductImageDTO> images;

    /**
     * 商品图片DTO
     */
    @Data
    @Schema(description = "商品图片DTO")
    public static class ProductImageDTO {
        @Schema(description = "文件ID")
        private Long id;

        @Schema(description = "文件路径")
        private String filePath;

        @Schema(description = "排序号")
        private Integer sortOrder;
    }
}
