package org.example.springboot.service.convert;

import org.example.springboot.dto.TagCreateDTO;
import org.example.springboot.dto.TagResponseDTO;
import org.example.springboot.dto.TagUpdateDTO;
import org.example.springboot.entity.Tag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签转换工具类
 * 用于Entity和DTO之间的转换
 */
@Component
public class TagConvert {

    /**
     * TagCreateDTO转换为Tag实体
     */
    public Tag createDtoToEntity(TagCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }
        
        Tag tag = new Tag();
        tag.setName(createDTO.getName());
        tag.setColor(createDTO.getColor());
        tag.setTextColor(createDTO.getTextColor());
        
        return tag;
    }

    /**
     * TagUpdateDTO转换为Tag实体
     */
    public Tag updateDtoToEntity(TagUpdateDTO updateDTO) {
        if (updateDTO == null) {
            return null;
        }
        
        Tag tag = new Tag();
        tag.setId(updateDTO.getId());
        tag.setName(updateDTO.getName());
        tag.setColor(updateDTO.getColor());
        tag.setTextColor(updateDTO.getTextColor());
        
        return tag;
    }

    /**
     * Tag实体转换为TagResponseDTO
     */
    public TagResponseDTO entityToResponseDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        TagResponseDTO responseDTO = new TagResponseDTO();
        responseDTO.setId(tag.getId());
        responseDTO.setName(tag.getName());
        responseDTO.setColor(tag.getColor());
        responseDTO.setTextColor(tag.getTextColor());
        responseDTO.setArticleCount(tag.getArticleCount());
        responseDTO.setCreateTime(tag.getCreateTime());
        responseDTO.setUpdateTime(tag.getUpdateTime());
        
        return responseDTO;
    }

    /**
     * Tag实体列表转换为TagResponseDTO列表
     */
    public List<TagResponseDTO> entityListToResponseDtoList(List<Tag> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<TagResponseDTO> responseDTOList = new ArrayList<>();
        for (Tag tag : tagList) {
            TagResponseDTO responseDTO = entityToResponseDto(tag);
            if (responseDTO != null) {
                responseDTOList.add(responseDTO);
            }
        }
        
        return responseDTOList;
    }

    /**
     * 使用UpdateDTO的数据更新现有Tag实体
     */
    public void updateEntityFromDto(Tag existingTag, TagUpdateDTO updateDTO) {
        if (existingTag == null || updateDTO == null) {
            return;
        }
        
        if (updateDTO.getName() != null) {
            existingTag.setName(updateDTO.getName());
        }
        if (updateDTO.getColor() != null) {
            existingTag.setColor(updateDTO.getColor());
        }
        if (updateDTO.getTextColor() != null) {
            existingTag.setTextColor(updateDTO.getTextColor());
        }
    }
}

