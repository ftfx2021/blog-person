package org.example.springboot.service.convert;

import org.example.springboot.dto.*;
import org.example.springboot.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户转换工具类
 * 负责DTO与Entity之间的转换
 */
public class UserConvert {

    /**
     * UserCreateDTO转换为User实体
     */
    public static User toEntity(UserCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleCode(dto.getRoleCode());
        user.setName(dto.getName());
        user.setSex(dto.getSex());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        
        return user;
    }

    /**
     * UserUpdateDTO转换为User实体
     */
    public static User toEntity(UserUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleCode(dto.getRoleCode());
        user.setName(dto.getName());
        user.setSex(dto.getSex());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        
        return user;
    }

    /**
     * UserLoginDTO转换为User实体
     */
    public static User toEntity(UserLoginDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        
        return user;
    }

    /**
     * User实体转换为UserResponseDTO
     */
    public static UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoleCode(user.getRoleCode());
        dto.setName(user.getName());
        dto.setSex(user.getSex());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        
        return dto;
    }

    /**
     * User实体转换为UserLoginResponseDTO
     */
    public static UserLoginResponseDTO toLoginResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserLoginResponseDTO dto = new UserLoginResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoleCode(user.getRoleCode());
        dto.setName(user.getName());
        dto.setSex(user.getSex());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        dto.setToken(user.getToken());
        
        return dto;
    }

    /**
     * User实体列表转换为UserResponseDTO列表
     */
    public static List<UserResponseDTO> toResponseDTOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<UserResponseDTO> dtoList = new ArrayList<>();
        for (User user : users) {
            UserResponseDTO dto = toResponseDTO(user);
            if (dto != null) {
                dtoList.add(dto);
            }
        }
        
        return dtoList;
    }

    /**
     * 将UserUpdateDTO的非空字段更新到现有User实体
     */
    public static void updateEntity(User existingUser, UserUpdateDTO updateDTO) {
        if (existingUser == null || updateDTO == null) {
            return;
        }
        
        if (updateDTO.getUsername() != null) {
            existingUser.setUsername(updateDTO.getUsername());
        }
        if (updateDTO.getEmail() != null) {
            existingUser.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            existingUser.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getRoleCode() != null) {
            existingUser.setRoleCode(updateDTO.getRoleCode());
        }
        if (updateDTO.getName() != null) {
            existingUser.setName(updateDTO.getName());
        }
        if (updateDTO.getSex() != null) {
            existingUser.setSex(updateDTO.getSex());
        }
        if (updateDTO.getAvatar() != null) {
            existingUser.setAvatar(updateDTO.getAvatar());
        }
        if (updateDTO.getStatus() != null) {
            existingUser.setStatus(updateDTO.getStatus());
        }
        
        // 更新时间
        existingUser.setUpdateTime(LocalDateTime.now());
    }
}

