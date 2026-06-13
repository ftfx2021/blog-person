package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "点赞状态响应DTO")
public class LikeStatusResponseDTO {
    
    @Schema(description = "是否已点赞")
    private Boolean liked;
    
    public LikeStatusResponseDTO() {}
    
    public LikeStatusResponseDTO(Boolean liked) {
        this.liked = liked;
    }
}
