package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库问答请求DTO
 */
@Data
@Schema(description = "知识库问答请求")
public class KnowledgeAskDTO {
    
    @Schema(description = "用户问题", required = true)
    private String question;
    
    @Schema(description = "返回相关文档数量", defaultValue = "5")
    private Integer topK = 5;
}
