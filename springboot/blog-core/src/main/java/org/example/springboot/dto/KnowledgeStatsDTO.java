package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库统计信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库统计信息")
public class KnowledgeStatsDTO {
    
    @Schema(description = "知识库是否已加载")
    private Boolean loaded;
    
    @Schema(description = "文件总数")
    private Integer totalFiles;
    
    @Schema(description = "文件名列表")
    private List<String> fileNames;
    
    @Schema(description = "错误信息")
    private String error;
}
