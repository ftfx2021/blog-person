package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 友情链接状态更新DTO
 */
@Data
@Schema(description = "友情链接状态更新DTO")
public class FriendLinkStatusUpdateDTO {
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态(0:隐藏,1:显示)", example = "1")
    private Integer status;
}


