package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载记录响应DTO
 */
@Data
@Schema(description = "下载记录响应DTO")
public class DownloadRecordResponseDTO {
    
    @Schema(description = "记录ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "商品ID")
    private String productId;
    
    @Schema(description = "商品名称")
    private String productName;
    
    @Schema(description = "商品封面图URL")
    private String productCoverUrl;
    
    @Schema(description = "下载时间")
    private LocalDateTime downloadTime;
    
    @Schema(description = "IP地址")
    private String ipAddress;
}
