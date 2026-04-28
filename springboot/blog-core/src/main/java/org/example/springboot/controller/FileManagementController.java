package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.service.FileManagementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理控制器 - 全新重构版本
 * 
 * 支持三种文件上传模式：
 * 1. OneToOne：单个业务对象对应单个文件（如用户头像）
 * 2. OneToMany：单个业务对象对应多个文件（如商品图片列表）
 * 3. 富文本：非结构化数据中的文件引用（如文章内容中的图片）
 * 
 * @author system
 */
@Tag(name = "文件管理", description = "文件上传、移动、删除接口")
@RequestMapping("/file")
@RestController
@Slf4j
public class FileManagementController {

    @Resource
    private FileManagementService fileManagementService;

    @Operation(summary = "上传文件到临时目录", description = "第一步：上传文件到临时目录，返回临时URL")
    @PostMapping("/upload/temp")
    public Result<String> uploadToTemp(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file) {
        try {
            log.info("上传文件到临时目录: {}", file.getOriginalFilename());
            String tempPath = fileManagementService.uploadToTemp(file);
            return Result.success(tempPath);
        } catch (Exception e) {
            log.error("上传文件到临时目录失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量上传文件到临时目录", description = "批量上传多个文件到临时目录")
    @PostMapping("/upload/temp/batch")
    public Result<List<String>> uploadMultipleToTemp(
            @Parameter(description = "上传的文件数组") @RequestParam("files") MultipartFile[] files) {
        try {
            log.info("批量上传文件到临时目录，数量: {}", files.length);
            List<String> tempPaths = fileManagementService.uploadMultipleToTemp(files);
            return Result.success(tempPaths);
        } catch (Exception e) {
            log.error("批量上传文件失败: {}", e.getMessage(), e);
            return Result.error("批量上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "移动临时文件到正式目录", 
               description = "第二步：将临时文件移动到正式目录（OneToOne场景）")
    @PostMapping("/move/formal")
    public Result<String> moveTempToFormal(
            @Parameter(description = "临时文件URL") @RequestParam("tempUrl") String tempUrl,
            @Parameter(description = "业务类型") @RequestParam("businessType") String businessType,
            @Parameter(description = "业务ID") @RequestParam("businessId") String businessId) {
        try {
            log.info("移动临时文件: tempUrl={}, businessType={}, businessId={}", 
                    tempUrl, businessType, businessId);
            String formalPath = fileManagementService.moveTempToFormal(tempUrl, businessType, businessId);
            return Result.success(formalPath);
        } catch (Exception e) {
            log.error("移动临时文件失败: {}", e.getMessage(), e);
            return Result.error("文件移动失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量移动临时文件到正式目录", 
               description = "第二步：批量移动临时文件（OneToMany场景）")
    @PostMapping("/move/formal/batch")
    public Result<List<String>> moveTempToFormalBatch(
            @Parameter(description = "临时文件URL列表") @RequestBody List<String> tempUrls,
            @Parameter(description = "业务类型") @RequestParam("businessType") String businessType,
            @Parameter(description = "业务ID") @RequestParam("businessId") String businessId) {
        try {
            log.info("批量移动临时文件: 数量={}, businessType={}, businessId={}", 
                    tempUrls.size(), businessType, businessId);
            List<String> formalPaths = fileManagementService.moveTempToFormalBatch(
                    tempUrls, businessType, businessId);
            return Result.success(formalPaths);
        } catch (Exception e) {
            log.error("批量移动临时文件失败: {}", e.getMessage(), e);
            return Result.error("批量移动失败: " + e.getMessage());
        }
    }

    @Operation(summary = "处理富文本中的临时文件", 
               description = "第二步：处理富文本内容，将临时文件移动到正式目录并替换URL")
    @PostMapping("/process/richtext")
    public Result<String> processRichTextFiles(
            @Parameter(description = "富文本内容") @RequestBody String htmlContent,
            @Parameter(description = "业务类型") @RequestParam("businessType") String businessType,
            @Parameter(description = "业务ID") @RequestParam("businessId") String businessId) {
        try {
            log.info("处理富文本文件: businessType={}, businessId={}", businessType, businessId);
            String processedContent = fileManagementService.processRichTextFiles(
                    htmlContent, businessType, businessId);
            return Result.success(processedContent);
        } catch (Exception e) {
            log.error("处理富文本文件失败: {}", e.getMessage(), e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除业务文件", description = "删除指定业务的所有文件")
    @DeleteMapping("/business/{businessType}/{businessId}")
    public Result<Void> deleteBusinessFiles(
            @Parameter(description = "业务类型") @PathVariable String businessType,
            @Parameter(description = "业务ID") @PathVariable String businessId) {
        try {
            log.info("删除业务文件: businessType={}, businessId={}", businessType, businessId);
            fileManagementService.deleteBusinessFiles(businessType, businessId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除业务文件失败: {}", e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清理过期临时文件", description = "系统管理接口：清理24小时前的临时文件")
    @PostMapping("/cleanup/temp")
    public Result<Integer> cleanupExpiredTempFiles() {
        try {
            log.info("清理过期临时文件");
            int deletedCount = fileManagementService.cleanupExpiredTempFiles();
            return Result.success(deletedCount);
        } catch (Exception e) {
            log.error("清理过期临时文件失败: {}", e.getMessage(), e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }
}
