package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "分类更新DTO")
public class CategoryUpdateDTO {
    
    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long id;
    
    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1到50个字符之间")
    private String name;

    @Schema(description = "分类描述")
    @Size(max = 200, message = "分类描述不能超过200个字符")
    private String description;

    @Schema(description = "分类主色调(十六进制颜色值)")
    private String mainColor;

    @Schema(description = "父分类ID(0表示顶级分类)")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer orderNum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMainColor() {
        return mainColor;
    }

    public void setMainColor(String mainColor) {
        this.mainColor = mainColor;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }
}

