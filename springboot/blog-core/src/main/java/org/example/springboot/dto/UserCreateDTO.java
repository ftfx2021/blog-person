package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建DTO
 */
@Data
@Schema(description = "用户创建DTO")
public class UserCreateDTO {
    
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    private String username;
    
    @Schema(description = "密码")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;
    
    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Schema(description = "角色编码")
    private String roleCode;
    
    @Schema(description = "姓名")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String name;
    
    @Schema(description = "性别")
    private String sex;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "状态(0:禁用,1:正常)")
    private Integer status;
}