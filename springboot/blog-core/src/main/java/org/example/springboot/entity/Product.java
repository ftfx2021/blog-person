package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
@Schema(description = "商品实体类")
public class Product {
    @TableId(type = IdType.INPUT)
    @Schema(description = "商品ID（UUID）")
    private String id;

    @Schema(description = "商品名称")
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 100, message = "商品名称长度必须在1到100个字符之间")
    private String productName;

    @Schema(description = "商品简介")
    @Size(max = 500, message = "商品简介不能超过500个字符")
    private String productDesc;

    @Schema(description = "商品详情（富文本）")
    private String productDetail;

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "封面图URL")
    private String coverImageUrl;

    @Schema(description = "演示图片URL列表（JSON数组）")
    private String demoImageUrls;

    @Schema(description = "价格（元）")
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    @Schema(description = "下载链接")
    @Size(max = 500, message = "下载链接不能超过500个字符")
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
}
