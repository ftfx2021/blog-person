package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "文章查询DTO")
public class ArticleQueryDTO {
    
    @Schema(description = "文章标题（模糊查询）")
    private String title;
    
    @Schema(description = "分类ID")
    private Long categoryId;
    
    @Schema(description = "状态(0:草稿,1:已发布,2:已删除)")
    private Integer status;
    
    @Schema(description = "是否已向量化(0:否,1:是)")
    private Integer isVectorized;
    
    @Min(value = 1, message = "当前页必须大于0")
    @Schema(description = "当前页")
    private Integer currentPage = 1;
    
    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小")
    private Integer size = 10;
}


