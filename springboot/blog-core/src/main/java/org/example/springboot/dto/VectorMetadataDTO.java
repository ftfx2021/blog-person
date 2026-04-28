package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "向量元数据DTO")
public class VectorMetadataDTO {
    
    @Schema(description = "文章ID")
    private String articleId;
    
    @Schema(description = "文章标题")
    private String title;
    
    @Schema(description = "分类ID")
    private Long categoryId;
    
    @Schema(description = "总分片数")
    private Long totalChunks;
    
    @Schema(description = "分片索引")
    private Long chunkIndex;
    
    @Schema(description = "分片ID")
    private String chunkId;
    
    @Schema(description = "分片内容预览(前100字符)")
    private String contentPreview;
}
