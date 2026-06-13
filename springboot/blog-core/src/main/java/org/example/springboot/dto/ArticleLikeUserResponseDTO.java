package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文章点赞用户响应DTO")
public class ArticleLikeUserResponseDTO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "用户姓名")
    private String name;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "点赞时间")
    private LocalDateTime likeTime;
}


