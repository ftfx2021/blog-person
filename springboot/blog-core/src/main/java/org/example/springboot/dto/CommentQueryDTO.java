package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * 评论查询DTO
 */
@Schema(description = "评论查询DTO")
public class CommentQueryDTO {
    
    @Schema(description = "文章标题（模糊查询）", example = "Java基础")
    private String articleTitle;
    
    @Schema(description = "评论状态（0:待审核,1:已通过,2:已拒绝）", example = "1")
    private Integer status;
    
    @Min(value = 1, message = "当前页必须大于0")
    @Schema(description = "当前页", example = "1", defaultValue = "1")
    private Integer currentPage = 1;
    
    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "10", defaultValue = "10")
    private Integer size = 10;

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
