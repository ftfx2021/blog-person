package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询DTO
 */
@Data
@Schema(description = "用户查询DTO")
public class UserQueryDTO {
    
    @Schema(description = "用户名(模糊查询)")
    private String username;
    
    @Schema(description = "姓名(模糊查询)")
    private String name;
    
    @Schema(description = "角色编码")
    private String roleCode;
    
    @Schema(description = "状态(0:禁用,1:正常)")
    private Integer status;
    
    @Schema(description = "当前页码", example = "1")
    private Integer currentPage = 1;
    
    @Schema(description = "每页显示数量", example = "10")
    private Integer size = 10;
}

