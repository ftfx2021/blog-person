package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.ProductCategoryCreateDTO;
import org.example.springboot.dto.ProductCategoryResponseDTO;
import org.example.springboot.dto.ProductCategoryUpdateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.ProductCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品分类管理接口")
@RestController
@RequestMapping("/product-category")
@Slf4j
public class ProductCategoryController {
    
    @Resource
    private ProductCategoryService productCategoryService;
    
    @Operation(summary = "获取所有商品分类")
    @GetMapping
    public Result<List<ProductCategoryResponseDTO>> getAllCategories() {
        List<ProductCategoryResponseDTO> categories = productCategoryService.getAllCategories();
        return Result.success(categories);
    }
    
    @Operation(summary = "获取启用状态的商品分类")
    @GetMapping("/enabled")
    public Result<List<ProductCategoryResponseDTO>> getEnabledCategories() {
        List<ProductCategoryResponseDTO> categories = productCategoryService.getEnabledCategories();
        return Result.success(categories);
    }
    
    @Operation(summary = "根据ID获取商品分类")
    @GetMapping("/{id}")
    public Result<ProductCategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        ProductCategoryResponseDTO category = productCategoryService.getCategoryById(id);
        return Result.success(category);
    }
    
    @Operation(summary = "创建商品分类")
    @PostMapping
    public Result<?> createCategory(@Valid @RequestBody ProductCategoryCreateDTO dto) {
        productCategoryService.createCategory(dto);
        return Result.success();
    }
    
    @Operation(summary = "更新商品分类")
    @PutMapping("/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @Valid @RequestBody ProductCategoryUpdateDTO dto) {
        dto.setId(id);
        productCategoryService.updateCategory(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除商品分类")
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return Result.success();
    }
}
