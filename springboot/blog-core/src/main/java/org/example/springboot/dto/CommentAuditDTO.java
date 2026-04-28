package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 评论审核DTO
 */
@Schema(description = "评论审核DTO")
public class CommentAuditDTO {
    
    @NotNull(message = "评论状态不能为空")
    @Schema(description = "评论状态（1:通过,2:拒绝）", required = true, example = "1")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
