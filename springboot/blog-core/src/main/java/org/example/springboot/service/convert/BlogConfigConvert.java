package org.example.springboot.service.convert;

import org.example.springboot.dto.BlogConfigResponseDTO;
import org.example.springboot.entity.BlogConfig;

/**
 * BlogConfig数据转换器
 */
public class BlogConfigConvert {
    
    /**
     * Entity转换为响应DTO
     */
    public static BlogConfigResponseDTO entityToResponseDTO(BlogConfig entity) {
        if (entity == null) {
            return null;
        }
        
        BlogConfigResponseDTO dto = new BlogConfigResponseDTO();
        dto.setId(entity.getId());
        dto.setConfigKey(entity.getConfigKey());
        dto.setConfigValue(entity.getConfigValue());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
}
