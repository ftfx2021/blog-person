package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.DownloadRecordResponseDTO;
import org.example.springboot.dto.PurchasedProductResponseDTO;
import org.example.springboot.entity.DownloadRecord;
import org.example.springboot.entity.Orders;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.User;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.DownloadRecordMapper;
import org.example.springboot.mapper.OrdersMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 下载服务类
 */
@Slf4j
@Service
public class DownloadService {
    
    @Resource
    private DownloadRecordMapper downloadRecordMapper;
    
    @Resource
    private OrdersMapper ordersMapper;
    
    @Resource
    private ProductMapper productMapper;
    
    @Resource
    private UserMapper userMapper;
    

    
    /**
     * 获取用户已购商品列表（分页）
     */
    public Page<PurchasedProductResponseDTO> getPurchasedProducts(Long userId, Long current, Long size, String productName) {
        log.info("查询用户已购商品列表，用户ID: {}, 页码: {}, 每页数量: {}, 商品名称: {}", 
                 userId, current, size, productName);
        
        // 查询用户已支付的订单
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .in(Orders::getOrderStatus, 2, 3) // 已支付或已完成
               .orderByDesc(Orders::getPayTime);
        
        if (StrUtil.isNotBlank(productName)) {
            wrapper.like(Orders::getProductName, productName);
        }
        
        Page<Orders> orderPage = new Page<>(current, size);
        Page<Orders> ordersPage = ordersMapper.selectPage(orderPage, wrapper);
        
        // 转换为已购商品DTO
        Page<PurchasedProductResponseDTO> resultPage = new Page<>(ordersPage.getCurrent(), ordersPage.getSize(), ordersPage.getTotal());
        List<PurchasedProductResponseDTO> dtoList = new ArrayList<>();
        
        for (Orders order : ordersPage.getRecords()) {
            PurchasedProductResponseDTO dto = new PurchasedProductResponseDTO();
            dto.setProductId(order.getProductId());
            dto.setProductName(order.getProductName());
            dto.setPurchasePrice(order.getOrderAmount());
            dto.setPurchaseTime(order.getPayTime());
            dto.setOrderId(order.getId());
            dto.setOrderNo(order.getOrderNo());
            
            // 填充商品详细信息
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                dto.setProductDesc(product.getProductDesc());
                dto.setDownloadLink(product.getDownloadLink());
                
                // 填充封面图 - 使用新的文件路径字段
                if (StrUtil.isNotBlank(product.getCoverImageUrl())) {
                    dto.setCoverImageUrl(product.getCoverImageUrl());
                }
            }
            
            // 查询下载次数
            LambdaQueryWrapper<DownloadRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(DownloadRecord::getUserId, userId)
                        .eq(DownloadRecord::getProductId, order.getProductId());
            Long downloadCount = downloadRecordMapper.selectCount(recordWrapper);
            dto.setDownloadCount(downloadCount != null ? downloadCount.intValue() : 0);
            
            dtoList.add(dto);
        }
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }
    
    /**
     * 记录下载行为
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordDownload(Long userId, String productId, HttpServletRequest request) {
        log.info("记录下载行为，用户ID: {}, 商品ID: {}", userId, productId);
        
        // 检查用户是否已购买该商品
        if (!hasUserPurchased(userId, productId)) {
            throw new BusinessException("您尚未购买该商品，无法下载");
        }
        
        // 获取IP地址
        String ipAddress = getClientIp(request);
        
        // 创建下载记录
        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setProductId(productId);
        record.setDownloadTime(LocalDateTime.now());
        record.setIpAddress(ipAddress);
        
        downloadRecordMapper.insert(record);
        log.info("下载记录创建成功");
    }
    
    /**
     * 获取用户下载记录（分页）
     */
    public Page<DownloadRecordResponseDTO> getDownloadRecords(Long userId, Long current, Long size, String productName) {
        log.info("查询用户下载记录，用户ID: {}, 页码: {}, 每页数量: {}", userId, current, size);
        
        Page<DownloadRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<DownloadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DownloadRecord::getUserId, userId)
               .orderByDesc(DownloadRecord::getDownloadTime);
        
        Page<DownloadRecord> recordPage = downloadRecordMapper.selectPage(page, wrapper);
        
        // 转换为ResponseDTO
        Page<DownloadRecordResponseDTO> resultPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<DownloadRecordResponseDTO> dtoList = new ArrayList<>();
        
        for (DownloadRecord record : recordPage.getRecords()) {
            DownloadRecordResponseDTO dto = new DownloadRecordResponseDTO();
            dto.setId(record.getId());
            dto.setUserId(record.getUserId());
            dto.setProductId(record.getProductId());
            dto.setDownloadTime(record.getDownloadTime());
            dto.setIpAddress(record.getIpAddress());
            
            // 填充用户名
            User user = userMapper.selectById(record.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
            }
            
            // 填充商品信息
            Product product = productMapper.selectById(record.getProductId());
            if (product != null) {
                dto.setProductName(product.getProductName());
                
                // 填充封面图 - 使用新的文件路径字段
                if (StrUtil.isNotBlank(product.getCoverImageUrl())) {
                    dto.setProductCoverUrl(product.getCoverImageUrl());
                }
            }
            
            dtoList.add(dto);
        }
        
        // 如果有商品名称筛选，进行过滤
        if (StrUtil.isNotBlank(productName)) {
            dtoList = dtoList.stream()
                    .filter(dto -> dto.getProductName() != null && dto.getProductName().contains(productName))
                    .collect(Collectors.toList());
        }
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }
    
    /**
     * 获取商品的下载链接（需要验证购买权限）
     */
    public String getDownloadLink(Long userId, String productId) {
        log.info("获取商品下载链接，用户ID: {}, 商品ID: {}", userId, productId);
        
        // 检查用户是否已购买该商品
        if (!hasUserPurchased(userId, productId)) {
            throw new BusinessException("您尚未购买该商品，无法获取下载链接");
        }
        
        // 获取商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        if (StrUtil.isBlank(product.getDownloadLink())) {
            throw new BusinessException("该商品暂无下载链接");
        }
        
        return product.getDownloadLink();
    }
    
    /**
     * 检查用户是否已购买商品
     */
    private boolean hasUserPurchased(Long userId, String productId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .eq(Orders::getProductId, productId)
               .in(Orders::getOrderStatus, 2, 3); // 已支付或已完成
        
        Long count = ordersMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
