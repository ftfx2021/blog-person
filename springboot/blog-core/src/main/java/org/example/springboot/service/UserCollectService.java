package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.dto.UserCollectedArticleResponseDTO;
import org.example.springboot.dto.UserCollectQueryDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.User;
import org.example.springboot.entity.UserCollect;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.UserCollectMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserCollectService {
    @Resource
    private UserCollectMapper userCollectMapper;
    
    @Resource
    private ArticleMapper articleMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 收藏或取消收藏文章
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 收藏状态 true-已收藏 false-未收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCollect(String articleId, Long userId) {
        // 参数校验
        if (StrUtil.isBlank(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 检查文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        // 查询用户是否已收藏
        LambdaQueryWrapper<UserCollect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollect::getUserId, userId)
                    .eq(UserCollect::getArticleId, articleId);
        
        UserCollect userCollect = userCollectMapper.selectOne(queryWrapper);
        
        if (userCollect == null) {
            // 未收藏，添加收藏记录
            userCollect = new UserCollect();
            userCollect.setUserId(userId);
            userCollect.setArticleId(articleId);
            userCollectMapper.insert(userCollect);
            
            return true;
        } else {
            // 已收藏，取消收藏
            userCollectMapper.deleteById(userCollect.getId());
            
            return false;
        }
    }
    
    /**
     * 检查用户是否收藏了文章
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否收藏
     */
    public boolean checkUserCollected(String articleId, Long userId) {
        // 参数校验
        if (StrUtil.isBlank(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        
        if (userId == null) {
            return false; // 未登录用户返回未收藏状态
        }
        
        LambdaQueryWrapper<UserCollect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollect::getUserId, userId)
                   .eq(UserCollect::getArticleId, articleId);
        
        return userCollectMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 获取用户收藏的文章ID列表
     * @param userId 用户ID
     * @return 收藏的文章ID列表
     */
    public String[] getUserCollectedArticleIds(Long userId) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        LambdaQueryWrapper<UserCollect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollect::getUserId, userId)
                   .select(UserCollect::getArticleId);
        
        return userCollectMapper.selectList(queryWrapper)
                           .stream()
                           .map(UserCollect::getArticleId)
                           .toArray(String[]::new);
    }
    
    /**
     * 获取用户收藏的文章分页列表
     * @param queryDTO 查询参数DTO
     * @return 收藏文章分页数据
     */
    public Page<UserCollectedArticleResponseDTO> getUserCollectedArticlePage(UserCollectQueryDTO queryDTO) {
        // 参数校验
        if (queryDTO == null) {
            throw new BusinessException("查询参数不能为空");
        }
        if (queryDTO.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 查询用户收藏记录
        Page<UserCollect> collectPage = queryUserCollects(queryDTO.getUserId(), queryDTO.getCurrentPage(), queryDTO.getSize());
        List<UserCollect> collectList = collectPage.getRecords();
        
        if (collectList.isEmpty()) {
            return createEmptyPage(queryDTO.getCurrentPage(), queryDTO.getSize());
        }
        
        // 获取文章详细信息并组装结果
        List<UserCollectedArticleResponseDTO> records = buildCollectedArticleList(collectList);
        
        // 创建返回分页对象
        return createResultPage(collectPage, records);
    }
    
    
    /**
     * 查询用户收藏记录
     */
    private Page<UserCollect> queryUserCollects(Long userId, Integer currentPage, Integer size) {
        Page<UserCollect> collectPage = new Page<>(currentPage, size);
        LambdaQueryWrapper<UserCollect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollect::getUserId, userId)
                   .orderByDesc(UserCollect::getCreateTime);
        
        userCollectMapper.selectPage(collectPage, queryWrapper);
        return collectPage;
    }
    
    /**
     * 创建空分页结果
     */
    private Page<UserCollectedArticleResponseDTO> createEmptyPage(Integer currentPage, Integer size) {
        return new Page<UserCollectedArticleResponseDTO>()
                .setRecords(new ArrayList<>())
                .setCurrent(currentPage)
                .setSize(size)
                .setTotal(0);
    }
    
    /**
     * 构建收藏文章列表
     */
    private List<UserCollectedArticleResponseDTO> buildCollectedArticleList(List<UserCollect> collectList) {
        // 提取文章ID列表
        List<String> articleIds = collectList.stream()
                                        .map(UserCollect::getArticleId)
                                        .collect(Collectors.toList());
        
        // 查询文章信息
        List<Article> articles = queryArticlesByIds(articleIds);
        
        // 查询作者信息
        Map<Long, String> authorNameMap = buildAuthorNameMap(articles);
        
        // 构建映射关系
        Map<String, Article> articleMap = articles.stream()
                                             .collect(Collectors.toMap(Article::getId, article -> article));
        
        Map<String, UserCollect> collectMap = collectList.stream()
                                                  .collect(Collectors.toMap(UserCollect::getArticleId, collect -> collect));
        
        // 组装结果
        List<UserCollectedArticleResponseDTO> records = new ArrayList<>();
        for (String articleId : articleIds) {
            Article article = articleMap.get(articleId);
            if (article != null) {
                UserCollectedArticleResponseDTO dto = buildCollectedArticleDTO(article, authorNameMap, collectMap);
                records.add(dto);
            }
        }
        
        return records;
    }
    
    /**
     * 根据文章ID列表查询文章信息
     */
    private List<Article> queryArticlesByIds(List<String> articleIds) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Article::getId, articleIds)
                   .select(Article::getId, Article::getTitle, Article::getUserId, 
                          Article::getCoverImage, Article::getSummary, Article::getCreateTime, 
                          Article::getViewCount, Article::getLikeCount, Article::getCommentCount);
        return articleMapper.selectList(queryWrapper);
    }
    
    /**
     * 构建作者名称映射
     */
    private Map<Long, String> buildAuthorNameMap(List<Article> articles) {
        List<Long> authorIds = articles.stream()
                                     .map(Article::getUserId)
                                     .distinct()
                                     .collect(Collectors.toList());
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getId, authorIds)
                   .select(User::getId, User::getName, User::getUsername);
        List<User> authors = userMapper.selectList(queryWrapper);
        
        return authors.stream()
                     .collect(Collectors.toMap(
                         User::getId,
                         user -> user.getName() != null ? user.getName() : user.getUsername()
                     ));
    }
    
    /**
     * 构建收藏文章DTO
     */
    private UserCollectedArticleResponseDTO buildCollectedArticleDTO(Article article, 
                                                                   Map<Long, String> authorNameMap, 
                                                                   Map<String, UserCollect> collectMap) {
        UserCollectedArticleResponseDTO dto = new UserCollectedArticleResponseDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setAuthorName(authorNameMap.getOrDefault(article.getUserId(), "未知作者"));
        dto.setCoverImage(article.getCoverImage());
        dto.setSummary(article.getSummary());
        dto.setCreateTime(article.getCreateTime());
        dto.setViewCount(article.getViewCount());
        dto.setLikeCount(article.getLikeCount());
        dto.setCommentCount(article.getCommentCount());
        dto.setCollectTime(collectMap.get(article.getId()).getCreateTime());
        return dto;
    }
    
    /**
     * 创建结果分页对象
     */
    private Page<UserCollectedArticleResponseDTO> createResultPage(Page<UserCollect> collectPage, 
                                                                  List<UserCollectedArticleResponseDTO> records) {
        Page<UserCollectedArticleResponseDTO> resultPage = new Page<>();
        resultPage.setRecords(records);
        resultPage.setCurrent(collectPage.getCurrent());
        resultPage.setSize(collectPage.getSize());
        resultPage.setTotal(collectPage.getTotal());
        return resultPage;
    }
} 