package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.OrderCreateDTO;
import org.example.springboot.dto.OrderResponseDTO;
import org.example.springboot.dto.OrderUpdateDTO;
import org.example.springboot.entity.Orders;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.User;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.OrdersMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.convert.OrderConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 订单服务类
 */
@Slf4j
@Service
public class OrderService {
    
    @Resource
    private OrdersMapper ordersMapper;
    
    @Resource
    private ProductMapper productMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseDTO createOrder(OrderCreateDTO dto, Long userId) {
        log.info("创建订单，用户ID: {}, 商品ID: {}", userId, dto.getProductId());
        
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 检查商品是否存在
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 检查商品是否上架
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品已下架，无法购买");
        }
        
        // 生成订单号
        String orderNo = generateOrderNo();
        
        // 创建订单
        Orders orders = new Orders();
        orders.setOrderNo(orderNo);
        orders.setUserId(userId);
        orders.setProductId(dto.getProductId());
        orders.setProductName(product.getProductName());
        orders.setOrderAmount(product.getPrice());
        orders.setPayType(dto.getPayType());
        orders.setOrderStatus(1); // 待支付
        
        ordersMapper.insert(orders);
        
        log.info("订单创建成功，订单号: {}", orderNo);
        
        // 返回订单详情
        return getOrderById(orders.getId());
    }
    
    /**
     * 分页查询订单列表
     */
    public Page<OrderResponseDTO> getOrderPage(Long current, Long size, Long userId, 
                                                String productName, Integer orderStatus) {
        log.info("分页查询订单列表，页码: {}, 每页数量: {}, 用户ID: {}, 商品名称: {}, 订单状态: {}", 
                 current, size, userId, productName, orderStatus);
        
        Page<Orders> page = new Page<>(current, size);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(Orders::getUserId, userId);
        }
        if (StrUtil.isNotBlank(productName)) {
            wrapper.like(Orders::getProductName, productName);
        }
        if (orderStatus != null) {
            wrapper.eq(Orders::getOrderStatus, orderStatus);
        }
        
        wrapper.orderByDesc(Orders::getCreateTime);
        
        Page<Orders> ordersPage = ordersMapper.selectPage(page, wrapper);
        
        // 转换为ResponseDTO并填充用户信息和商品封面
        Page<OrderResponseDTO> resultPage = new Page<>(ordersPage.getCurrent(), ordersPage.getSize(), ordersPage.getTotal());
        List<OrderResponseDTO> dtoList = OrderConvert.toResponseDTOList(ordersPage.getRecords());
        
        for (OrderResponseDTO dto : dtoList) {
            enrichOrderInfo(dto);
        }
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }
    
    /**
     * 根据ID获取订单详情
     */
    public OrderResponseDTO getOrderById(Long id) {
        log.info("查询订单详情，ID: {}", id);
        Orders orders = ordersMapper.selectById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        
        OrderResponseDTO dto = OrderConvert.toResponseDTO(orders);
        enrichOrderInfo(dto);
        return dto;
    }
    
    /**
     * 根据订单号获取订单详情
     */
    public OrderResponseDTO getOrderByOrderNo(String orderNo) {
        log.info("根据订单号查询订单详情，订单号: {}", orderNo);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getOrderNo, orderNo);
        Orders orders = ordersMapper.selectOne(wrapper);
        
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        
        OrderResponseDTO dto = OrderConvert.toResponseDTO(orders);
        enrichOrderInfo(dto);
        return dto;
    }
    
    /**
     * 更新订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(OrderUpdateDTO dto) {
        log.info("更新订单，ID: {}", dto.getId());
        
        // 检查订单是否存在
        Orders existingOrder = ordersMapper.selectById(dto.getId());
        if (existingOrder == null) {
            throw new BusinessException("订单不存在");
        }
        
        Orders orders = OrderConvert.toEntity(dto);
        ordersMapper.updateById(orders);
        
        log.info("订单更新成功");
    }
    
    /**
     * 更新订单状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long id, Integer orderStatus) {
        log.info("更新订单状态，ID: {}, 状态: {}", id, orderStatus);
        
        Orders orders = ordersMapper.selectById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        
        orders.setOrderStatus(orderStatus);
        
        // 如果状态更新为已支付，设置支付时间
        if (orderStatus == 2 && orders.getPayTime() == null) {
            orders.setPayTime(LocalDateTime.now());
        }
        
        ordersMapper.updateById(orders);
        
        // 如果订单状态变为已支付或已完成，更新商品销量
        if (orderStatus == 2 || orderStatus == 3) {
            updateProductSaleCount(orders.getProductId());
        }
        
        log.info("订单状态更新成功");
    }
    
    /**
     * 支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id, Integer payType, String thirdPayNo) {
        log.info("支付订单，ID: {}, 支付方式: {}, 第三方流水号: {}", id, payType, thirdPayNo);
        
        Orders orders = ordersMapper.selectById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (orders.getOrderStatus() != 1) {
            throw new BusinessException("订单状态不正确，无法支付");
        }
        
        orders.setOrderStatus(2); // 已支付
        orders.setPayType(payType);
        orders.setPayTime(LocalDateTime.now());
        orders.setThirdPayNo(thirdPayNo);
        
        ordersMapper.updateById(orders);
        
        // 更新商品销量
        updateProductSaleCount(orders.getProductId());
        
        log.info("订单支付成功");
    }
    
    /**
     * 检查用户是否已购买商品
     */
    public boolean hasUserPurchasedProduct(Long userId, String productId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .eq(Orders::getProductId, productId)
               .in(Orders::getOrderStatus, 2, 3); // 已支付或已完成
        
        Long count = ordersMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
    
    /**
     * 获取用户已购买的商品列表
     */
    public List<String> getUserPurchasedProductIds(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .in(Orders::getOrderStatus, 2, 3) // 已支付或已完成
               .select(Orders::getProductId);
        
        List<Orders> orders = ordersMapper.selectList(wrapper);
        return orders.stream()
                .map(Orders::getProductId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 生成订单号
     * 格式：yyyyMMddHHmmss + 6位随机数
     */
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNum = random.nextInt(900000) + 100000; // 生成6位随机数
        return timestamp + randomNum;
    }
    
    /**
     * 丰富订单信息（填充用户名和商品封面）
     */
    private void enrichOrderInfo(OrderResponseDTO dto) {
        // 填充用户名
        if (dto.getUserId() != null) {
            User user = userMapper.selectById(dto.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
            }
        }
        
        // 填充商品封面图URL - 使用新的文件路径字段
        if (StrUtil.isNotBlank(dto.getProductId())) {
            Product product = productMapper.selectById(dto.getProductId());
            if (product != null && StrUtil.isNotBlank(product.getCoverImageUrl())) {
                dto.setProductCoverUrl(product.getCoverImageUrl());
            }
        }
    }
    
    /**
     * 更新商品销量
     */
    private void updateProductSaleCount(String productId) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            Integer saleCount = product.getSaleCount() != null ? product.getSaleCount() : 0;
            product.setSaleCount(saleCount + 1);
            productMapper.updateById(product);
            log.info("商品销量更新成功，商品ID: {}, 新销量: {}", productId, product.getSaleCount());
        }
    }
}
