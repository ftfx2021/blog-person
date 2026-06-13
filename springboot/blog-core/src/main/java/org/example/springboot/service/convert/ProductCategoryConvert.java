package org.example.springboot.service.convert;

import org.example.springboot.dto.ProductCategoryCreateDTO;
import org.example.springboot.dto.ProductCategoryResponseDTO;
import org.example.springboot.dto.ProductCategoryUpdateDTO;
import org.example.springboot.entity.ProductCategory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类转换工具类
 */
public class ProductCategoryConvert {
    
    /**
     * CreateDTO转Entity
     */
    public static ProductCategory toEntity(ProductCategoryCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductCategory entity = new ProductCategory();
        entity.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        entity.setCategoryName(dto.getCategoryName());
        entity.setLevel(dto.getParentId() != null && dto.getParentId() > 0 ? 2 : 1);
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return entity;
    }
    
    /**
     * UpdateDTO转Entity
     */
    public static ProductCategory toEntity(ProductCategoryUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductCategory entity = new ProductCategory();
        entity.setId(dto.getId());
        entity.setParentId(dto.getParentId());
        entity.setCategoryName(dto.getCategoryName());
        entity.setLevel(dto.getParentId() != null && dto.getParentId() > 0 ? 2 : 1);
        entity.setSortOrder(dto.getSortOrder());
        entity.setStatus(dto.getStatus());
        return entity;
    }
    
    /**
     * Entity转ResponseDTO
     */
    public static ProductCategoryResponseDTO toResponseDTO(ProductCategory entity) {
        if (entity == null) {
            return null;
        }
        ProductCategoryResponseDTO dto = new ProductCategoryResponseDTO();
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setLevel(entity.getLevel());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }
    
    /**
     * Entity列表转ResponseDTO列表
     */
    public static List<ProductCategoryResponseDTO> toResponseDTOList(List<ProductCategory> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ProductCategoryConvert::toResponseDTO)
                .collect(Collectors.toList());
    }
}
