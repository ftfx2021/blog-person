package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.ProductCreateDTO;
import org.example.springboot.dto.ProductResponseDTO;
import org.example.springboot.dto.ProductUpdateDTO;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.ProductCategory;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ProductCategoryMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.service.convert.ProductConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductService {
    
    @Resource
    private ProductMapper productMapper;
    
    @Resource
    private ProductCategoryMapper productCategoryMapper;
    
    @Resource
    private FileManagementService fileManagementService;
    
    /**
     * 分页查询商品列表
     */
    public Page<ProductResponseDTO> getProductPage(Long current, Long size, String productName, 
                                                    Long categoryId, Integer status) {
        log.info("分页查询商品列表，页码: {}, 每页数量: {}, 商品名称: {}, 分类ID: {}, 状态: {}", 
                 current, size, productName, categoryId, status);
        
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(productName)) {
            wrapper.like(Product::getProductName, productName);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        
        wrapper.orderByDesc(Product::getCreateTime);
        
        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        
        // 转换为ResponseDTO并填充分类名称和封面图URL
        Page<ProductResponseDTO> resultPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductResponseDTO> dtoList = ProductConvert.toResponseDTOList(productPage.getRecords());
        
        for (ProductResponseDTO dto : dtoList) {
            enrichProductInfo(dto);
        }
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }
    
    /**
     * 根据ID获取商品详情
     */
    public ProductResponseDTO getProductById(String id) {
        log.info("查询商品详情，ID: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        ProductResponseDTO dto = ProductConvert.toResponseDTO(product);
        enrichProductInfo(dto);
        return dto;
    }
    
    /**
     * 创建商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductCreateDTO dto) {
        log.info("创建商品: {}", dto.getProductName());
        
        // 检查商品ID是否已存在
        Product existingProduct = productMapper.selectById(dto.getId());
        if (existingProduct != null) {
            throw new BusinessException("商品ID已存在");
        }
        
        // 检查分类是否存在
        checkCategoryExists(dto.getCategoryId());
        
        // 注意：新系统使用URL字段，不再需要检查文件ID
        
        Product product = ProductConvert.toEntity(dto);
        productMapper.insert(product);
        log.info("商品创建成功，ID: {}", product.getId());
    }
    
    /**
     * 更新商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateDTO dto) {
        log.info("更新商品，ID: {}", dto.getId());
        
        // 检查商品是否存在
        Product existingProduct = productMapper.selectById(dto.getId());
        if (existingProduct == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 检查分类是否存在
        checkCategoryExists(dto.getCategoryId());
        
        Product product = ProductConvert.toEntity(dto);
        
        // 处理封面图更新 - 如果是新上传的临时文件，删除旧文件
        if (product.getCoverImageUrl() != null && product.getCoverImageUrl().contains("/temp/")) {
            try {
                // 删除旧封面图
                if (existingProduct.getCoverImageUrl() != null && !existingProduct.getCoverImageUrl().isEmpty()) {
                    fileManagementService.deleteFileByUrl(existingProduct.getCoverImageUrl());
                    log.info("已删除旧商品封面: {}", existingProduct.getCoverImageUrl());
                }
            } catch (Exception e) {
                log.error("删除旧商品封面失败: {}", e.getMessage());
            }
        }
        
        // 处理演示图片更新 - 如果有新图片，删除旧图片
        if (product.getDemoImageUrls() != null && !product.getDemoImageUrls().isEmpty()) {
            try {
                // 获取旧的演示图片列表
                List<String> oldImageUrls = getProductImageUrls(dto.getId());
                if (!oldImageUrls.isEmpty()) {
                    for (String oldUrl : oldImageUrls) {
                        fileManagementService.deleteFileByUrl(oldUrl);
                        log.info("已删除旧演示图片: {}", oldUrl);
                    }
                }
            } catch (Exception e) {
                log.error("删除旧演示图片失败: {}", e.getMessage());
            }
        }
        
        productMapper.updateById(product);
        log.info("商品更新成功");
    }
    
    /**
     * 删除商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(String id) {
        log.info("删除商品，ID: {}", id);
        
        // 检查商品是否存在
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // TODO: 检查是否有关联订单（订单模块实现后添加）
        
        productMapper.deleteById(id);
        log.info("商品删除成功");
    }
    
    /**
     * 更新商品状态（上架/下架）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(String id, Integer status) {
        log.info("更新商品状态，ID: {}, 状态: {}", id, status);
        
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        product.setStatus(status);
        productMapper.updateById(product);
        log.info("商品状态更新成功");
    }
    
    /**
     * 增加商品浏览次数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(String id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setViewCount(product.getViewCount() + 1);
            productMapper.updateById(product);
        }
    }
    
    /**
    /**
     * 根据商品的demoImageUrls获取演示图片URL列表
     * 注意：新系统直接存储URL，不再使用文件ID
     */
    public List<String> getProductImageUrls(String productId) {
        log.info("查询商品演示图片URL，商品ID: {}", productId);
        
        Product product = productMapper.selectById(productId);
        if (product == null || StrUtil.isBlank(product.getDemoImageUrls())) {
            return new java.util.ArrayList<>();
        }
        
        try {
            // 解析JSON数组
            return JSON.parseArray(product.getDemoImageUrls(), String.class);
        } catch (Exception e) {
            log.error("解析商品图片URL失败: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 丰富商品信息（填充分类名称、封面图URL和演示图片列表）
     */
    private void enrichProductInfo(ProductResponseDTO dto) {
        // 填充分类名称
        if (dto.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(dto.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getCategoryName());
            }
        }
        
        // 填充封面图URL - 直接使用URL字段
        if (StrUtil.isNotBlank(dto.getCoverImageUrl())) {
            // 封面图URL已经在DTO中，无需额外处理
            log.debug("商品封面图URL: {}", dto.getCoverImageUrl());
        }
        
        // 填充演示图片列表 - 直接使用URL列表
        List<String> imageUrls = getProductImageUrls(dto.getId());
        if (!imageUrls.isEmpty()) {
            List<ProductResponseDTO.ProductImageDTO> imageDTOs = imageUrls.stream()
                .map(url -> {
                    ProductResponseDTO.ProductImageDTO imageDTO = new ProductResponseDTO.ProductImageDTO();
                    imageDTO.setFilePath(url);
                    return imageDTO;
                })
                .collect(java.util.stream.Collectors.toList());
            dto.setImages(imageDTOs);
        }
    }
    
    /**
     * 检查分类是否存在
     */
    private void checkCategoryExists(Long categoryId) {
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
    }
}
