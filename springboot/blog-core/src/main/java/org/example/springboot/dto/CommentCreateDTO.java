package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 评论创建DTO
 */
@Schema(description = "评论创建DTO")
public class CommentCreateDTO {
    
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500个字符")
    @Schema(description = "评论内容", required = true, example = "这是一条评论")
    private String content;
    
    @NotNull(message = "文章ID不能为空")
    @Schema(description = "文章ID", required = true, example = "article-123")
    private String articleId;
    
    @Schema(description = "父评论ID（0表示顶级评论）", example = "0")
    private Long parentId;
    
    @Schema(description = "回复用户ID（0表示不是回复）", example = "0")
    private Long toUserId;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }
}
