package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户点赞文章响应DTO")
public class UserLikedArticleResponseDTO {
    
    @Schema(description = "文章ID")
    private String id;
    
    @Schema(description = "文章标题")
    private String title;
    
    @Schema(description = "作者姓名")
    private String authorName;
    
    @Schema(description = "点赞时间")
    private LocalDateTime createTime;
    
    public UserLikedArticleResponseDTO() {}
    
    public UserLikedArticleResponseDTO(String id, String title, String authorName, LocalDateTime createTime) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.createTime = createTime;
    }
}
