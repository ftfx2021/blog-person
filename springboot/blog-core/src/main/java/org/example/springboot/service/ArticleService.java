package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.config.rabbitmq.ArticleRabbitMqConfig;
import org.example.springboot.dto.*;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.ArticleTag;
import org.example.springboot.entity.Category;
import org.example.springboot.entity.Tag;
import org.example.springboot.entity.User;
import org.example.springboot.enums.ArticleVectorizedStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.service.convert.ArticleConvert;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleService {
    @Resource
    private ArticleMapper articleMapper;
    
    @Resource
    private CategoryMapper categoryMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private TagMapper tagMapper;
    
    @Resource
    private ArticleTagMapper articleTagMapper;
    
    @Resource
    private UserLikeMapper userLikeMapper;
    
    @Resource
    private UserCollectMapper userCollectMapper;
    

    
    @Resource
    private FileManagementService fileManagementService;
    
    @Resource
    private HtmlContentProcessor htmlContentProcessor;
    
    @Resource
    private org.example.springboot.AiService.rag.KnowledgeBaseService knowledgeBaseService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 分页查询文章
     */
    public Page<ArticleResponseDTO> getArticlesByPage(ArticleQueryDTO queryDTO) {
        Page<Article> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(queryDTO.getTitle())) {
            queryWrapper.like(Article::getTitle, queryDTO.getTitle());
        }
        if (queryDTO.getCategoryId() != null) {
            // 包含子分类的查询
            List<Long> categoryIds = getCategoryAndDescendantIds(queryDTO.getCategoryId());
            if (!categoryIds.isEmpty()) {
                queryWrapper.in(Article::getCategoryId, categoryIds);
            } else {
                queryWrapper.eq(Article::getCategoryId, queryDTO.getCategoryId());
            }
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Article::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getIsVectorized() != null) {
            // Map legacy isVectorized field to vectorizedStatus
            // isVectorized=0 means not vectorized (vectorizedStatus=0 or NULL)
            // isVectorized=1 means vectorized (vectorizedStatus=2)
            if (queryDTO.getIsVectorized() == 0) {
                // 未向量化：vectorizedStatus为0或NULL
                queryWrapper.and(w -> w.isNull(Article::getVectorizedStatus)
                        .or().eq(Article::getVectorizedStatus, 0));
            } else {
                // 已向量化：vectorizedStatus=2
                queryWrapper.eq(Article::getVectorizedStatus, 2);
            }
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Article::getCreateTime);
        
        Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
        
        if (articlePage.getRecords().isEmpty()) {
            Page<ArticleResponseDTO> responsePage = new Page<>();
            responsePage.setRecords(new ArrayList<>());
            responsePage.setCurrent(articlePage.getCurrent());
            responsePage.setSize(articlePage.getSize());
            responsePage.setTotal(articlePage.getTotal());
            return responsePage;
        }
        
        // 批量获取文章ID列表
        List<String> articleIds = articlePage.getRecords().stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        
        // 批量查询收藏数
        Map<String, Integer> collectCountMap = batchCalculateArticleCollectCount(articleIds);
        
        // 转换为响应DTO（列表版，不包含文章内容）
        List<ArticleResponseDTO> responseDTOList = new ArrayList<>();
        for (Article article : articlePage.getRecords()) {
            setCategoryAndAuthor(article);
            setArticleTags(article);
            
            // 转换为DTO（列表版，不含content、htmlContent、outline）
            ArticleResponseDTO dto = ArticleConvert.toListResponseDTO(article);
            
            // 设置收藏数
            dto.setCollectCount(collectCountMap.getOrDefault(article.getId(), 0));
            
            responseDTOList.add(dto);
        }
        
        // 创建响应分页对象
        Page<ArticleResponseDTO> responsePage = new Page<>();
        responsePage.setRecords(responseDTOList);
        responsePage.setCurrent(articlePage.getCurrent());
        responsePage.setSize(articlePage.getSize());
        responsePage.setTotal(articlePage.getTotal());
        
        return responsePage;
    }
    
    /**
     * 批量计算文章收藏数
     * @param articleIds 文章ID列表
     * @return 文章ID到收藏数的映射
     */
    private Map<String, Integer> batchCalculateArticleCollectCount(List<String> articleIds) {
        if (articleIds.isEmpty()) {
            return new HashMap<>();
        }
        
        LambdaQueryWrapper<org.example.springboot.entity.UserCollect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(org.example.springboot.entity.UserCollect::getArticleId, articleIds);
        List<org.example.springboot.entity.UserCollect> collectList = userCollectMapper.selectList(queryWrapper);
        
        // 统计每个文章的收藏数
        Map<String, Integer> collectCountMap = new HashMap<>();
        for (String articleId : articleIds) {
            collectCountMap.put(articleId, 0);
        }
        
        for (org.example.springboot.entity.UserCollect collect : collectList) {
            String articleId = collect.getArticleId();
            collectCountMap.put(articleId, collectCountMap.getOrDefault(articleId, 0) + 1);
        }
        
        return collectCountMap;
    }
    
    /**
     * 获取推荐文章
     */
    public List<ArticleResponseDTO> getRecommendArticles(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1) // 已发布的文章
                .eq(Article::getIsRecommend, 1) // 推荐的文章
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT " + limit);
        
        List<Article> articles = articleMapper.selectList(queryWrapper);
        
        List<ArticleResponseDTO> responseDTOList = new ArrayList<>();
        for (Article article : articles) {
            setCategoryAndAuthor(article);
            setArticleTags(article);
            
            // 转换为DTO（列表版，不含文章内容）
            ArticleResponseDTO dto = ArticleConvert.toListResponseDTO(article);
            responseDTOList.add(dto);
        }
        
        return responseDTOList;
    }
    
    /**
     * 获取热门文章
     */
    public List<ArticleResponseDTO> getHotArticles(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1) // 已发布的文章
                .orderByDesc(Article::getViewCount)
                .last("LIMIT " + limit);
        
        List<Article> articles = articleMapper.selectList(queryWrapper);
        
        List<ArticleResponseDTO> responseDTOList = new ArrayList<>();
        for (Article article : articles) {
            setCategoryAndAuthor(article);
            setArticleTags(article);
            
            // 转换为DTO（列表版，不含文章内容）
            ArticleResponseDTO dto = ArticleConvert.toListResponseDTO(article);
            responseDTOList.add(dto);
        }
        
        return responseDTOList;
    }
    
    /**
     * 根据ID获取文章详情
     * @param id 文章ID
     * @param password 访问密码（可选）
     * @return 文章详情
     */
    public Article getArticleById(String id, String password) {
        Article article = articleMapper.selectById(id);

        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 检查文章是否需要密码保护
        if (isPasswordProtectedAndValid(article)) {
            // 获取当前用户
            User currentUser = null;
            try {
                currentUser = JwtTokenUtils.getCurrentUser();
            } catch (Exception e) {
                // 未登录用户，currentUser 为 null
            }
            
            // 如果是管理员，直接放行
            if (currentUser != null && "ADMIN".equals(currentUser.getRoleCode())) {
                // 管理员无需密码验证
            } else {
                // 非管理员或未登录用户需要验证密码
                if (StringUtils.isBlank(password)) {
                    throw new ServiceException("文章已开启密码访问，请输入密码");
                }
                
                // 验证密码（明文比较）
                if (!password.equals(article.getAccessPassword())) {
                    throw new ServiceException("密码错误");
                }
            }
        }
   
        // 设置文章的分类名称和作者名称
        setCategoryAndAuthor(article);
        // 设置文章的标签
        setArticleTags(article);
        
        return article;
    }
    
    /**
     * 检查文章是否开启了有效的密码保护
     * @param article 文章实体
     * @return 是否需要密码验证
     */
    private boolean isPasswordProtectedAndValid(Article article) {
        // 未开启密码保护
        if (article.getIsPasswordProtected() == null || article.getIsPasswordProtected() != 1) {
            return false;
        }
        
        // 检查密码是否过期
        if (article.getPasswordExpireTime() != null && 
            LocalDateTime.now().isAfter(article.getPasswordExpireTime())) {
            // 密码已过期，自动关闭密码保护
            article.setIsPasswordProtected(0);
            article.setAccessPassword(null);
            article.setPasswordExpireTime(null);
            articleMapper.updateById(article);
            log.info("文章[{}]密码已过期，自动关闭密码保护", article.getId());
            return false;
        }
        
        // 没有设置密码
        if (article.getAccessPassword() == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 更新文章浏览量
     */
    public void updateArticleViewCount(String id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            articleMapper.updateById(article);
        }
    }
    
    /**
     * 新增文章
     */
    @Transactional
    public void addArticle(ArticleCreateDTO createDTO) {
        // 转换为实体类
        Article article = ArticleConvert.toEntity(createDTO);
        
        // 调试：检查接收到的大纲数据
        log.info("创建文章 - 接收到的大纲数据: {}", createDTO.getOutline());
        log.info("转换后的文章大纲: {}", article.getOutline());
        
        // 处理HTML内容，添加锚点ID
        if (article.getHtmlContent() != null) {
            String processedHtml = htmlContentProcessor.processHtmlContentWithAnchors(article.getHtmlContent());
            article.setHtmlContent(processedHtml);
            
            // 同步大纲数据的锚点ID
            if (article.getOutline() != null && !article.getOutline().trim().isEmpty()) {
                log.info("开始同步大纲锚点ID");
                String syncedOutline = htmlContentProcessor.syncOutlineWithHtmlAnchors(article.getOutline(), processedHtml);
                article.setOutline(syncedOutline);
                log.info("同步后的大纲: {}", article.getOutline());
            } else {
                log.info("大纲为空，跳过锚点同步");
            }
        }
        
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser != null) {
            article.setUserId(currentUser.getId());
        }

        // 默认值设置
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getLikeCount() == null) {
            article.setLikeCount(0);
        }
        if (article.getCommentCount() == null) {
            article.setCommentCount(0);
        }
        if (article.getIsTop() == null) {
            article.setIsTop(0);
        }
        if (article.getIsRecommend() == null) {
            article.setIsRecommend(0);
        }
        if (article.getIsPasswordProtected() == null) {
            article.setIsPasswordProtected(0);
        }
        
        // 如果开启密码保护，生成随机密码
        if (article.getIsPasswordProtected() != null && article.getIsPasswordProtected() == 1) {
            String generatedPassword = generateRandomPassword();
            article.setAccessPassword(generatedPassword);
            log.info("创建文章时生成密码保护，密码: {}, 过期时间: {}", generatedPassword, article.getPasswordExpireTime());
        }
        article.setVectorizedStatus(ArticleVectorizedStatus.NO.getCode());
        
        // 处理封面图（如果是临时URL）
        if (article.getCoverImage() != null && article.getCoverImage().contains("/temp/")) {
            try {
                String formalPath = fileManagementService.moveTempToFormal(
                    article.getCoverImage(),
                    "article_cover",
                    article.getId()
                );
                article.setCoverImage(formalPath);
                log.info("文章封面已移动到正式目录: {}", formalPath);
            } catch (Exception e) {
                log.error("移动文章封面失败，但不影响文章保存: articleId={}, error={}", article.getId(), e.getMessage());
            }
        }
        
        // 处理富文本中的临时文件
        try {
            String processedContent = fileManagementService.processRichTextFiles(
                article.getHtmlContent(), 
                "article_content", 
                article.getId()
            );
            article.setHtmlContent(processedContent);
        } catch (Exception e) {
            log.error("处理文章富文本文件失败，但不影响文章保存: articleId={}, error={}", article.getId(), e.getMessage());
        }
        
        // 保存文章
        articleMapper.insert(article);
        
        // 保存文章标签关联
        saveArticleTags(article);
        
        // 在事务提交后发送RabbitMQ消息
        if(article.getStatus()==1){
            // 使用 TransactionSynchronization 确保在事务提交后发送消息
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        try {
                            log.info("事务已提交，将创建文章消息发给rabbitmq...");
                            rabbitTemplate.convertAndSend(
                                    ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                                    ArticleRabbitMqConfig.ARTICLE_CREATE_ROUTING_KEY,
                                    article
                            );
                        } catch (AmqpException e) {
                            log.warn("发送MQ失败，可能MQ未启动");
                        }
                    }
                }
            );
        }
    }
    
    /**
     * 更新文章
     */
    @Transactional
    public void updateArticle(String id, ArticleUpdateDTO updateDTO) {
        // 验证文章是否存在
        Article existArticle = articleMapper.selectById(id);
        if (existArticle == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 验证当前用户是否有权限修改
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!Objects.equals(existArticle.getUserId(), currentUser.getId()) && 
            !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限修改此文章");
        }
        String oldContent = existArticle.getContent();
        
        // 转换为实体类并设置ID
        Article article = ArticleConvert.toEntity(updateDTO);
        article.setId(id);
        article.setUserId(existArticle.getUserId()); // 保持原作者ID
        
        // 调试：检查接收到的大纲数据
        log.info("更新文章 - 接收到的大纲数据: {}", updateDTO.getOutline());
        log.info("转换后的文章大纲: {}", article.getOutline());
        
        // 处理HTML内容，添加锚点ID
        if (article.getHtmlContent() != null) {
            String processedHtml = htmlContentProcessor.processHtmlContentWithAnchors(article.getHtmlContent());
            article.setHtmlContent(processedHtml);
            
            // 同步大纲数据的锚点ID
            if (article.getOutline() != null && !article.getOutline().trim().isEmpty()) {
                log.info("开始同步大纲锚点ID");
                String syncedOutline = htmlContentProcessor.syncOutlineWithHtmlAnchors(article.getOutline(), processedHtml);
                article.setOutline(syncedOutline);
                log.info("同步后的大纲: {}", article.getOutline());
            } else {
                log.info("大纲为空，跳过锚点同步");
            }
        }


        if(!oldContent.equals(article.getContent())) {
            log.info("内容变化，重置向量库");
            article.setVectorizedRetryCount(0);
            article.setVectorizedProcessAt(null);
            article.setVectorizedErrorReason(null);
            article.setVectorizedStatus(0);
            article.setLastVectorizedSuccessTime(null);
        }


        // 处理封面图（如果是临时URL）
        if (article.getCoverImage() != null && article.getCoverImage().contains("/temp/")) {
            try {
                // 删除旧封面图
                if (existArticle.getCoverImage() != null && !existArticle.getCoverImage().isEmpty()) {
                    fileManagementService.deleteFileByUrl(existArticle.getCoverImage());
                    log.info("已删除旧封面图: {}", existArticle.getCoverImage());
                }
                
                // 移动新封面图到正式目录
                String formalPath = fileManagementService.moveTempToFormal(
                    article.getCoverImage(),
                    "article_cover",
                    id
                );
                article.setCoverImage(formalPath);
                log.info("文章封面已移动到正式目录: {}", formalPath);
            } catch (Exception e) {
                log.error("移动文章封面失败，但不影响文章更新: articleId={}, error={}", id, e.getMessage());
            }
        }

        // 处理富文本中的临时文件
        try {
            String processedContent = fileManagementService.processRichTextFiles(
                article.getHtmlContent(), 
                "article_content", 
                id
            );
            article.setHtmlContent(processedContent);
        } catch (Exception e) {
            log.error("处理文章富文本文件失败，但不影响文章更新: articleId={}, error={}", id, e.getMessage());
        }
        
        // 更新文章
        articleMapper.updateById(article);
        
        // 更新文章标签关联
        // 先删除原有关联
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, id);
        articleTagMapper.delete(wrapper);
        
        // 重新保存关联
        saveArticleTags(article);
        
        // 在事务提交后发送RabbitMQ消息（避免消费者查询到旧数据）
        if(article.getStatus()==1){
            // 使用 TransactionSynchronization 确保在事务提交后发送消息
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            log.info("事务已提交，发布状态，将更新文章消息发给rabbitmq...");
                            rabbitTemplate.convertAndSend(
                                    ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                                    ArticleRabbitMqConfig.ARTICLE_UPDATE_ROUTING_KEY,
                                    article
                            );
                        } catch (AmqpException e) {
                            log.warn("发送MQ失败，可能MQ未启动");
                        }
                    }
                }
            );
        }

    }
    
    /**
     * 删除文章
     */
    @Transactional
    public void deleteArticle(String id) {
        // 验证文章是否存在
        Article existArticle = articleMapper.selectById(id);
        if (existArticle == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 验证当前用户是否有权限删除
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!Objects.equals(existArticle.getUserId(), currentUser.getId()) && 
            !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限删除此文章");
        }
        
        // 删除文章
        articleMapper.deleteById(id);
        
        // 删除文章标签关联
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, id);
        articleTagMapper.delete(wrapper);
        
        // 删除文章的所有文件
        try {
            fileManagementService.deleteBusinessFiles("article_cover", id);
            fileManagementService.deleteBusinessFiles("article_content", id);
            log.info("文章文件已删除: articleId={}", id);
        } catch (Exception e) {
            log.error("删除文章文件失败，但不影响文章删除: articleId={}, error={}", id, e.getMessage());
        }
        
        log.info("将删除文章消息发给rabbitmq...");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rabbitTemplate.convertAndSend(
                            ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                            ArticleRabbitMqConfig.ARTICLE_DELETE_ROUTING_KEY,
                            existArticle
                    );
                } catch (AmqpException e) {
                    log.warn("发送MQ失败，可能MQ未启动");
                }
            }
        });


    }
    
    /**
     * 修改文章状态
     */
    @Transactional
    public void updateArticleStatus(String id, Integer status) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        if(article.getStatus() .equals(status)) {
            throw new ServiceException("文章状态一致");
        }

        
        article.setStatus(status);
        log.info("新状态：{}",status);
        articleMapper.updateById(article);
        
        // 在事务提交后发送RabbitMQ消息（避免消费者查询到旧数据）
        if(status==1){
            // 使用 TransactionSynchronization 确保在事务提交后发送消息
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            log.info("事务已提交，文章状态变更为发布，通知消息队列");
                            rabbitTemplate.convertAndSend(
                                    ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                                    ArticleRabbitMqConfig.ARTICLE_PUBLISH_ROUTING_KEY,
                                    article
                            );
                        } catch (AmqpException e) {
                            log.warn("发送MQ失败，可能MQ未启动");
                        }
                    }
                }
            );
        }
    }
    
    /**
     * 设置文章的分类名称和作者名称
     */
    private void setCategoryAndAuthor(Article article) {
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                article.setCategoryName(category.getName());
            }
        }
        
        if (article.getUserId() != null) {
            User author = userMapper.selectById(article.getUserId());
            if (author != null) {
                article.setAuthorName(author.getUsername());
            }
        }
    }
    
    /**
     * 设置文章的标签
     */
    private void setArticleTags(Article article) {
        LambdaQueryWrapper<ArticleTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleTag::getArticleId, article.getId());
        List<ArticleTag> articleTags = articleTagMapper.selectList(queryWrapper);
        
        if (!articleTags.isEmpty()) {
            List<Long> tagIds = articleTags.stream()
                    .map(ArticleTag::getTagId)
                    .collect(Collectors.toList());
            
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(Tag::getId, tagIds);
            List<Tag> tags = tagMapper.selectList(tagWrapper);
            
            article.setTags(tags);
        } else {
            article.setTags(new ArrayList<>());
        }
    }
    
    /**
     * 保存文章标签关联
     */
    private void saveArticleTags(Article article) {
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            for (Tag tag : article.getTags()) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tag.getId());
                articleTagMapper.insert(articleTag);
            }
        }
    }
    
    /**
     * 根据分类ID获取文章列表
     */
    public Page<Article> getArticlesByCategoryId(Long categoryId, Integer currentPage, Integer size) {
        Page<Article> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        
        // 父分类应包含其所有子分类
        List<Long> categoryIds = getCategoryAndDescendantIds(categoryId);
        if (!categoryIds.isEmpty()) {
            queryWrapper.in(Article::getCategoryId, categoryIds)
                    .eq(Article::getStatus, 1) // 已发布的文章
                    .orderByDesc(Article::getCreateTime);
        } else {
            queryWrapper.eq(Article::getCategoryId, categoryId)
                .eq(Article::getStatus, 1) // 已发布的文章
                .orderByDesc(Article::getCreateTime);
        }
        
        Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
        
        // 设置文章的分类名称和作者名称
        for (Article article : articlePage.getRecords()) {
            setCategoryAndAuthor(article);
            // 设置文章的标签
            setArticleTags(article);
        }
        
        return articlePage;
    }

    /**
     * 获取某分类及其所有子分类ID集合（包含自身）
     */
    private List<Long> getCategoryAndDescendantIds(Long rootCategoryId) {
        List<Long> result = new ArrayList<>();
        if (rootCategoryId == null) {
            return result;
        }
        result.add(rootCategoryId);

        // 采用迭代广度优先，避免递归过深
        List<Long> queue = new ArrayList<>();
        queue.add(rootCategoryId);
        while (!queue.isEmpty()) {
            Long parentId = queue.remove(0);
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getParentId, parentId);
            List<Category> children = categoryMapper.selectList(wrapper);
            if (children != null && !children.isEmpty()) {
                for (Category c : children) {
                    if (c.getId() != null) {
                        result.add(c.getId());
                        queue.add(c.getId());
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * 根据标签ID获取文章列表
     */
    public Page<Article> getArticlesByTagId(Long tagId, Integer currentPage, Integer size) {
        // 先查询所有包含该标签的文章ID
        LambdaQueryWrapper<ArticleTag> articleTagWrapper = new LambdaQueryWrapper<>();
        articleTagWrapper.eq(ArticleTag::getTagId, tagId);
        List<ArticleTag> articleTags = articleTagMapper.selectList(articleTagWrapper);
        
        List<String> articleIds = articleTags.stream()
                .map(ArticleTag::getArticleId)
                .collect(Collectors.toList());
        
        if (articleIds.isEmpty()) {
            return new Page<>(currentPage, size);
        }
        
        // 根据文章ID查询文章
        Page<Article> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.in(Article::getId, articleIds)
                .eq(Article::getStatus, 1) // 已发布的文章
                .orderByDesc(Article::getCreateTime);
        
        Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
        
        // 设置文章的分类名称和作者名称
        for (Article article : articlePage.getRecords()) {
            setCategoryAndAuthor(article);
            // 设置文章的标签
            setArticleTags(article);
        }
        
        return articlePage;
    }
    
    /**
     * 搜索文章
     */
    public Page<Article> searchArticles(String keyword, Integer currentPage, Integer size) {
        Page<Article> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.like(Article::getTitle, keyword)
                .or()
                .like(Article::getContent, keyword)
                .or()
                .like(Article::getSummary, keyword)
                .eq(Article::getStatus, 1) // 已发布的文章
                .orderByDesc(Article::getCreateTime);
        
        Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
        
        // 设置文章的分类名称和作者名称
        for (Article article : articlePage.getRecords()) {
            setCategoryAndAuthor(article);
            // 设置文章的标签
            setArticleTags(article);
        }
        
        return articlePage;
    }
    
    /**
     * 获取文章点赞用户列表
     * @param articleId 文章ID
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 用户分页数据
     */
    public Page<ArticleLikeUserResponseDTO> getArticleLikeUsers(String articleId, Integer currentPage, Integer size) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 查询点赞用户ID列表
        LambdaQueryWrapper<org.example.springboot.entity.UserLike> likeQueryWrapper = new LambdaQueryWrapper<>();
        likeQueryWrapper.eq(org.example.springboot.entity.UserLike::getArticleId, articleId);
        Page<org.example.springboot.entity.UserLike> likePage = new Page<>(currentPage, size);
        
        userLikeMapper.selectPage(likePage, likeQueryWrapper);
        
        // 创建响应分页对象
        Page<ArticleLikeUserResponseDTO> resultPage = new Page<>();
        resultPage.setCurrent(likePage.getCurrent());
        resultPage.setSize(likePage.getSize());
        resultPage.setTotal(likePage.getTotal());
        
        if (likePage.getRecords().isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }
        
        // 获取用户ID列表
        List<Long> userIds = likePage.getRecords().stream()
                                   .map(org.example.springboot.entity.UserLike::getUserId)
                                   .collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(userQueryWrapper);
        
        // 构建用户ID到点赞时间的映射
        Map<Long, LocalDateTime> userLikeTimeMap = likePage.getRecords().stream()
                                                     .collect(Collectors.toMap(
                                                         org.example.springboot.entity.UserLike::getUserId,
                                                         org.example.springboot.entity.UserLike::getCreateTime
                                                     ));
        
        // 组装结果
        List<ArticleLikeUserResponseDTO> records = new ArrayList<>();
        for (User user : users) {
            LocalDateTime likeTime = userLikeTimeMap.get(user.getId());
            ArticleLikeUserResponseDTO dto = ArticleConvert.toLikeUserResponseDTO(user, likeTime);
            records.add(dto);
        }
        
        resultPage.setRecords(records);
        return resultPage;
    }
    
    /**
     * 获取文章收藏用户列表
     * @param articleId 文章ID
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 用户分页数据
     */
    public Page<ArticleCollectUserResponseDTO> getArticleCollectUsers(String articleId, Integer currentPage, Integer size) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 查询收藏用户ID列表
        LambdaQueryWrapper<org.example.springboot.entity.UserCollect> collectQueryWrapper = new LambdaQueryWrapper<>();
        collectQueryWrapper.eq(org.example.springboot.entity.UserCollect::getArticleId, articleId);
        Page<org.example.springboot.entity.UserCollect> collectPage = new Page<>(currentPage, size);
        
        userCollectMapper.selectPage(collectPage, collectQueryWrapper);
        
        // 创建响应分页对象
        Page<ArticleCollectUserResponseDTO> resultPage = new Page<>();
        resultPage.setCurrent(collectPage.getCurrent());
        resultPage.setSize(collectPage.getSize());
        resultPage.setTotal(collectPage.getTotal());
        
        if (collectPage.getRecords().isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }
        
        // 获取用户ID列表
        List<Long> userIds = collectPage.getRecords().stream()
                                     .map(org.example.springboot.entity.UserCollect::getUserId)
                                     .collect(Collectors.toList());
        
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(userQueryWrapper);
        
        // 构建用户ID到收藏时间的映射
        Map<Long, LocalDateTime> userCollectTimeMap = collectPage.getRecords().stream()
                                                         .collect(Collectors.toMap(
                                                             org.example.springboot.entity.UserCollect::getUserId,
                                                             org.example.springboot.entity.UserCollect::getCreateTime
                                                         ));
        
        // 组装结果
        List<ArticleCollectUserResponseDTO> records = new ArrayList<>();
        for (User user : users) {
            LocalDateTime collectTime = userCollectTimeMap.get(user.getId());
            ArticleCollectUserResponseDTO dto = ArticleConvert.toCollectUserResponseDTO(user, collectTime);
            records.add(dto);
        }
        
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * 获取最近发布的文章
     * @param size 获取数量
     * @return 文章列表
     */
    public List<ArticleResponseDTO> getRecentArticles(Integer size) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 1) // 已发布的文章
                    .orderByDesc(Article::getCreateTime)
                    .last("LIMIT " + size);

        List<Article> articles = articleMapper.selectList(queryWrapper);
        
        List<ArticleResponseDTO> responseDTOList = new ArrayList<>();
        for (Article article : articles) {
            setCategoryAndAuthor(article);
            setArticleTags(article);
            
            // 转换为DTO（列表版，不含文章内容）
            ArticleResponseDTO dto = ArticleConvert.toListResponseDTO(article);
            responseDTOList.add(dto);
        }
        
        return responseDTOList;
    }
    
    // ========== 文章密码保护相关方法 ==========
    
    /**
     * 随机密码字符集
     */
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 生成随机访问密码
     * @return 6位随机密码
     */
    public String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
    
    /**
     * 设置文章密码保护
     * @param articleId 文章ID
     * @param enablePassword 是否开启密码保护
     * @param expireTime 密码过期时间（可选，null表示永不过期）
     * @return 生成的明文密码（仅在开启时返回）
     */
    @Transactional
    public String setArticlePassword(String articleId, boolean enablePassword, LocalDateTime expireTime) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 验证权限
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!Objects.equals(article.getUserId(), currentUser.getId()) && 
            !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限修改此文章");
        }
        
        if (enablePassword) {
            // 生成随机密码
            String plainPassword = generateRandomPassword();
            
            article.setIsPasswordProtected(1);
            article.setAccessPassword(plainPassword);
            article.setPasswordExpireTime(expireTime);
            articleMapper.updateById(article);
            
            log.info("文章[{}]已开启密码保护，密码: {}, 过期时间: {}", articleId, plainPassword, expireTime);
            return plainPassword;
        } else {
            // 关闭密码保护
            article.setIsPasswordProtected(0);
            article.setAccessPassword(null);
            article.setPasswordExpireTime(null);
            articleMapper.updateById(article);
            
            log.info("文章[{}]已关闭密码保护", articleId);
            return null;
        }
    }
    
    /**
     * 检查文章是否需要密码访问
     * @param articleId 文章ID
     * @return 是否需要密码
     */
    public boolean isPasswordRequired(String articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }
        
        // 检查是否开启密码保护
        if (article.getIsPasswordProtected() == null || article.getIsPasswordProtected() != 1) {
            return false;
        }
        
        // 检查密码是否过期
        if (article.getPasswordExpireTime() != null && 
            LocalDateTime.now().isAfter(article.getPasswordExpireTime())) {
            // 密码已过期
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取文章密码信息（仅管理员或文章作者可调用）
     * @param articleId 文章ID
     * @return 密码信息DTO
     */
    public ArticlePasswordResultDTO getArticlePassword(String articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 验证权限
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!Objects.equals(article.getUserId(), currentUser.getId()) && 
            !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限查看此文章密码");
        }
        
        // 检查是否开启密码保护
        if (article.getIsPasswordProtected() == null || article.getIsPasswordProtected() != 1) {
            return ArticlePasswordResultDTO.disabled();
        }
        
        // 检查密码是否过期
        if (article.getPasswordExpireTime() != null && 
            LocalDateTime.now().isAfter(article.getPasswordExpireTime())) {
            // 密码已过期，自动关闭
            article.setIsPasswordProtected(0);
            article.setAccessPassword(null);
            article.setPasswordExpireTime(null);
            articleMapper.updateById(article);
            return ArticlePasswordResultDTO.disabled();
        }
        
        // 返回密码信息（明文密码）
        return ArticlePasswordResultDTO.enabled(article.getAccessPassword(), article.getPasswordExpireTime());
    }
    
    /**
     * 重新生成文章密码
     * @param articleId 文章ID
     * @return 新生成的明文密码
     */
    @Transactional
    public String regenerateArticlePassword(String articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        
        // 验证权限
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!Objects.equals(article.getUserId(), currentUser.getId()) && 
            !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限修改此文章");
        }
        
        // 检查是否已开启密码保护
        if (article.getIsPasswordProtected() == null || article.getIsPasswordProtected() != 1) {
            throw new ServiceException("文章未开启密码保护");
        }
        
        // 生成新密码
        String plainPassword = generateRandomPassword();
        
        article.setAccessPassword(plainPassword);
        articleMapper.updateById(article);
        
        log.info("文章[{}]密码已重新生成", articleId);
        return plainPassword;
    }
} 