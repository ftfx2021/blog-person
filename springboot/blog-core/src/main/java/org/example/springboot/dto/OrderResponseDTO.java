package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 */
@Data
@Schema(description = "订单响应DTO")
public class OrderResponseDTO {
    
    @Schema(description = "订单ID")
    private Long id;
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "商品ID")
    private String productId;
    
    @Schema(description = "商品名称")
    private String productName;
    
    @Schema(description = "商品封面图URL")
    private String productCoverUrl;
    
    @Schema(description = "订单金额（元）")
    private BigDecimal orderAmount;
    
    @Schema(description = "支付方式（1:支付宝, 2:微信）")
    private Integer payType;
    
    @Schema(description = "支付方式名称")
    private String payTypeName;
    
    @Schema(description = "支付时间")
    private LocalDateTime payTime;
    
    @Schema(description = "订单状态（1:待支付, 2:已支付, 3:已完成）")
    private Integer orderStatus;
    
    @Schema(description = "订单状态名称")
    private String orderStatusName;
    
    @Schema(description = "第三方支付流水号")
    private String thirdPayNo;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
