package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.dto.CommentCreateDTO;
import org.example.springboot.dto.CommentQueryDTO;
import org.example.springboot.dto.CommentResponseDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.User;
import org.example.springboot.enums.CommentStatus;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.convert.CommentConvert;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private ArticleMapper articleMapper;
    
    
    /**
     * 添加评论
     * @param commentCreateDTO 评论创建DTO
     * @return 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentCreateDTO commentCreateDTO) {
        // 获取当前登录用户
        Long currentUserId = JwtTokenUtils.getCurrentUserId();
        
        // 检查文章是否存在
        Article article = articleMapper.selectById(commentCreateDTO.getArticleId());
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        // 转换DTO为Entity
        Comment comment = CommentConvert.toEntity(commentCreateDTO);
        comment.setUserId(currentUserId);
        
        // 设置初始状态为待审核
        comment.setStatus(CommentStatus.PENDING.getValue());
        
        // 如果是回复评论，检查父评论是否存在
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment == null) {
                throw new BusinessException("父评论不存在");
            }
        } else {
            // 顶级评论，父评论ID为0
            comment.setParentId(0L);
        }
        
        // 如果没有指定回复用户，默认为0
        if (comment.getToUserId() == null) {
            comment.setToUserId(0L);
        }
        
        // 插入评论
        commentMapper.insert(comment);
        
        // 更新文章评论数
        article.setCommentCount(article.getCommentCount() + 1);
        articleMapper.updateById(article);
        
        return comment.getId();
    }
    
    /**
     * 获取文章评论列表（分页）
     * @param articleId 文章ID
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 评论分页数据
     */
    public Page<CommentResponseDTO> getCommentsByArticle(String articleId, Integer currentPage, Integer size) {
        // 分页查询顶级评论
        Page<Comment> commentPage = new Page<>(currentPage, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId)
                    .eq(Comment::getParentId, 0) // 只查顶级评论
                    .eq(Comment::getStatus, CommentStatus.APPROVED.getValue()) // 只显示已通过的评论
                    .orderByDesc(Comment::getCreateTime);
        
        commentMapper.selectPage(commentPage, queryWrapper);
        List<Comment> comments = commentPage.getRecords();
        
        if (comments.isEmpty()) {
            return new Page<CommentResponseDTO>().setCurrent(currentPage)
                                                .setSize(size)
                                                .setTotal(0)
                                                .setRecords(new ArrayList<>());
        }
        
        // 获取评论的用户ID
        List<Long> userIds = comments.stream()
                                    .map(Comment::getUserId)
                                    .distinct()
                                    .collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds)
                        .select(User::getId, User::getUsername, User::getName, User::getAvatar);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                                     .collect(Collectors.toMap(User::getId, user -> user));
        
        // 查询每个顶级评论的回复
        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponseDTO commentDTO = CommentConvert.toResponseDTO(comment);
            
            // 设置评论用户信息
            User commentUser = userMap.get(comment.getUserId());
            if (commentUser != null) {
                commentDTO.setUsername(commentUser.getUsername());
                commentDTO.setUserName(commentUser.getName());
                commentDTO.setUserAvatar(commentUser.getAvatar());
            }
            
            // 查询回复
            List<CommentResponseDTO> replies = getCommentReplies(comment.getId());
            commentDTO.setReplies(replies);
            
            result.add(commentDTO);
        }
        
        // 创建返回分页对象
        Page<CommentResponseDTO> resultPage = new Page<>();
        resultPage.setRecords(result);
        resultPage.setCurrent(commentPage.getCurrent());
        resultPage.setSize(commentPage.getSize());
        resultPage.setTotal(commentPage.getTotal());
        
        return resultPage;
    }
    
    /**
     * 获取评论的回复列表
     * @param parentId 父评论ID
     * @return 回复列表
     */
    private List<CommentResponseDTO> getCommentReplies(Long parentId) {
        // 查询回复
        LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(Comment::getParentId, parentId)
                   .eq(Comment::getStatus, CommentStatus.APPROVED.getValue()) // 只显示已通过的评论
                   .orderByAsc(Comment::getCreateTime);
        List<Comment> replies = commentMapper.selectList(replyWrapper);
        
        if (replies.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取回复涉及的用户ID（评论者和被回复者）
        List<Long> userIds = new ArrayList<>();
        for (Comment reply : replies) {
            userIds.add(reply.getUserId());
            if (reply.getToUserId() > 0) {
                userIds.add(reply.getToUserId());
            }
        }
        userIds = userIds.stream().distinct().collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds)
                        .select(User::getId, User::getUsername, User::getName, User::getAvatar);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                                     .collect(Collectors.toMap(User::getId, user -> user));
        
        // 组装回复结果
        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment reply : replies) {
            CommentResponseDTO replyDTO = CommentConvert.toResponseDTO(reply);
            
            // 设置评论用户信息
            User replyUser = userMap.get(reply.getUserId());
            if (replyUser != null) {
                replyDTO.setUsername(replyUser.getUsername());
                replyDTO.setUserName(replyUser.getName());
                replyDTO.setUserAvatar(replyUser.getAvatar());
            }
            
            // 设置被回复用户信息
            if (reply.getToUserId() > 0) {
                User toUser = userMap.get(reply.getToUserId());
                if (toUser != null) {
                    replyDTO.setToUserId(toUser.getId());
                    replyDTO.setToUsername(toUser.getUsername());
                    replyDTO.setToUserName(toUser.getName());
                }
            }
            
            result.add(replyDTO);
        }
        
        return result;
    }
    
    /**
     * 获取用户评论列表（分页）
     * @param userId 用户ID
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 评论分页数据
     */
    public Page<CommentResponseDTO> getUserComments(Long userId, Integer currentPage, Integer size) {
        Page<Comment> commentPage = new Page<>(currentPage, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getUserId, userId)
                    .orderByDesc(Comment::getCreateTime);
        
        commentMapper.selectPage(commentPage, queryWrapper);
        List<Comment> comments = commentPage.getRecords();
        
        if (comments.isEmpty()) {
            return new Page<CommentResponseDTO>().setCurrent(currentPage)
                                                .setSize(size)
                                                .setTotal(0)
                                                .setRecords(new ArrayList<>());
        }
        
        // 获取评论涉及的文章ID
        List<String> articleIds = comments.stream()
                                      .map(Comment::getArticleId)
                                      .distinct()
                                      .collect(Collectors.toList());
        
        // 查询文章信息
        LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
        articleQueryWrapper.in(Article::getId, articleIds)
                          .select(Article::getId, Article::getTitle);
        List<Article> articles = articleMapper.selectList(articleQueryWrapper);
        Map<String, String> articleTitleMap = articles.stream()
                                                 .collect(Collectors.toMap(Article::getId, Article::getTitle));
        
        // 组装结果
        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponseDTO commentDTO = CommentConvert.toResponseDTO(comment);
            commentDTO.setArticleTitle(articleTitleMap.getOrDefault(comment.getArticleId(), "未知文章"));
            
            result.add(commentDTO);
        }
        
        // 创建返回分页对象
        Page<CommentResponseDTO> resultPage = new Page<>();
        resultPage.setRecords(result);
        resultPage.setCurrent(commentPage.getCurrent());
        resultPage.setSize(commentPage.getSize());
        resultPage.setTotal(commentPage.getTotal());
        
        return resultPage;
    }
    
    /**
     * 删除评论
     * @param commentId 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 判断是否有权限删除（管理员或评论作者）
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!currentUser.getRoleCode().equals("ADMIN") && !comment.getUserId().equals(currentUser.getId())) {
            throw new BusinessException("没有权限删除该评论");
        }
        
        // 删除评论及其关联数据
        deleteCommentAndRelated(commentId);
        
        // 更新文章评论数
        updateArticleCommentCount(comment.getArticleId(), -1);
    }
    
    /**
     * 删除评论及其相关数据
     * @param commentId 评论ID
     */
    private void deleteCommentAndRelated(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        
        // 如果是顶级评论，先删除其所有回复
        if (comment.getParentId() == 0) {
            LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(Comment::getParentId, commentId);
            commentMapper.delete(replyWrapper);
        }
        
        // 删除评论本身
        commentMapper.deleteById(commentId);
    }
    
    /**
     * 更新文章评论数
     * @param articleId 文章ID
     * @param increment 增量（可为负数）
     */
    private void updateArticleCommentCount(String articleId, int increment) {
        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            int newCount = Math.max(0, article.getCommentCount() + increment);
            article.setCommentCount(newCount);
            articleMapper.updateById(article);
        }
    }
    
    /**
     * 审核评论
     * @param commentId 评论ID
     * @param status 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditComment(Long commentId, Integer status) {
        // 检查权限，只有管理员可以审核
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!currentUser.getRoleCode().equals("ADMIN")) {
            throw new BusinessException("没有权限审核评论");
        }
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 验证状态值的有效性
        CommentStatus newStatus = CommentStatus.fromValue(status);
        CommentStatus currentStatus = CommentStatus.fromValue(comment.getStatus());
        
        if (!CommentStatus.isValidTransition(currentStatus, newStatus)) {
            throw new BusinessException("无效的状态变更");
        }
        
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }
    
    /**
     * 管理员获取评论分页列表
     * @param queryDTO 查询条件DTO
     * @return 评论分页数据
     */
    public Page<CommentResponseDTO> getAdminCommentPage(CommentQueryDTO queryDTO) {
        // 先查询符合条件的文章ID
        List<String> articleIds = null;
        if (queryDTO.getArticleTitle() != null && !queryDTO.getArticleTitle().trim().isEmpty()) {
            LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
            articleQueryWrapper.like(Article::getTitle, queryDTO.getArticleTitle().trim())
                              .select(Article::getId);
            List<Article> articles = articleMapper.selectList(articleQueryWrapper);
            if (articles.isEmpty()) {
                // 没有符合条件的文章，直接返回空结果
                return new Page<CommentResponseDTO>().setCurrent(queryDTO.getCurrentPage())
                                                    .setSize(queryDTO.getSize())
                                                    .setTotal(0)
                                                    .setRecords(new ArrayList<>());
            }
            articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        }
        
        // 查询评论
        Page<Comment> commentPage = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加筛选条件
        if (articleIds != null) {
            queryWrapper.in(Comment::getArticleId, articleIds);
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Comment::getStatus, queryDTO.getStatus());
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(Comment::getCreateTime);
        
        commentMapper.selectPage(commentPage, queryWrapper);
        List<Comment> comments = commentPage.getRecords();
        
        if (comments.isEmpty()) {
            return new Page<CommentResponseDTO>().setCurrent(queryDTO.getCurrentPage())
                                                .setSize(queryDTO.getSize())
                                                .setTotal(0)
                                                .setRecords(new ArrayList<>());
        }
        
        // 获取评论的用户ID和文章ID
        List<Long> userIds = comments.stream()
                                    .map(Comment::getUserId)
                                    .distinct()
                                    .collect(Collectors.toList());
        
        List<String> allArticleIds = comments.stream()
                                         .map(Comment::getArticleId)
                                         .distinct()
                                         .collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds)
                        .select(User::getId, User::getUsername, User::getName, User::getAvatar);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                                     .collect(Collectors.toMap(User::getId, user -> user));
        
        // 查询文章信息
        LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
        articleQueryWrapper.in(Article::getId, allArticleIds)
                          .select(Article::getId, Article::getTitle);
        List<Article> articles = articleMapper.selectList(articleQueryWrapper);
        Map<String, String> articleTitleMap = articles.stream()
                                                 .collect(Collectors.toMap(Article::getId, Article::getTitle));
        
        // 组装结果
        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponseDTO commentDTO = CommentConvert.toResponseDTO(comment);
            commentDTO.setArticleTitle(articleTitleMap.getOrDefault(comment.getArticleId(), "未知文章"));
            
            // 设置评论用户信息
            User commentUser = userMap.get(comment.getUserId());
            if (commentUser != null) {
                commentDTO.setUsername(commentUser.getUsername());
                commentDTO.setUserName(commentUser.getName());
                commentDTO.setUserAvatar(commentUser.getAvatar());
            }
            
            // 如果是回复评论，获取父评论信息
            if (comment.getParentId() > 0) {
                Comment parentComment = commentMapper.selectById(comment.getParentId());
                if (parentComment != null) {
                    commentDTO.setParentContent(parentComment.getContent());
                }
                
                // 如果有回复对象，获取回复对象信息
                if (comment.getToUserId() > 0) {
                    User toUser = userMap.get(comment.getToUserId());
                    if (toUser != null) {
                        commentDTO.setToUserId(toUser.getId());
                        commentDTO.setToUserName(toUser.getName() != null ? toUser.getName() : toUser.getUsername());
                    }
                }
            }
            
            result.add(commentDTO);
        }
        
        // 创建返回分页对象
        Page<CommentResponseDTO> resultPage = new Page<>();
        resultPage.setRecords(result);
        resultPage.setCurrent(commentPage.getCurrent());
        resultPage.setSize(commentPage.getSize());
        resultPage.setTotal(commentPage.getTotal());
        
        return resultPage;
    }
    
    /**
     * 获取最近评论
     * @param size 获取数量
     * @return 评论列表
     */
    public List<CommentResponseDTO> getRecentComments(Integer size) {
        // 使用MyBatis-Plus查询最近的已通过评论
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getStatus, CommentStatus.APPROVED.getValue())
                   .orderByDesc(Comment::getCreateTime)
                   .last("LIMIT " + size);
        
        List<Comment> comments = commentMapper.selectList(queryWrapper);
        
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取涉及的用户ID和文章ID
        List<Long> userIds = comments.stream()
                                   .map(Comment::getUserId)
                                   .distinct()
                                   .collect(Collectors.toList());
        
        List<String> articleIds = comments.stream()
                                        .map(Comment::getArticleId)
                                        .distinct()
                                        .collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds)
                        .select(User::getId, User::getUsername, User::getName, User::getAvatar);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                                     .collect(Collectors.toMap(User::getId, user -> user));
        
        // 查询文章信息
        LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
        articleQueryWrapper.in(Article::getId, articleIds)
                          .select(Article::getId, Article::getTitle);
        List<Article> articles = articleMapper.selectList(articleQueryWrapper);
        Map<String, String> articleTitleMap = articles.stream()
                                                 .collect(Collectors.toMap(Article::getId, Article::getTitle));
        
        // 组装结果
        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponseDTO commentDTO = CommentConvert.toResponseDTO(comment);
            commentDTO.setArticleTitle(articleTitleMap.getOrDefault(comment.getArticleId(), "未知文章"));
            
            // 设置用户信息
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                commentDTO.setUsername(user.getUsername());
                commentDTO.setUserName(user.getName());
                commentDTO.setUserAvatar(user.getAvatar());
            }
            
            result.add(commentDTO);
        }
        
        return result;
    }
} 