package org.example.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    
    /**
     * 文件路径（相对路径）
     */
    private String filePath;
    
    /**
     * 文件访问URL
     */
    private String fileUrl;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否为临时文件
     */
    private Boolean isTemp;
}
