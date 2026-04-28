package org.example.springboot.service.convert;

import org.example.springboot.dto.*;
import org.example.springboot.entity.FriendLink;

import java.time.LocalDateTime;

/**
 * 友情链接数据转换类
 */
public class FriendLinkConvert {
    
    /**
     * Entity转换为ResponseDTO
     */
    public static FriendLinkResponseDTO toResponseDTO(FriendLink friendLink) {
        if (friendLink == null) {
            return null;
        }
        
        FriendLinkResponseDTO dto = new FriendLinkResponseDTO();
        dto.setId(friendLink.getId());
        dto.setName(friendLink.getName());
        dto.setUrl(friendLink.getUrl());
        dto.setLogo(friendLink.getLogo());
        dto.setDescription(friendLink.getDescription());
        dto.setOrderNum(friendLink.getOrderNum());
        dto.setStatus(friendLink.getStatus());
        dto.setCreateTime(friendLink.getCreateTime());
        dto.setUpdateTime(friendLink.getUpdateTime());
        
        return dto;
    }
    
    /**
     * CreateDTO转换为Entity
     */
    public static FriendLink toEntity(FriendLinkCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }
        
        FriendLink friendLink = new FriendLink();
        friendLink.setName(createDTO.getName());
        friendLink.setUrl(createDTO.getUrl());
        friendLink.setLogo(createDTO.getLogo());
        friendLink.setDescription(createDTO.getDescription());
        friendLink.setOrderNum(createDTO.getOrderNum());
        friendLink.setStatus(createDTO.getStatus());
        friendLink.setCreateTime(LocalDateTime.now());
        friendLink.setUpdateTime(LocalDateTime.now());
        
        return friendLink;
    }
    
    /**
     * UpdateDTO转换为Entity
     */
    public static FriendLink toEntity(FriendLinkUpdateDTO updateDTO) {
        if (updateDTO == null) {
            return null;
        }
        
        FriendLink friendLink = new FriendLink();
        friendLink.setId(updateDTO.getId());
        friendLink.setName(updateDTO.getName());
        friendLink.setUrl(updateDTO.getUrl());
        friendLink.setLogo(updateDTO.getLogo());
        friendLink.setDescription(updateDTO.getDescription());
        friendLink.setOrderNum(updateDTO.getOrderNum());
        friendLink.setStatus(updateDTO.getStatus());
        friendLink.setUpdateTime(LocalDateTime.now());
        
        return friendLink;
    }
    
    /**
     * 批量转换Entity列表为ResponseDTO列表
     */
    public static java.util.List<FriendLinkResponseDTO> toResponseDTOList(java.util.List<FriendLink> friendLinkList) {
        if (friendLinkList == null || friendLinkList.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        return friendLinkList.stream()
                .map(FriendLinkConvert::toResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }
}


