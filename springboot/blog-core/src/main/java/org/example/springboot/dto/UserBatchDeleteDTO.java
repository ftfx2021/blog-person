package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户批量删除DTO
 */
@Data
@Schema(description = "用户批量删除DTO")
public class UserBatchDeleteDTO {
    
    @Schema(description = "用户ID列表")
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> ids;
}

