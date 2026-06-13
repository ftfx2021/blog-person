package org.example.springboot.service.convert;

import org.example.springboot.dto.OrderCreateDTO;
import org.example.springboot.dto.OrderResponseDTO;
import org.example.springboot.dto.OrderUpdateDTO;
import org.example.springboot.entity.Orders;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单实体与DTO转换工具类
 */
public class OrderConvert {
    
    /**
     * OrderCreateDTO 转 Orders实体
     */
    public static Orders toEntity(OrderCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Orders orders = new Orders();
        orders.setProductId(dto.getProductId());
        orders.setPayType(dto.getPayType());
        return orders;
    }
    
    /**
     * OrderUpdateDTO 转 Orders实体
     */
    public static Orders toEntity(OrderUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Orders orders = new Orders();
        orders.setId(dto.getId());
        orders.setOrderStatus(dto.getOrderStatus());
        orders.setPayType(dto.getPayType());
        orders.setPayTime(dto.getPayTime());
        orders.setThirdPayNo(dto.getThirdPayNo());
        return orders;
    }
    
    /**
     * Orders实体 转 OrderResponseDTO
     */
    public static OrderResponseDTO toResponseDTO(Orders orders) {
        if (orders == null) {
            return null;
        }
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(orders.getId());
        dto.setOrderNo(orders.getOrderNo());
        dto.setUserId(orders.getUserId());
        dto.setProductId(orders.getProductId());
        dto.setProductName(orders.getProductName());
        dto.setOrderAmount(orders.getOrderAmount());
        dto.setPayType(orders.getPayType());
        dto.setPayTime(orders.getPayTime());
        dto.setOrderStatus(orders.getOrderStatus());
        dto.setThirdPayNo(orders.getThirdPayNo());
        dto.setCreateTime(orders.getCreateTime());
        dto.setUpdateTime(orders.getUpdateTime());
        
        // 设置支付方式名称
        if (orders.getPayType() != null) {
            switch (orders.getPayType()) {
                case 1:
                    dto.setPayTypeName("支付宝");
                    break;
                case 2:
                    dto.setPayTypeName("微信");
                    break;
                default:
                    dto.setPayTypeName("未知");
            }
        }
        
        // 设置订单状态名称
        if (orders.getOrderStatus() != null) {
            switch (orders.getOrderStatus()) {
                case 1:
                    dto.setOrderStatusName("待支付");
                    break;
                case 2:
                    dto.setOrderStatusName("已支付");
                    break;
                case 3:
                    dto.setOrderStatusName("已完成");
                    break;
                default:
                    dto.setOrderStatusName("未知");
            }
        }
        
        return dto;
    }
    
    /**
     * Orders实体列表 转 OrderResponseDTO列表
     */
    public static List<OrderResponseDTO> toResponseDTOList(List<Orders> ordersList) {
        if (ordersList == null) {
            return null;
        }
        return ordersList.stream()
                .map(OrderConvert::toResponseDTO)
                .collect(Collectors.toList());
    }
}
