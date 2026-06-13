package org.example.springboot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 文件移动请求DTO
 * 
 * @author system
 */
@Data
public class FileMoveRequest {
    
    /**
     * 临时文件URL
     */
    @NotBlank(message = "临时文件URL不能为空")
    private String tempUrl;
    
    /**
     * 业务类型（如：user_avatar, article_cover, product_image）
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;
    
    /**
     * 业务ID
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;
}
