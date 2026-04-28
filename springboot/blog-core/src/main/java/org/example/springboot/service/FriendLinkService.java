package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.FriendLink;
import org.example.springboot.mapper.FriendLinkMapper;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.dto.*;
import org.example.springboot.service.convert.FriendLinkConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class FriendLinkService {
    
    @Resource
    private FriendLinkMapper friendLinkMapper;
    
    @Resource
    private FileManagementService fileManagementService;
    
    /**
     * 获取所有友情链接
     */
    public List<FriendLinkResponseDTO> getAllFriendLinks() {
        // 按排序号升序排列
        LambdaQueryWrapper<FriendLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(FriendLink::getOrderNum);
        
        List<FriendLink> friendLinks = friendLinkMapper.selectList(queryWrapper);
        return FriendLinkConvert.toResponseDTOList(friendLinks);
    }
    
    /**
     * 获取所有显示状态的友情链接（前台使用）
     */
    public List<FriendLinkResponseDTO> getVisibleFriendLinks() {
        LambdaQueryWrapper<FriendLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendLink::getStatus, 1)
                   .orderByAsc(FriendLink::getOrderNum);
        
        List<FriendLink> friendLinks = friendLinkMapper.selectList(queryWrapper);
        return FriendLinkConvert.toResponseDTOList(friendLinks);
    }
    
    /**
     * 分页获取友情链接
     */
    public Page<FriendLinkResponseDTO> getFriendLinksByPage(FriendLinkQueryDTO queryDTO) {
        Page<FriendLink> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());
        LambdaQueryWrapper<FriendLink> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(queryDTO.getName())) {
            queryWrapper.like(FriendLink::getName, queryDTO.getName());
        }
        
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(FriendLink::getStatus, queryDTO.getStatus());
        }
        
        // 按排序号升序排列
        queryWrapper.orderByAsc(FriendLink::getOrderNum);
        
        Page<FriendLink> entityPage = friendLinkMapper.selectPage(page, queryWrapper);
        
        // 转换为DTO分页对象
        Page<FriendLinkResponseDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(entityPage.getCurrent());
        dtoPage.setSize(entityPage.getSize());
        dtoPage.setTotal(entityPage.getTotal());
        dtoPage.setRecords(FriendLinkConvert.toResponseDTOList(entityPage.getRecords()));
        
        return dtoPage;
    }
    
    /**
     * 根据ID获取友情链接
     */
    public FriendLinkResponseDTO getFriendLinkById(Long id) {
        if (id == null) {
            throw new ServiceException("友链ID不能为空");
        }
        
        FriendLink friendLink = friendLinkMapper.selectById(id);
        if (friendLink == null) {
            throw new ServiceException("友链不存在");
        }
        
        return FriendLinkConvert.toResponseDTO(friendLink);
    }
    
    /**
     * 新增友情链接
     */
    @Transactional(rollbackFor = Exception.class)
    public FriendLinkResponseDTO addFriendLink(FriendLinkCreateDTO createDTO) {
        if (createDTO == null) {
            throw new ServiceException("友链信息不能为空");
        }
        
        FriendLink friendLink = FriendLinkConvert.toEntity(createDTO);
        
        int result = friendLinkMapper.insert(friendLink);
        if (result <= 0) {
            throw new ServiceException("新增友链失败");
        }
        
        // 返回创建的友情链接数据
        return FriendLinkConvert.toResponseDTO(friendLink);
    }
    
    /**
     * 更新友情链接
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendLink(FriendLinkUpdateDTO updateDTO) {
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new ServiceException("友链信息不完整");
        }
        
        FriendLink existFriendLink = friendLinkMapper.selectById(updateDTO.getId());
        if (existFriendLink == null) {
            throw new ServiceException("友链不存在");
        }
        
        // 处理Logo更新 - 如果是新上传的临时文件，删除旧Logo
        if (updateDTO.getLogo() != null && updateDTO.getLogo().contains("/temp/")) {
            try {
                // 删除旧Logo
                if (existFriendLink.getLogo() != null && !existFriendLink.getLogo().isEmpty()) {
                    fileManagementService.deleteFileByUrl(existFriendLink.getLogo());
                }
            } catch (Exception e) {
                // 删除失败不影响更新
            }
        }
        
        FriendLink friendLink = FriendLinkConvert.toEntity(updateDTO);
        
        int result = friendLinkMapper.updateById(friendLink);
        if (result <= 0) {
            throw new ServiceException("更新友链失败");
        }
    }
    
    /**
     * 删除友情链接
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriendLink(Long id) {
        if (id == null) {
            throw new ServiceException("友链ID不能为空");
        }
        
        FriendLink existFriendLink = friendLinkMapper.selectById(id);
        if (existFriendLink == null) {
            throw new ServiceException("友链不存在");
        }
        
        int result = friendLinkMapper.deleteById(id);
        if (result <= 0) {
            throw new ServiceException("删除友链失败");
        }
    }
    
    /**
     * 更新友情链接状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendLinkStatus(Long id, FriendLinkStatusUpdateDTO statusUpdateDTO) {
        if (id == null) {
            throw new ServiceException("友链ID不能为空");
        }
        
        if (statusUpdateDTO == null || statusUpdateDTO.getStatus() == null) {
            throw new ServiceException("状态不能为空");
        }
        
        FriendLink friendLink = friendLinkMapper.selectById(id);
        if (friendLink == null) {
            throw new ServiceException("友链不存在");
        }
        
        friendLink.setStatus(statusUpdateDTO.getStatus());
        friendLink.setUpdateTime(java.time.LocalDateTime.now());
        
        int result = friendLinkMapper.updateById(friendLink);
        if (result <= 0) {
            throw new ServiceException("更新友链状态失败");
        }
    }
} 