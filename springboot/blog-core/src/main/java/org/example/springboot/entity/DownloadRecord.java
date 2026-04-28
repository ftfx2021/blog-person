package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("download_record")
@Schema(description = "下载记录实体类")
public class DownloadRecord {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;
    
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Schema(description = "商品ID（UUID）")
    @NotBlank(message = "商品ID不能为空")
    private String productId;
    
    @Schema(description = "下载时间")
    private LocalDateTime downloadTime;
    
    @Schema(description = "IP地址")
    private String ipAddress;
}
