package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "知识库统计DTO")
public class KnowledgeBaseStatsDTO {
    
    @Schema(description = "已发布文章总数")
    private Integer totalPublishedArticles;
    
    @Schema(description = "成功向量化文章数")
    private Integer successfullyVectorized;
    
    @Schema(description = "向量化处理中文章数")
    private Integer inProgressVectorization;
    
    @Schema(description = "向量化失败文章数")
    private Integer failedVectorization;
    
    @Schema(description = "待向量化文章数")
    private Integer pendingVectorization;
    
    @Schema(description = "文章向量化状态列表")
    private List<ArticleVectorizationStatus> articleStatuses;
    
    @Data
    @Schema(description = "文章向量化状态")
    public static class ArticleVectorizationStatus {
        
        @Schema(description = "文章ID")
        private String articleId;
        
        @Schema(description = "文章标题")
        private String title;
        
        @Schema(description = "向量化状态")
        private Integer vectorizedStatus;
        
        @Schema(description = "向量化失败原因")
        private String vectorizedErrorReason;
        
        @Schema(description = "最后成功向量化时间")
        private LocalDateTime lastVectorizedSuccessTime;
    }
}
