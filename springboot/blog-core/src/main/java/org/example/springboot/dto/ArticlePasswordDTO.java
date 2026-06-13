package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章密码设置DTO
 */
@Data
@Schema(description = "文章密码设置DTO")
public class ArticlePasswordDTO {
    
    @Schema(description = "是否开启密码保护")
    private Boolean enablePassword;
    
    @Schema(description = "密码过期时间(NULL表示永不过期)")
    private LocalDateTime expireTime;
}
