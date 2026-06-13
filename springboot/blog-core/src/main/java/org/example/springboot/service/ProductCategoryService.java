package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.ProductCategoryCreateDTO;
import org.example.springboot.dto.ProductCategoryResponseDTO;
import org.example.springboot.dto.ProductCategoryUpdateDTO;
import org.example.springboot.entity.ProductCategory;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ProductCategoryMapper;
import org.example.springboot.service.convert.ProductCategoryConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductCategoryService {
    
    @Resource
    private ProductCategoryMapper productCategoryMapper;
    
    /**
     * 获取所有商品分类树（按排序号排序）
     */
    public List<ProductCategoryResponseDTO> getAllCategories() {
        log.info("查询所有商品分类树");
        List<ProductCategory> categories = productCategoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder)
        );
        List<ProductCategoryResponseDTO> dtoList = ProductCategoryConvert.toResponseDTOList(categories);
        return buildCategoryTree(dtoList);
    }
    
    /**
     * 获取启用状态的商品分类树
     */
    public List<ProductCategoryResponseDTO> getEnabledCategories() {
        log.info("查询启用状态的商品分类树");
        List<ProductCategory> categories = productCategoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSortOrder)
        );
        List<ProductCategoryResponseDTO> dtoList = ProductCategoryConvert.toResponseDTOList(categories);
        return buildCategoryTree(dtoList);
    }
    
    /**
     * 构建分类树形结构（最多两级）
     */
    private List<ProductCategoryResponseDTO> buildCategoryTree(List<ProductCategoryResponseDTO> allCategories) {
        // 找出所有一级分类
        List<ProductCategoryResponseDTO> rootCategories = allCategories.stream()
            .filter(category -> category.getParentId() == null || category.getParentId() == 0)
            .collect(Collectors.toList());
        
        // 为每个一级分类添加子分类
        for (ProductCategoryResponseDTO root : rootCategories) {
            List<ProductCategoryResponseDTO> children = allCategories.stream()
                .filter(category -> category.getParentId() != null && category.getParentId().equals(root.getId()))
                .collect(Collectors.toList());
            root.setChildren(children.isEmpty() ? null : children);
        }
        
        return rootCategories;
    }
    
    /**
     * 根据ID获取商品分类
     */
    public ProductCategoryResponseDTO getCategoryById(Long id) {
        log.info("查询商品分类，ID: {}", id);
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        return ProductCategoryConvert.toResponseDTO(category);
    }
    
    /**
     * 创建商品分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(ProductCategoryCreateDTO dto) {
        log.info("创建商品分类: {}", dto.getCategoryName());
        
        // 检查分类名称是否已存在
        checkCategoryNameExists(dto.getCategoryName(), null);
        
        // 如果有父分类，验证父分类存在且为一级分类
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            ProductCategory parentCategory = productCategoryMapper.selectById(dto.getParentId());
            if (parentCategory == null) {
                throw new BusinessException("父分类不存在");
            }
            if (parentCategory.getLevel() != 1) {
                throw new BusinessException("只能在一级分类下创建二级分类");
            }
        }
        
        ProductCategory category = ProductCategoryConvert.toEntity(dto);
        productCategoryMapper.insert(category);
        log.info("商品分类创建成功，ID: {}", category.getId());
    }
    
    /**
     * 更新商品分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(ProductCategoryUpdateDTO dto) {
        log.info("更新商品分类，ID: {}", dto.getId());
        
        // 检查分类是否存在
        ProductCategory existingCategory = productCategoryMapper.selectById(dto.getId());
        if (existingCategory == null) {
            throw new BusinessException("商品分类不存在");
        }
        
        // 检查分类名称是否已被其他分类使用
        checkCategoryNameExists(dto.getCategoryName(), dto.getId());
        
        // 如果修改了父分类，验证父分类存在且为一级分类
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            ProductCategory parentCategory = productCategoryMapper.selectById(dto.getParentId());
            if (parentCategory == null) {
                throw new BusinessException("父分类不存在");
            }
            if (parentCategory.getLevel() != 1) {
                throw new BusinessException("只能在一级分类下创建二级分类");
            }
        }
        
        ProductCategory category = ProductCategoryConvert.toEntity(dto);
        productCategoryMapper.updateById(category);
        log.info("商品分类更新成功");
    }
    
    /**
     * 删除商品分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        log.info("删除商品分类，ID: {}", id);
        
        // 检查分类是否存在
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        
        // 如果是一级分类，检查是否有子分类
        if (category.getLevel() == 1) {
            long childCount = productCategoryMapper.selectCount(
                new LambdaQueryWrapper<ProductCategory>()
                    .eq(ProductCategory::getParentId, id)
            );
            if (childCount > 0) {
                throw new BusinessException("该分类下存在子分类，无法删除");
            }
        }
        
        // TODO: 检查是否有商品使用该分类（功能点2实现后添加）
        
        productCategoryMapper.deleteById(id);
        log.info("商品分类删除成功");
    }
    
    /**
     * 检查分类名称是否已存在
     */
    private void checkCategoryNameExists(String categoryName, Long excludeId) {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getCategoryName, categoryName);
        if (excludeId != null) {
            wrapper.ne(ProductCategory::getId, excludeId);
        }
        Long count = productCategoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }
    }
}
