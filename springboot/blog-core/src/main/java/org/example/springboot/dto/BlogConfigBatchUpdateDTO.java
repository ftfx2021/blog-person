package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * 博客配置批量更新DTO
 */
@Data
@Schema(description = "博客配置批量更新DTO")
public class BlogConfigBatchUpdateDTO {
    
    @NotEmpty(message = "配置数据不能为空")
    @Schema(description = "配置键值对映射", example = "{\"site_name\": \"我的博客\", \"site_desc\": \"个人博客系统\"}")
    private Map<String, String> configMap;
}
