package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 友情链接查询DTO
 */
@Data
@Schema(description = "友情链接查询DTO")
public class FriendLinkQueryDTO {
    
    @Schema(description = "链接名称", example = "示例网站")
    private String name;
    
    @Schema(description = "状态(0:隐藏,1:显示)", example = "1")
    private Integer status;
    
    @Schema(description = "当前页码", example = "1")
    private Integer currentPage = 1;
    
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;
}


