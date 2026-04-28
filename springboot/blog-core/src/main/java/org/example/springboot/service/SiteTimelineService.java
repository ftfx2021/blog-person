package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.dto.*;
import org.example.springboot.entity.SiteTimeline;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.SiteTimelineMapper;
import org.example.springboot.service.convert.SiteTimelineConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 网站发展历程服务类
 */
@Service
public class SiteTimelineService {
    
    @Resource
    private SiteTimelineMapper siteTimelineMapper;
    
    /**
     * 获取所有发展历程（后台管理使用）
     */
    public List<SiteTimelineResponseDTO> getAllTimelines() {
        LambdaQueryWrapper<SiteTimeline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(SiteTimeline::getOrderNum)
                   .orderByDesc(SiteTimeline::getEventDate);
        
        List<SiteTimeline> timelines = siteTimelineMapper.selectList(queryWrapper);
        return SiteTimelineConvert.toResponseDTOList(timelines);
    }
    
    /**
     * 获取所有显示状态的发展历程（前台展示使用）
     */
    public List<SiteTimelineResponseDTO> getVisibleTimelines() {
        LambdaQueryWrapper<SiteTimeline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SiteTimeline::getStatus, 1)
                   .orderByAsc(SiteTimeline::getOrderNum)
                   .orderByDesc(SiteTimeline::getEventDate);
        
        List<SiteTimeline> timelines = siteTimelineMapper.selectList(queryWrapper);
        return SiteTimelineConvert.toResponseDTOList(timelines);
    }
    
    /**
     * 分页获取发展历程
     */
    public Page<SiteTimelineResponseDTO> getTimelinesByPage(Long current, Long size, String title, Integer status) {
        Page<SiteTimeline> page = new Page<>(current, size);
        LambdaQueryWrapper<SiteTimeline> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(SiteTimeline::getTitle, title);
        }
        
        if (status != null) {
            queryWrapper.eq(SiteTimeline::getStatus, status);
        }
        
        // 按排序号升序、事件日期降序排列
        queryWrapper.orderByAsc(SiteTimeline::getOrderNum)
                   .orderByDesc(SiteTimeline::getEventDate);
        
        Page<SiteTimeline> entityPage = siteTimelineMapper.selectPage(page, queryWrapper);
        
        // 转换为DTO分页对象
        Page<SiteTimelineResponseDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(entityPage.getCurrent());
        dtoPage.setSize(entityPage.getSize());
        dtoPage.setTotal(entityPage.getTotal());
        dtoPage.setRecords(SiteTimelineConvert.toResponseDTOList(entityPage.getRecords()));
        
        return dtoPage;
    }
    
    /**
     * 根据ID获取发展历程
     */
    public SiteTimelineResponseDTO getTimelineById(Long id) {
        if (id == null) {
            throw new ServiceException("事件ID不能为空");
        }
        
        SiteTimeline timeline = siteTimelineMapper.selectById(id);
        if (timeline == null) {
            throw new ServiceException("事件不存在");
        }
        
        return SiteTimelineConvert.toResponseDTO(timeline);
    }
    
    /**
     * 新增发展历程
     */
    @Transactional(rollbackFor = Exception.class)
    public SiteTimelineResponseDTO addTimeline(SiteTimelineCreateDTO createDTO) {
        if (createDTO == null) {
            throw new ServiceException("事件信息不能为空");
        }
        
        SiteTimeline timeline = SiteTimelineConvert.toEntity(createDTO);
        
        int result = siteTimelineMapper.insert(timeline);
        if (result <= 0) {
            throw new ServiceException("新增事件失败");
        }
        
        return SiteTimelineConvert.toResponseDTO(timeline);
    }
    
    /**
     * 更新发展历程
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTimeline(SiteTimelineUpdateDTO updateDTO) {
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new ServiceException("事件信息不完整");
        }
        
        SiteTimeline existTimeline = siteTimelineMapper.selectById(updateDTO.getId());
        if (existTimeline == null) {
            throw new ServiceException("事件不存在");
        }
        
        SiteTimeline timeline = SiteTimelineConvert.toEntity(updateDTO);
        
        int result = siteTimelineMapper.updateById(timeline);
        if (result <= 0) {
            throw new ServiceException("更新事件失败");
        }
    }
    
    /**
     * 删除发展历程
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTimeline(Long id) {
        if (id == null) {
            throw new ServiceException("事件ID不能为空");
        }
        
        SiteTimeline existTimeline = siteTimelineMapper.selectById(id);
        if (existTimeline == null) {
            throw new ServiceException("事件不存在");
        }
        
        int result = siteTimelineMapper.deleteById(id);
        if (result <= 0) {
            throw new ServiceException("删除事件失败");
        }
    }
    
    /**
     * 更新发展历程状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTimelineStatus(Long id, SiteTimelineStatusUpdateDTO statusUpdateDTO) {
        if (id == null) {
            throw new ServiceException("事件ID不能为空");
        }
        
        if (statusUpdateDTO == null || statusUpdateDTO.getStatus() == null) {
            throw new ServiceException("状态不能为空");
        }
        
        SiteTimeline timeline = siteTimelineMapper.selectById(id);
        if (timeline == null) {
            throw new ServiceException("事件不存在");
        }
        
        timeline.setStatus(statusUpdateDTO.getStatus());
        timeline.setUpdateTime(LocalDateTime.now());
        
        int result = siteTimelineMapper.updateById(timeline);
        if (result <= 0) {
            throw new ServiceException("更新事件状态失败");
        }
    }
}
