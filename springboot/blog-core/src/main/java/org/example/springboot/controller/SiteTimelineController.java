package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.dto.*;
import org.example.springboot.service.SiteTimelineService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 网站发展历程控制器
 */
@Tag(name = "网站发展历程接口")
@RestController
@RequestMapping("/timeline")
public class SiteTimelineController {
    
    @Resource
    private SiteTimelineService siteTimelineService;
    
    /**
     * 获取前台显示的发展历程
     */
    @Operation(summary = "获取前台显示的发展历程")
    @GetMapping("/visible")
    public Result<List<SiteTimelineResponseDTO>> getVisibleTimelines() {
        List<SiteTimelineResponseDTO> timelines = siteTimelineService.getVisibleTimelines();
        return Result.success(timelines);
    }
    
    /**
     * 管理员获取所有发展历程
     */
    @Operation(summary = "管理员获取所有发展历程")
    @GetMapping("/admin/all")
    public Result<List<SiteTimelineResponseDTO>> getAllTimelines() {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        List<SiteTimelineResponseDTO> timelines = siteTimelineService.getAllTimelines();
        return Result.success(timelines);
    }
    
    /**
     * 管理员分页获取发展历程
     */
    @Operation(summary = "管理员分页获取发展历程")
    @GetMapping("/admin/page")
    public Result<Page<SiteTimelineResponseDTO>> getTimelinesByPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "事件标题") @RequestParam(required = false) String title,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        Page<SiteTimelineResponseDTO> page = siteTimelineService.getTimelinesByPage(current, size, title, status);
        return Result.success(page);
    }
    
    /**
     * 根据ID获取发展历程
     */
    @Operation(summary = "根据ID获取发展历程")
    @GetMapping("/admin/{id}")
    public Result<SiteTimelineResponseDTO> getTimelineById(@PathVariable Long id) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        SiteTimelineResponseDTO timeline = siteTimelineService.getTimelineById(id);
        return Result.success(timeline);
    }
    
    /**
     * 管理员新增发展历程
     */
    @Operation(summary = "管理员新增发展历程")
    @PostMapping("/admin")
    public Result<SiteTimelineResponseDTO> addTimeline(@Validated @RequestBody SiteTimelineCreateDTO createDTO) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        SiteTimelineResponseDTO timeline = siteTimelineService.addTimeline(createDTO);
        return Result.success(timeline);
    }
    
    /**
     * 管理员更新发展历程
     */
    @Operation(summary = "管理员更新发展历程")
    @PutMapping("/admin/{id}")
    public Result<Void> updateTimeline(@PathVariable Long id, @Validated @RequestBody SiteTimelineUpdateDTO updateDTO) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        updateDTO.setId(id);
        siteTimelineService.updateTimeline(updateDTO);
        return Result.success();
    }
    
    /**
     * 管理员删除发展历程
     */
    @Operation(summary = "管理员删除发展历程")
    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteTimeline(@PathVariable Long id) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        siteTimelineService.deleteTimeline(id);
        return Result.success();
    }
    
    /**
     * 管理员更新发展历程状态
     */
    @Operation(summary = "管理员更新发展历程状态")
    @PutMapping("/admin/{id}/status")
    public Result<Void> updateTimelineStatus(@PathVariable Long id, @Validated @RequestBody SiteTimelineStatusUpdateDTO statusUpdateDTO) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        siteTimelineService.updateTimelineStatus(id, statusUpdateDTO);
        return Result.success();
    }
}
