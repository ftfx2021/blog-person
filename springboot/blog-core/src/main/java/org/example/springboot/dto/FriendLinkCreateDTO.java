package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 友情链接创建DTO
 */
@Data
@Schema(description = "友情链接创建DTO")
public class FriendLinkCreateDTO {
    
    @NotBlank(message = "链接名称不能为空")
    @Size(max = 50, message = "链接名称长度不能超过50个字符")
    @Schema(description = "链接名称", example = "示例网站")
    private String name;
    
    @NotBlank(message = "链接地址不能为空")
    @Size(max = 200, message = "链接地址长度不能超过200个字符")
    @Schema(description = "链接地址", example = "https://example.com")
    private String url;
    
    @Size(max = 200, message = "链接logo长度不能超过200个字符")
    @Schema(description = "链接logo", example = "https://example.com/logo.png")
    private String logo;
    
    @Size(max = 200, message = "链接描述长度不能超过200个字符")
    @Schema(description = "链接描述", example = "这是一个示例网站")
    private String description;
    
    @Schema(description = "排序号", example = "1")
    private Integer orderNum = 0;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态(0:隐藏,1:显示)", example = "1")
    private Integer status = 1;
}


