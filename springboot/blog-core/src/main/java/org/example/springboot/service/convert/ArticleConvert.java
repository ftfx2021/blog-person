package org.example.springboot.service.convert;

import org.example.springboot.dto.ArticleCollectUserResponseDTO;
import org.example.springboot.dto.ArticleCreateDTO;
import org.example.springboot.dto.ArticleLikeUserResponseDTO;
import org.example.springboot.dto.ArticleResponseDTO;
import org.example.springboot.dto.ArticleUpdateDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.User;

import java.time.LocalDateTime;

/**
 * 文章转换器
 */
public class ArticleConvert {
    
    /**
     * Article实体转换为ArticleResponseDTO（完整版，包含内容）
     * @param article 文章实体
     * @return 文章响应DTO
     */
    public static ArticleResponseDTO toResponseDTO(Article article) {
        if (article == null) {
            return null;
        }
        
        ArticleResponseDTO dto = new ArticleResponseDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setContent(article.getContent());
        dto.setHtmlContent(article.getHtmlContent());
        dto.setSummary(article.getSummary());
        dto.setOutline(article.getOutline());
        dto.setCoverImage(article.getCoverImage());
        dto.setMainColor(article.getMainColor());
        dto.setCategoryId(article.getCategoryId());
        dto.setUserId(article.getUserId());
        dto.setViewCount(article.getViewCount());
        dto.setLikeCount(article.getLikeCount());
        dto.setCommentCount(article.getCommentCount());
        dto.setStatus(article.getStatus());
        dto.setCreateTime(article.getCreateTime());
        dto.setUpdateTime(article.getUpdateTime());
        dto.setIsTop(article.getIsTop());
        dto.setIsRecommend(article.getIsRecommend());
        dto.setIsPasswordProtected(article.getIsPasswordProtected());
        dto.setPasswordExpireTime(article.getPasswordExpireTime());
        // isVectorized is a legacy field, derive from vectorizedStatus if needed
        // dto.setIsVectorized(article.getVectorizedStatus() != null && article.getVectorizedStatus() == 2 ? 1 : 0);
        dto.setVectorizedStatus(article.getVectorizedStatus());
        dto.setVectorizedErrorReason(article.getVectorizedErrorReason());
        dto.setLastVectorizedSuccessTime(article.getLastVectorizedSuccessTime());
        dto.setVectorizedProcessAt(article.getVectorizedProcessAt());
        dto.setVectorizedRetryCount(article.getVectorizedRetryCount());
        dto.setCategoryName(article.getCategoryName());
        dto.setAuthorName(article.getAuthorName());
        dto.setTags(article.getTags());
        
        return dto;
    }
    
    /**
     * Article实体转换为ArticleResponseDTO（列表版，不包含内容和大纲）
     * 用于分页查询等场景，减少数据传输量
     * @param article 文章实体
     * @return 文章响应DTO（不含content、htmlContent、outline）
     */
    public static ArticleResponseDTO toListResponseDTO(Article article) {
        if (article == null) {
            return null;
        }
        
        ArticleResponseDTO dto = new ArticleResponseDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        // 不设置 content、htmlContent、outline，减少数据传输
        dto.setSummary(article.getSummary());
        dto.setCoverImage(article.getCoverImage());
        dto.setMainColor(article.getMainColor());
        dto.setCategoryId(article.getCategoryId());
        dto.setUserId(article.getUserId());
        dto.setViewCount(article.getViewCount());
        dto.setLikeCount(article.getLikeCount());
        dto.setCommentCount(article.getCommentCount());
        dto.setStatus(article.getStatus());
        dto.setCreateTime(article.getCreateTime());
        dto.setUpdateTime(article.getUpdateTime());
        dto.setIsTop(article.getIsTop());
        dto.setIsRecommend(article.getIsRecommend());
        dto.setIsPasswordProtected(article.getIsPasswordProtected());
        dto.setPasswordExpireTime(article.getPasswordExpireTime());
        // isVectorized is a legacy field, derive from vectorizedStatus if needed
        // dto.setIsVectorized(article.getVectorizedStatus() != null && article.getVectorizedStatus() == 2 ? 1 : 0);
        dto.setVectorizedStatus(article.getVectorizedStatus());
        dto.setVectorizedErrorReason(article.getVectorizedErrorReason());
        dto.setLastVectorizedSuccessTime(article.getLastVectorizedSuccessTime());
        dto.setVectorizedProcessAt(article.getVectorizedProcessAt());
        dto.setVectorizedRetryCount(article.getVectorizedRetryCount());
        dto.setCategoryName(article.getCategoryName());
        dto.setAuthorName(article.getAuthorName());
        dto.setTags(article.getTags());
        
        return dto;
    }
    
    /**
     * ArticleCreateDTO转换为Article实体
     * @param createDTO 文章创建DTO
     * @return 文章实体
     */
    public static Article toEntity(ArticleCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }
        
        Article article = new Article();
        // 如果前端传入了ID（UUID），则使用前端的ID
        if (createDTO.getId() != null && !createDTO.getId().trim().isEmpty()) {
            article.setId(createDTO.getId());
        }
        article.setTitle(createDTO.getTitle());
        article.setContent(createDTO.getContent());
        article.setHtmlContent(createDTO.getHtmlContent());
        article.setSummary(createDTO.getSummary());
        article.setOutline(createDTO.getOutline());
        article.setCoverImage(createDTO.getCoverImage());
        article.setMainColor(createDTO.getMainColor());
        article.setCategoryId(createDTO.getCategoryId());
        article.setStatus(createDTO.getStatus());
        article.setIsTop(createDTO.getIsTop());
        article.setIsRecommend(createDTO.getIsRecommend());
        article.setIsPasswordProtected(createDTO.getIsPasswordProtected());
        article.setPasswordExpireTime(createDTO.getPasswordExpireTime());
        article.setTags(createDTO.getTags());
        
        return article;
    }
    
    /**
     * ArticleUpdateDTO转换为Article实体
     * @param updateDTO 文章更新DTO
     * @return 文章实体
     */
    public static Article toEntity(ArticleUpdateDTO updateDTO) {
        if (updateDTO == null) {
            return null;
        }
        
        Article article = new Article();
        article.setTitle(updateDTO.getTitle());
        article.setContent(updateDTO.getContent());
        article.setHtmlContent(updateDTO.getHtmlContent());
        article.setSummary(updateDTO.getSummary());
        article.setOutline(updateDTO.getOutline());
        article.setCoverImage(updateDTO.getCoverImage());
        article.setMainColor(updateDTO.getMainColor());
        article.setCategoryId(updateDTO.getCategoryId());
        article.setStatus(updateDTO.getStatus());
        article.setIsTop(updateDTO.getIsTop());
        article.setIsRecommend(updateDTO.getIsRecommend());
        article.setIsPasswordProtected(updateDTO.getIsPasswordProtected());
        article.setPasswordExpireTime(updateDTO.getPasswordExpireTime());
        article.setTags(updateDTO.getTags());
        
        return article;
    }
    
    /**
     * 用户和点赞时间转换为ArticleLikeUserResponseDTO
     * @param user 用户实体
     * @param likeTime 点赞时间
     * @return 文章点赞用户响应DTO
     */
    public static ArticleLikeUserResponseDTO toLikeUserResponseDTO(User user, LocalDateTime likeTime) {
        if (user == null) {
            return null;
        }
        
        ArticleLikeUserResponseDTO dto = new ArticleLikeUserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setAvatar(user.getAvatar());
        dto.setEmail(user.getEmail());
        dto.setLikeTime(likeTime);
        
        return dto;
    }
    
    /**
     * 用户和收藏时间转换为ArticleCollectUserResponseDTO
     * @param user 用户实体
     * @param collectTime 收藏时间
     * @return 文章收藏用户响应DTO
     */
    public static ArticleCollectUserResponseDTO toCollectUserResponseDTO(User user, LocalDateTime collectTime) {
        if (user == null) {
            return null;
        }
        
        ArticleCollectUserResponseDTO dto = new ArticleCollectUserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setAvatar(user.getAvatar());
        dto.setEmail(user.getEmail());
        dto.setCollectTime(collectTime);
        
        return dto;
    }
}
