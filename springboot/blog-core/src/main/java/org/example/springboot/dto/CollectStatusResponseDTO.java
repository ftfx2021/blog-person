package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文章收藏状态响应DTO")
public class CollectStatusResponseDTO {
    
    @Schema(description = "是否已收藏")
    private boolean collected;
    
    public CollectStatusResponseDTO() {}
    
    public CollectStatusResponseDTO(boolean collected) {
        this.collected = collected;
    }
}
