package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.ProductCreateDTO;
import org.example.springboot.dto.ProductResponseDTO;
import org.example.springboot.dto.ProductUpdateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.ProductService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理接口")
@RestController
@RequestMapping("/product")
@Slf4j
public class ProductController {
    
    @Resource
    private ProductService productService;
    
    @Operation(summary = "分页查询商品列表")
    @GetMapping("/page")
    public Result<Page<ProductResponseDTO>> getProductPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        log.info("商品分页查询: current={}, size={}, productName={}, categoryId={}, status={}", 
                 current, size, productName, categoryId, status);
        Page<ProductResponseDTO> pageResult = productService.getProductPage(current, size, productName, categoryId, status);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "根据ID获取商品详情")
    @GetMapping("/{id}")
    public Result<ProductResponseDTO> getProductById(@PathVariable String id) {
        ProductResponseDTO product = productService.getProductById(id);
        return Result.success(product);
    }
    
    @Operation(summary = "创建商品")
    @PostMapping
    public Result<?> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        productService.createProduct(dto);
        return Result.success();
    }
    
    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<?> updateProduct(@PathVariable String id, @Valid @RequestBody ProductUpdateDTO dto) {
        dto.setId(id);
        productService.updateProduct(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<?> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return Result.success();
    }
    
    @Operation(summary = "更新商品状态")
    @PutMapping("/{id}/status")
    public Result<?> updateProductStatus(
            @PathVariable String id,
            @Parameter(description = "状态（0:下架, 1:上架）") @RequestParam Integer status) {
        productService.updateProductStatus(id, status);
        return Result.success();
    }
    
    @Operation(summary = "增加商品浏览次数")
    @PostMapping("/{id}/view")
    public Result<?> incrementViewCount(@PathVariable String id) {
        productService.incrementViewCount(id);
        return Result.success();
    }

}
