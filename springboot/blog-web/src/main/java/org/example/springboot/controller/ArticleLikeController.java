package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.UserLikeService;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.dto.LikeStatusResponseDTO;
import org.example.springboot.dto.UserLikedArticleResponseDTO;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Tag(name="文章点赞接口")
@RestController
@RequestMapping("/article")
public class ArticleLikeController {
    
    @Resource
    private UserLikeService userLikeService;
    
    @Operation(summary = "点赞或取消点赞文章")
    @PostMapping("/{id}/like")
    public Result<LikeStatusResponseDTO> toggleLike(@PathVariable("id") String articleId) {
        boolean isLiked = userLikeService.toggleLike(articleId);
        LikeStatusResponseDTO response = new LikeStatusResponseDTO(isLiked);
        return Result.success(response);
    }
    
    @Operation(summary = "检查用户是否点赞了文章")
    @GetMapping("/{id}/like/status")
    public Result<LikeStatusResponseDTO> checkLikeStatus(@PathVariable("id") String articleId) {
        boolean isLiked = userLikeService.checkUserLiked(articleId);
        LikeStatusResponseDTO response = new LikeStatusResponseDTO(isLiked);
        return Result.success(response);
    }
    
    @Operation(summary = "获取用户点赞的文章ID列表")
    @GetMapping("/user/likes")
    public Result<String[]> getUserLikedArticles() {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String[] articleIds = userLikeService.getUserLikedArticleIds(userId);
        return Result.success(articleIds);
    }
    
    @Operation(summary = "获取用户点赞的文章分页列表")
    @GetMapping("/user/likes/page")
    public Result<Page<UserLikedArticleResponseDTO>> getUserLikedArticlePage(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userLikeService.getUserLikedArticlePage(currentPage, size));
    }
} 