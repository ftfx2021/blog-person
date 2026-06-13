package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已购商品响应DTO
 */
@Data
@Schema(description = "已购商品响应DTO")
public class PurchasedProductResponseDTO {
    
    @Schema(description = "商品ID")
    private String productId;
    
    @Schema(description = "商品名称")
    private String productName;
    
    @Schema(description = "商品简介")
    private String productDesc;
    
    @Schema(description = "商品封面图URL")
    private String coverImageUrl;
    
    @Schema(description = "购买价格")
    private BigDecimal purchasePrice;
    
    @Schema(description = "下载链接")
    private String downloadLink;
    
    @Schema(description = "购买时间（支付时间）")
    private LocalDateTime purchaseTime;
    
    @Schema(description = "订单ID")
    private Long orderId;
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "下载次数")
    private Integer downloadCount;
}
