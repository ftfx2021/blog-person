package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录响应DTO
 */
@Data
@Schema(description = "用户登录响应DTO")
public class UserLoginResponseDTO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "角色编码")
    private String roleCode;
    
    @Schema(description = "姓名")
    private String name;
    
    @Schema(description = "性别")
    private String sex;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "状态(0:禁用,1:正常)")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "JWT令牌")
    private String token;
}

