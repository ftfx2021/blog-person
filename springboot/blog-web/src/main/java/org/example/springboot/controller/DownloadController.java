package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.DownloadRecordResponseDTO;
import org.example.springboot.dto.PurchasedProductResponseDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.DownloadService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 下载管理接口
 */
@Tag(name = "下载管理接口")
@RestController
@RequestMapping("/download")
@Slf4j
public class DownloadController {
    
    @Resource
    private DownloadService downloadService;
    
    @Operation(summary = "获取用户已购商品列表")
    @GetMapping("/purchased")
    public Result<Page<PurchasedProductResponseDTO>> getPurchasedProducts(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName) {
        
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("查询已购商品列表: userId={}, current={}, size={}, productName={}", 
                 userId, current, size, productName);
        Page<PurchasedProductResponseDTO> pageResult = downloadService.getPurchasedProducts(userId, current, size, productName);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "记录下载行为")
    @PostMapping("/record/{productId}")
    public Result<?> recordDownload(
            @PathVariable String productId,
            HttpServletRequest request) {
        
        Long userId = JwtTokenUtils.getCurrentUserId();
        downloadService.recordDownload(userId, productId, request);
        return Result.success();
    }
    
    @Operation(summary = "获取用户下载记录")
    @GetMapping("/records")
    public Result<Page<DownloadRecordResponseDTO>> getDownloadRecords(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName) {
        
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("查询下载记录: userId={}, current={}, size={}", userId, current, size);
        Page<DownloadRecordResponseDTO> pageResult = downloadService.getDownloadRecords(userId, current, size, productName);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "获取商品下载链接")
    @GetMapping("/link/{productId}")
    public Result<String> getDownloadLink(@PathVariable String productId) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String downloadLink = downloadService.getDownloadLink(userId, productId);
        return Result.success(downloadLink);
    }
}
