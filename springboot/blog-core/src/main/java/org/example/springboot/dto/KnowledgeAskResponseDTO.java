package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库问答响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库问答响应")
public class KnowledgeAskResponseDTO {
    
    @Schema(description = "AI生成的答案")
    private String answer;
    
    @Schema(description = "相关文档来源")
    private List<DocumentSource> sources;
    
    @Schema(description = "问题")
    private String question;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文档来源信息")
    public static class DocumentSource {
        @Schema(description = "来源文件名")
        private String source;
        
        @Schema(description = "文档片段内容")
        private String content;
        
        @Schema(description = "相似度分数")
        private Double score;
    }
}
