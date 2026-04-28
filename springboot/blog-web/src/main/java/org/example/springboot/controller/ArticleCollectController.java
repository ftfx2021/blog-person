package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.dto.CollectStatusResponseDTO;
import org.example.springboot.dto.UserCollectedArticleResponseDTO;
import org.example.springboot.dto.UserCollectQueryDTO;
import org.example.springboot.common.Result;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.service.UserCollectService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;
import cn.hutool.core.util.StrUtil;

@Tag(name="文章收藏接口")
@RestController
@RequestMapping("/article")
public class ArticleCollectController {
    
    @Resource
    private UserCollectService userCollectService;
    
    @Operation(summary = "收藏或取消收藏文章")
    @PostMapping("/{id}/collect")
    public Result<CollectStatusResponseDTO> toggleCollect(@PathVariable("id") String articleId) {
        // 参数校验
        if (StrUtil.isBlank(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        boolean isCollected = userCollectService.toggleCollect(articleId, userId);
        return Result.success(new CollectStatusResponseDTO(isCollected));
    }
    
    @Operation(summary = "检查用户是否收藏了文章")
    @GetMapping("/{id}/collect/status")
    public Result<CollectStatusResponseDTO> checkCollectStatus(@PathVariable("id") String articleId) {
        // 参数校验
        if (StrUtil.isBlank(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        
        boolean isCollected = userCollectService.checkUserCollected(articleId, userId);
        return Result.success(new CollectStatusResponseDTO(isCollected));
    }
    
    @Operation(summary = "获取用户收藏的文章ID列表")
    @GetMapping("/user/collects")
    public Result<String[]> getUserCollectedArticles() {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        String[] articleIds = userCollectService.getUserCollectedArticleIds(userId);
        return Result.success(articleIds);
    }
    
    @Operation(summary = "获取用户收藏的文章分页列表")
    @GetMapping("/user/collects/page")
    public Result<Page<UserCollectedArticleResponseDTO>> getUserCollectedArticlePage(UserCollectQueryDTO queryDTO) {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 设置用户ID到查询DTO中
        queryDTO.setUserId(userId);
        
        Page<UserCollectedArticleResponseDTO> result = userCollectService.getUserCollectedArticlePage(queryDTO);
        return Result.success(result);
    }
} 