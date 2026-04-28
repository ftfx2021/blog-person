package org.example.springboot.service.convert;

import org.example.springboot.dto.ProductCreateDTO;
import org.example.springboot.dto.ProductResponseDTO;
import org.example.springboot.dto.ProductUpdateDTO;
import org.example.springboot.entity.Product;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品转换工具类
 */
public class ProductConvert {
    
    /**
     * CreateDTO转Entity
     */
    public static Product toEntity(ProductCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Product entity = new Product();
        entity.setId(dto.getId());
        entity.setProductName(dto.getProductName());
        entity.setProductDesc(dto.getProductDesc());
        entity.setProductDetail(dto.getProductDetail());
        entity.setCategoryId(dto.getCategoryId());
        entity.setCoverImageUrl(dto.getCoverImageUrl());
        entity.setDemoImageUrls(dto.getDemoImageUrls());
        entity.setPrice(dto.getPrice());
        entity.setDownloadLink(dto.getDownloadLink());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setViewCount(0);
        entity.setSaleCount(0);
        return entity;
    }
    
    /**
     * UpdateDTO转Entity
     */
    public static Product toEntity(ProductUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Product entity = new Product();
        entity.setId(dto.getId());
        entity.setProductName(dto.getProductName());
        entity.setProductDesc(dto.getProductDesc());
        entity.setProductDetail(dto.getProductDetail());
        entity.setCategoryId(dto.getCategoryId());
        entity.setCoverImageUrl(dto.getCoverImageUrl());
        entity.setDemoImageUrls(dto.getDemoImageUrls());
        entity.setPrice(dto.getPrice());
        entity.setDownloadLink(dto.getDownloadLink());
        entity.setStatus(dto.getStatus());
        return entity;
    }
    
    /**
     * Entity转ResponseDTO
     */
    public static ProductResponseDTO toResponseDTO(Product entity) {
        if (entity == null) {
            return null;
        }
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setProductName(entity.getProductName());
        dto.setProductDesc(entity.getProductDesc());
        dto.setProductDetail(entity.getProductDetail());
        dto.setCategoryId(entity.getCategoryId());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setPrice(entity.getPrice());
        dto.setDownloadLink(entity.getDownloadLink());
        dto.setViewCount(entity.getViewCount());
        dto.setSaleCount(entity.getSaleCount());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
    
    /**
     * Entity列表转ResponseDTO列表
     */
    public static List<ProductResponseDTO> toResponseDTOList(List<Product> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ProductConvert::toResponseDTO)
                .collect(Collectors.toList());
    }
}
