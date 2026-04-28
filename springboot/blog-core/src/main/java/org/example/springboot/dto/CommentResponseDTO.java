package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应DTO
 */
@Schema(description = "评论响应DTO")
public class CommentResponseDTO {
    
    @Schema(description = "评论ID")
    private Long id;
    
    @Schema(description = "评论内容")
    private String content;
    
    @Schema(description = "文章ID")
    private String articleId;
    
    @Schema(description = "文章标题")
    private String articleTitle;
    
    @Schema(description = "评论用户ID")
    private Long userId;
    
    @Schema(description = "评论用户名")
    private String username;
    
    @Schema(description = "评论用户昵称")
    private String userName;
    
    @Schema(description = "评论用户头像")
    private String userAvatar;
    
    @Schema(description = "父评论ID（0表示顶级评论）")
    private Long parentId;
    
    @Schema(description = "父评论内容")
    private String parentContent;
    
    @Schema(description = "回复用户ID（0表示不是回复）")
    private Long toUserId;
    
    @Schema(description = "回复用户名")
    private String toUsername;
    
    @Schema(description = "回复用户昵称")
    private String toUserName;
    
    @Schema(description = "评论状态（0:待审核,1:已通过,2:已拒绝）")
    private Integer status;
    
    @Schema(description = "是否为回复评论")
    private Boolean isReply;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "回复列表")
    private List<CommentResponseDTO> replies;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentContent() {
        return parentContent;
    }

    public void setParentContent(String parentContent) {
        this.parentContent = parentContent;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsReply() {
        return isReply;
    }

    public void setIsReply(Boolean isReply) {
        this.isReply = isReply;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<CommentResponseDTO> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponseDTO> replies) {
        this.replies = replies;
    }
}
