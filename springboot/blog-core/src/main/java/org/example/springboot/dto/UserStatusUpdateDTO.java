package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态更新DTO
 */
@Data
@Schema(description = "用户状态更新DTO")
public class UserStatusUpdateDTO {
    
    @Schema(description = "状态(0:禁用,1:正常)")
    @NotNull(message = "状态不能为空")
    private Integer status;
}

