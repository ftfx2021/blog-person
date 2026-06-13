package org.example.springboot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 富文本处理请求DTO
 * 
 * @author system
 */
@Data
public class RichTextProcessRequest {
    
    /**
     * 富文本内容（HTML）
     */
    @NotBlank(message = "富文本内容不能为空")
    private String htmlContent;
    
    /**
     * 业务类型（如：article_content）
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;
    
    /**
     * 业务ID
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;
}
