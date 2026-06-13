package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户统计数据DTO
 */
@Data
@Schema(description = "用户统计数据DTO")
public class UserStatsDTO {
    
    @Schema(description = "点赞数量")
    private Long likeCount;
    
    @Schema(description = "收藏数量")
    private Long collectCount;
    
    @Schema(description = "评论数量")
    private Long commentCount;
}

