package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新订单DTO
 */
@Data
@Schema(description = "更新订单DTO")
public class OrderUpdateDTO {
    
    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long id;
    
    @Schema(description = "订单状态（1:待支付, 2:已支付, 3:已完成）")
    private Integer orderStatus;
    
    @Schema(description = "支付方式（1:支付宝, 2:微信）")
    private Integer payType;
    
    @Schema(description = "支付时间")
    private LocalDateTime payTime;
    
    @Schema(description = "第三方支付流水号")
    private String thirdPayNo;
}
