package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章密码操作结果DTO
 */
@Data
@Schema(description = "文章密码操作结果DTO")
public class ArticlePasswordResultDTO {
    
    @Schema(description = "是否开启密码保护")
    private Boolean enabled;
    
    @Schema(description = "生成的明文密码(仅在开启时返回)")
    private String password;
    
    @Schema(description = "密码过期时间")
    private LocalDateTime expireTime;
    
    @Schema(description = "操作消息")
    private String message;
    
    /**
     * 创建开启密码保护的结果
     */
    public static ArticlePasswordResultDTO enabled(String password, LocalDateTime expireTime) {
        ArticlePasswordResultDTO dto = new ArticlePasswordResultDTO();
        dto.setEnabled(true);
        dto.setPassword(password);
        dto.setExpireTime(expireTime);
        dto.setMessage("密码保护已开启");
        return dto;
    }
    
    /**
     * 创建关闭密码保护的结果
     */
    public static ArticlePasswordResultDTO disabled() {
        ArticlePasswordResultDTO dto = new ArticlePasswordResultDTO();
        dto.setEnabled(false);
        dto.setPassword(null);
        dto.setExpireTime(null);
        dto.setMessage("密码保护已关闭");
        return dto;
    }
    
    /**
     * 创建已存在密码保护的结果（密码已设置，但不返回明文密码）
     */
    public static ArticlePasswordResultDTO existingPassword(LocalDateTime expireTime) {
        ArticlePasswordResultDTO dto = new ArticlePasswordResultDTO();
        dto.setEnabled(true);
        dto.setPassword(null); // 不返回明文密码
        dto.setExpireTime(expireTime);
        dto.setMessage("密码保护已开启");
        return dto;
    }
}
