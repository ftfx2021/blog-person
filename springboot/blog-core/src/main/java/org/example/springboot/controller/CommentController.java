package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.springboot.dto.CommentAuditDTO;
import org.example.springboot.dto.CommentCreateDTO;
import org.example.springboot.dto.CommentQueryDTO;
import org.example.springboot.dto.CommentResponseDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.CommentService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="评论接口")
@RestController
@RequestMapping("/comment")
public class CommentController {
    
    @Resource
    private CommentService commentService;
    
    @Operation(summary = "添加评论")
    @PostMapping
    public Result<Long> addComment(@Valid @RequestBody CommentCreateDTO commentCreateDTO) {
        Long commentId = commentService.addComment(commentCreateDTO);
        return Result.success(commentId);
    }
    
    @Operation(summary = "获取文章评论列表")
    @GetMapping("/article/{articleId}")
    public Result<?> getCommentsByArticle(
            @PathVariable String articleId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(commentService.getCommentsByArticle(articleId, currentPage, size));
    }
    
    @Operation(summary = "获取用户评论列表")
    @GetMapping("/user")
    public Result<?> getUserComments(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        return Result.success(commentService.getUserComments(userId, currentPage, size));
    }
    
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
    
    @Operation(summary = "审核评论")
    @PutMapping("/audit/{id}")
    public Result<Void> auditComment(
            @PathVariable Long id, 
            @Valid @RequestBody CommentAuditDTO auditDTO) {
        commentService.auditComment(id, auditDTO.getStatus());
        return Result.success();
    }

    @Operation(summary = "管理员获取评论分页列表")
    @GetMapping("/admin/page")
    public Result<?> getAdminCommentPage(@Valid CommentQueryDTO queryDTO) {
        return Result.success(commentService.getAdminCommentPage(queryDTO));
    }

    @Operation(summary = "获取最近评论")
    @GetMapping("/recent")
    public Result<List<CommentResponseDTO>> getRecentComments(@RequestParam(defaultValue = "5") Integer size) {
        List<CommentResponseDTO> comments = commentService.getRecentComments(size);
        return Result.success(comments);
    }
} 