package org.example.springboot.service.convert;

import org.example.springboot.dto.CategoryCreateDTO;
import org.example.springboot.dto.CategoryResponseDTO;
import org.example.springboot.dto.CategoryUpdateDTO;
import org.example.springboot.entity.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryConvert {
    
    /**
     * CategoryCreateDTO 转换为 Category
     */
    public static Category toEntity(CategoryCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setMainColor(dto.getMainColor());
        category.setParentId(dto.getParentId());
        category.setOrderNum(dto.getOrderNum());
        
        return category;
    }
    
    /**
     * CategoryUpdateDTO 转换为 Category
     */
    public static Category toEntity(CategoryUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setMainColor(dto.getMainColor());
        category.setParentId(dto.getParentId());
        category.setOrderNum(dto.getOrderNum());
        
        return category;
    }
    
    /**
     * Category 转换为 CategoryResponseDTO
     */
    public static CategoryResponseDTO toResponseDTO(Category category) {
        if (category == null) {
            return null;
        }
        
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setMainColor(category.getMainColor());
        dto.setParentId(category.getParentId());
        dto.setOrderNum(category.getOrderNum());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        dto.setArticleCount(category.getArticleCount());
        
        // 转换子分类列表
        if (category.getChildren() != null) {
            List<CategoryResponseDTO> childrenDTOs = new ArrayList<>();
            for (Category child : category.getChildren()) {
                childrenDTOs.add(toResponseDTO(child));
            }
            dto.setChildren(childrenDTOs);
        }
        
        return dto;
    }
    
    /**
     * Category 列表转换为 CategoryResponseDTO 列表
     */
    public static List<CategoryResponseDTO> toResponseDTOList(List<Category> categories) {
        if (categories == null) {
            return null;
        }
        
        List<CategoryResponseDTO> dtoList = new ArrayList<>();
        for (Category category : categories) {
            dtoList.add(toResponseDTO(category));
        }
        
        return dtoList;
    }
}

