package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签更新DTO
 */
@Data
@Schema(description = "标签更新DTO")
public class TagUpdateDTO {
    
    @Schema(description = "标签ID")
    private Long id;
    
    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 20, message = "标签名称长度必须在1到20个字符之间")
    private String name;

    @Schema(description = "标签颜色")
    @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$", message = "标签颜色必须是有效的十六进制颜色值")
    private String color;

    @Schema(description = "标签文字颜色")
    @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$", message = "标签文字颜色必须是有效的十六进制颜色值")
    private String textColor;
}

