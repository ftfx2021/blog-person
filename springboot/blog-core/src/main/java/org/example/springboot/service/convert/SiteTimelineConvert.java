package org.example.springboot.service.convert;

import org.example.springboot.dto.*;
import org.example.springboot.entity.SiteTimeline;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 网站发展历程数据转换类
 */
public class SiteTimelineConvert {
    
    /**
     * Entity转换为ResponseDTO
     */
    public static SiteTimelineResponseDTO toResponseDTO(SiteTimeline entity) {
        if (entity == null) {
            return null;
        }
        
        SiteTimelineResponseDTO dto = new SiteTimelineResponseDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setEventDate(entity.getEventDate());
        dto.setIcon(entity.getIcon());
        dto.setColor(entity.getColor());
        dto.setOrderNum(entity.getOrderNum());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        
        return dto;
    }
    
    /**
     * CreateDTO转换为Entity
     */
    public static SiteTimeline toEntity(SiteTimelineCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }
        
        SiteTimeline entity = new SiteTimeline();
        entity.setTitle(createDTO.getTitle());
        entity.setContent(createDTO.getContent());
        entity.setEventDate(createDTO.getEventDate());
        entity.setIcon(createDTO.getIcon());
        entity.setColor(createDTO.getColor());
        entity.setOrderNum(createDTO.getOrderNum());
        entity.setStatus(createDTO.getStatus());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        
        return entity;
    }
    
    /**
     * UpdateDTO转换为Entity
     */
    public static SiteTimeline toEntity(SiteTimelineUpdateDTO updateDTO) {
        if (updateDTO == null) {
            return null;
        }
        
        SiteTimeline entity = new SiteTimeline();
        entity.setId(updateDTO.getId());
        entity.setTitle(updateDTO.getTitle());
        entity.setContent(updateDTO.getContent());
        entity.setEventDate(updateDTO.getEventDate());
        entity.setIcon(updateDTO.getIcon());
        entity.setColor(updateDTO.getColor());
        entity.setOrderNum(updateDTO.getOrderNum());
        entity.setStatus(updateDTO.getStatus());
        entity.setUpdateTime(LocalDateTime.now());
        
        return entity;
    }
    
    /**
     * 批量转换Entity列表为ResponseDTO列表
     */
    public static List<SiteTimelineResponseDTO> toResponseDTOList(List<SiteTimeline> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }
        
        return entityList.stream()
                .map(SiteTimelineConvert::toResponseDTO)
                .collect(Collectors.toList());
    }
}
