package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户收藏文章响应DTO")
public class UserCollectedArticleResponseDTO {
    
    @Schema(description = "文章ID")
    private String id;
    
    @Schema(description = "文章标题")
    private String title;
    
    @Schema(description = "作者名称")
    private String authorName;
    
    @Schema(description = "封面图片")
    private String coverImage;
    
    @Schema(description = "文章摘要")
    private String summary;
    
    @Schema(description = "文章创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "浏览次数")
    private Integer viewCount;
    
    @Schema(description = "点赞次数")
    private Integer likeCount;
    
    @Schema(description = "评论次数")
    private Integer commentCount;
    
    @Schema(description = "收藏时间")
    private LocalDateTime collectTime;
}
