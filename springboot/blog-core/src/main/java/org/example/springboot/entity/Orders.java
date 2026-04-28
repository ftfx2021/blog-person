package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
@Schema(description = "订单实体类")
public class Orders {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;
    
    @Schema(description = "订单号（唯一）")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Schema(description = "商品ID（UUID）")
    @NotBlank(message = "商品ID不能为空")
    private String productId;
    
    @Schema(description = "商品名称（冗余字段）")
    private String productName;
    
    @Schema(description = "订单金额（元）")
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.00", message = "订单金额不能小于0")
    private BigDecimal orderAmount;
    
    @Schema(description = "支付方式（1:支付宝, 2:微信）")
    private Integer payType;
    
    @Schema(description = "支付时间")
    private LocalDateTime payTime;
    
    @Schema(description = "订单状态（1:待支付, 2:已支付, 3:已完成）")
    private Integer orderStatus;
    
    @Schema(description = "第三方支付流水号")
    private String thirdPayNo;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
