package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建订单DTO
 */
@Data
@Schema(description = "创建订单请求DTO")
public class OrderCreateDTO {
    
    @Schema(description = "商品ID（UUID）")
    @NotBlank(message = "商品ID不能为空")
    private String productId;
    
    @Schema(description = "支付方式（1:支付宝, 2:微信）", example = "1")
    private Integer payType;
}
