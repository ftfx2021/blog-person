package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.springboot.dto.CategoryCreateDTO;
import org.example.springboot.dto.CategoryResponseDTO;
import org.example.springboot.dto.CategoryUpdateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类管理接口")
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Resource
    private CategoryService categoryService;
    
    @Operation(summary = "获取所有分类（树形结构）")
    @GetMapping("/tree")
    public Result<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }
    
    @Operation(summary = "获取所有分类（列表结构）")
    @GetMapping
    public Result<List<CategoryResponseDTO>> getCategoryList() {
        List<CategoryResponseDTO> categories = categoryService.getCategoryList();
        return Result.success(categories);
    }
    
    @Operation(summary = "获取顶级分类（包含子分类文章数量累计）")
    @GetMapping("/top-level")
    public Result<List<CategoryResponseDTO>> getTopLevelCategories() {
        List<CategoryResponseDTO> categories = categoryService.getTopLevelCategories();
        return Result.success(categories);
    }
    
    @Operation(summary = "根据ID获取分类")
    @GetMapping("/{id}")
    public Result<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return Result.success(category);
    }
    
    @Operation(summary = "新增分类")
    @PostMapping
    public Result<?> addCategory(@Valid @RequestBody CategoryCreateDTO categoryCreateDTO) {
        categoryService.addCategory(categoryCreateDTO);
        return Result.success();
    }
    
    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdateDTO categoryUpdateDTO) {
        categoryUpdateDTO.setId(id);
        categoryService.updateCategory(categoryUpdateDTO);
        return Result.success();
    }
    
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
} 