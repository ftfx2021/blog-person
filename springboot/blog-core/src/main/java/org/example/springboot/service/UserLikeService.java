package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.User;
import org.example.springboot.entity.UserLike;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.UserLikeMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.example.springboot.dto.UserLikedArticleResponseDTO;

@Service
public class UserLikeService {
    @Resource
    private UserLikeMapper userLikeMapper;
    
    @Resource
    private ArticleMapper articleMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 点赞或取消点赞文章
     * @param articleId 文章ID
     * @return 点赞状态 true-已点赞 false-未点赞
     */
    @Transactional
    public boolean toggleLike(String articleId) {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        
        // 检查文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        // 查询用户是否已点赞
        LambdaQueryWrapper<UserLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLike::getUserId, userId)
                    .eq(UserLike::getArticleId, articleId);
        
        UserLike userLike = userLikeMapper.selectOne(queryWrapper);
        
        if (userLike == null) {
            // 未点赞，添加点赞记录
            userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setArticleId(articleId);
            userLikeMapper.insert(userLike);
            
            // 更新文章点赞数
            article.setLikeCount(article.getLikeCount() + 1);
            articleMapper.updateById(article);
            
            return true;
        } else {
            // 已点赞，取消点赞
            userLikeMapper.deleteById(userLike.getId());
            
            // 更新文章点赞数
            if (article.getLikeCount() > 0) {
                article.setLikeCount(article.getLikeCount() - 1);
                articleMapper.updateById(article);
            }
            
            return false;
        }
    }
    
    /**
     * 检查用户是否点赞了文章
     * @param articleId 文章ID
     * @return 是否点赞
     */
    public boolean checkUserLiked(String articleId) {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        
        LambdaQueryWrapper<UserLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLike::getUserId, userId)
                   .eq(UserLike::getArticleId, articleId);
        
        return userLikeMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 获取用户点赞的文章ID列表
     * @param userId 用户ID
     * @return 点赞的文章ID列表
     */
    public String[] getUserLikedArticleIds(Long userId) {
        LambdaQueryWrapper<UserLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLike::getUserId, userId)
                   .select(UserLike::getArticleId);
        
        return userLikeMapper.selectList(queryWrapper)
                           .stream()
                           .map(UserLike::getArticleId)
                           .toArray(String[]::new);
    }
    
    /**
     * 获取用户点赞的文章分页列表
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 点赞文章分页数据
     */
    public Page<UserLikedArticleResponseDTO> getUserLikedArticlePage(Integer currentPage, Integer size) {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        
        // 查询用户点赞记录
        Page<UserLike> userLikePage = new Page<>(currentPage, size);
        LambdaQueryWrapper<UserLike> likeQueryWrapper = new LambdaQueryWrapper<>();
        likeQueryWrapper.eq(UserLike::getUserId, userId)
                       .orderByDesc(UserLike::getCreateTime);
        
        userLikeMapper.selectPage(userLikePage, likeQueryWrapper);
        List<UserLike> likeList = userLikePage.getRecords();
        
        // 提取文章ID列表
        List<String> articleIds = likeList.stream()
                                     .map(UserLike::getArticleId)
                                     .collect(Collectors.toList());
        
        if (articleIds.isEmpty()) {
            // 没有点赞记录，返回空列表
            return new Page<UserLikedArticleResponseDTO>().setRecords(new ArrayList<>())
                                                .setCurrent(currentPage)
                                                .setSize(size)
                                                .setTotal(0);
        }
        
        // 查询文章信息
        LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
        articleQueryWrapper.in(Article::getId, articleIds)
                          .select(Article::getId, Article::getTitle, Article::getUserId);
        List<Article> articles = articleMapper.selectList(articleQueryWrapper);
        
        // 查询作者信息
        List<Long> authorIds = articles.stream()
                                     .map(Article::getUserId)
                                     .distinct()
                                     .collect(Collectors.toList());
        
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, authorIds)
                        .select(User::getId, User::getName, User::getUsername);
        List<User> authors = userMapper.selectList(userQueryWrapper);
        
        // 构建作者ID到名称的映射
        Map<Long, String> authorNameMap = authors.stream()
                                              .collect(Collectors.toMap(
                                                  User::getId,
                                                  user -> user.getName() != null ? user.getName() : user.getUsername()
                                              ));
        
        // 构建文章ID到文章的映射
        Map<String, Article> articleMap = articles.stream()
                                             .collect(Collectors.toMap(Article::getId, article -> article));
        
        // 构建点赞ID到点赞时间的映射
        Map<String, UserLike> likeMap = likeList.stream()
                                          .collect(Collectors.toMap(UserLike::getArticleId, like -> like));
        
        // 组装结果
        List<UserLikedArticleResponseDTO> records = new ArrayList<>();
        for (String articleId : articleIds) {
            Article article = articleMap.get(articleId);
            if (article != null) {
                UserLikedArticleResponseDTO record = new UserLikedArticleResponseDTO();
                record.setId(article.getId());
                record.setTitle(article.getTitle());
                record.setAuthorName(authorNameMap.getOrDefault(article.getUserId(), "未知作者"));
                record.setCreateTime(likeMap.get(articleId).getCreateTime());
                records.add(record);
            }
        }
        
        // 创建返回分页对象
        Page<UserLikedArticleResponseDTO> resultPage = new Page<>();
        resultPage.setRecords(records);
        resultPage.setCurrent(userLikePage.getCurrent());
        resultPage.setSize(userLikePage.getSize());
        resultPage.setTotal(userLikePage.getTotal());
        
        return resultPage;
    }
} 