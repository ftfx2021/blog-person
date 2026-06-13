package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.dto.*;
import org.example.springboot.service.FriendLinkService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "友情链接接口")
@RestController
@RequestMapping("/friendLinks")
public class FriendLinkController {
    
    @Resource
    private FriendLinkService friendLinkService;
    
    @Operation(summary = "获取前台显示的友情链接")
    @GetMapping("/visible")
    public Result<List<FriendLinkResponseDTO>> getVisibleFriendLinks() {
        List<FriendLinkResponseDTO> friendLinks = friendLinkService.getVisibleFriendLinks();
        return Result.success(friendLinks);
    }
    
    @Operation(summary = "管理员获取所有友情链接")
    @GetMapping("/admin/all")
    public Result<List<FriendLinkResponseDTO>> getAllFriendLinks() {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }

        List<FriendLinkResponseDTO> friendLinks = friendLinkService.getAllFriendLinks();
        return Result.success(friendLinks);
    }
    
    @Operation(summary = "管理员分页获取友情链接")
    @GetMapping("/admin/page")
    public Result<Page<FriendLinkResponseDTO>> getFriendLinksByPage(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        // 验证管理员权限
        String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        FriendLinkQueryDTO queryDTO = new FriendLinkQueryDTO();
        queryDTO.setName(name);
        queryDTO.setStatus(status);
        queryDTO.setCurrentPage(currentPage);
        queryDTO.setSize(size);
        
        Page<FriendLinkResponseDTO> page = friendLinkService.getFriendLinksByPage(queryDTO);
        return Result.success(page);
    }
    
    @Operation(summary = "根据ID获取友情链接")
    @GetMapping("/admin/{id}")
    public Result<FriendLinkResponseDTO> getFriendLinkById(@PathVariable Long id) {
        // 验证管理员权限
            String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        FriendLinkResponseDTO friendLink = friendLinkService.getFriendLinkById(id);
        return Result.success(friendLink);
    }
    
    @Operation(summary = "管理员新增友情链接")
    @PostMapping("/admin")
    public Result<FriendLinkResponseDTO> addFriendLink(@Validated @RequestBody FriendLinkCreateDTO createDTO) {
        // 验证管理员权限
            String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！"+currentUserRole);
        }
        
        FriendLinkResponseDTO friendLink = friendLinkService.addFriendLink(createDTO);
        return Result.success(friendLink);
    }
    
    @Operation(summary = "管理员更新友情链接")
    @PutMapping("/admin/{id}")
    public Result<Void> updateFriendLink(@PathVariable Long id, @Validated @RequestBody FriendLinkUpdateDTO updateDTO) {
        // 验证管理员权限
            String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        updateDTO.setId(id);
        friendLinkService.updateFriendLink(updateDTO);
        return Result.success();
    }
    
    @Operation(summary = "管理员删除友情链接")
    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteFriendLink(@PathVariable Long id) {
        // 验证管理员权限
            String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        friendLinkService.deleteFriendLink(id);
        return Result.success();
    }
    
    @Operation(summary = "管理员更新友情链接状态")
    @PutMapping("/admin/{id}/status")
    public Result<Void> updateFriendLinkStatus(@PathVariable Long id, @Validated @RequestBody FriendLinkStatusUpdateDTO statusUpdateDTO) {
        // 验证管理员权限
            String currentUserRole = JwtTokenUtils.getCurrentUserRole();
        if (!"ADMIN".equals(currentUserRole)) {
            return Result.error("无权操作！");
        }
        
        friendLinkService.updateFriendLinkStatus(id, statusUpdateDTO);
        return Result.success();
    }
} 