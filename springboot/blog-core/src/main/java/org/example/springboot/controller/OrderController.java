package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.OrderCreateDTO;
import org.example.springboot.dto.OrderResponseDTO;
import org.example.springboot.dto.OrderUpdateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.OrderService;

import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单管理接口
 */
@Tag(name = "订单管理接口")
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {
    
    @Resource
    private OrderService orderService;
    
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderResponseDTO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        OrderResponseDTO order = orderService.createOrder(dto, userId);
        return Result.success(order);
    }
    
    @Operation(summary = "分页查询订单列表")
    @GetMapping("/page")
    public Result<Page<OrderResponseDTO>> getOrderPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "用户ID（管理员可查询所有用户订单）") @RequestParam(required = false) Long userId,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer orderStatus) {
        
        // 如果不是管理员，只能查询自己的订单
        String roleCode = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(roleCode)) {
            userId = JwtTokenUtils.getCurrentUserId();
        }
        
        log.info("订单分页查询: current={}, size={}, userId={}, productName={}, orderStatus={}", 
                 current, size, userId, productName, orderStatus);
        Page<OrderResponseDTO> pageResult = orderService.getOrderPage(current, size, userId, productName, orderStatus);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "根据ID获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrderById(id);
        
        // 权限检查：只能查看自己的订单（管理员除外）
        String roleCode = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(roleCode)) {
            Long currentUserId = JwtTokenUtils.getCurrentUserId();
            if (!currentUserId.equals(order.getUserId())) {
                return Result.error("无权查看此订单");
            }
        }
        
        return Result.success(order);
    }
    
    @Operation(summary = "根据订单号获取订单详情")
    @GetMapping("/no/{orderNo}")
    public Result<OrderResponseDTO> getOrderByOrderNo(@PathVariable String orderNo) {
        OrderResponseDTO order = orderService.getOrderByOrderNo(orderNo);
        
        // 权限检查：只能查看自己的订单（管理员除外）
        String roleCode = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(roleCode)) {
            Long currentUserId = JwtTokenUtils.getCurrentUserId();
            if (!currentUserId.equals(order.getUserId())) {
                return Result.error("无权查看此订单");
            }
        }
        
        return Result.success(order);
    }
    
    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    public Result<?> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateDTO dto) {
        dto.setId(id);
        orderService.updateOrder(dto);
        return Result.success();
    }
    
    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public Result<?> updateOrderStatus(
            @PathVariable Long id,
            @Parameter(description = "订单状态（1:待支付, 2:已支付, 3:已完成）") @RequestParam Integer orderStatus) {
        orderService.updateOrderStatus(id, orderStatus);
        return Result.success();
    }
    

    
    @Operation(summary = "检查用户是否已购买商品")
    @GetMapping("/check-purchased")
    public Result<Boolean> checkPurchased(@RequestParam String productId) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        boolean purchased = orderService.hasUserPurchasedProduct(userId, productId);
        return Result.success(purchased);
    }
    
    @Operation(summary = "获取用户已购买的商品ID列表")
    @GetMapping("/purchased-products")
    public Result<List<String>> getPurchasedProducts() {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<String> productIds = orderService.getUserPurchasedProductIds(userId);
        return Result.success(productIds);
    }
    
    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @RequestBody Map<String, Object> payParams) {
        
        Integer payType = (Integer) payParams.get("payType");
        String thirdPayNo = (String) payParams.get("thirdPayNo");
        
        log.info("支付订单请求: 订单ID={}, 支付方式={}, 流水号={}", id, payType, thirdPayNo);
        orderService.payOrder(id, payType, thirdPayNo);
        return Result.success();
    }
}
