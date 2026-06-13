package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.BlogConfigBatchUpdateDTO;
import org.example.springboot.dto.BlogConfigCreateDTO;
import org.example.springboot.service.BlogConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "博客设置接口")
@RestController
@RequestMapping("/config")
@Slf4j
public class BlogConfigController {
    
    @Resource
    private BlogConfigService blogConfigService;
    
    @Operation(summary = "获取所有博客配置")
    @GetMapping
    public Result<Map<String, String>> getAllConfigs() {
        log.info("获取所有博客配置");
        Map<String, String> configMap = blogConfigService.getAllConfigs();
        return Result.success(configMap);
    }
    
    @Operation(summary = "根据配置键获取配置值")
    @GetMapping("/{key}")
    public Result<String> getConfigByKey(@PathVariable String key) {
        log.info("根据配置键获取配置值, key={}", key);
        String value = blogConfigService.getConfigByKey(key);
        return Result.success(value);
    }
    
    @Operation(summary = "管理员更新博客配置")
    @PutMapping("/admin")
    public Result<Void> updateConfig(@Valid @RequestBody BlogConfigBatchUpdateDTO updateDTO) {
        log.info("管理员更新博客配置, configMap={}", updateDTO.getConfigMap());
        blogConfigService.updateBatchConfig(updateDTO.getConfigMap());
        return Result.success();
    }
    
    @Operation(summary = "管理员新增博客配置")
    @PostMapping("/admin")
    public Result<Void> addConfig(@Valid @RequestBody BlogConfigCreateDTO createDTO) {
        log.info("管理员新增博客配置, key={}, value={}", createDTO.getConfigKey(), createDTO.getConfigValue());
        blogConfigService.addConfig(createDTO.getConfigKey(), createDTO.getConfigValue());
        return Result.success();
    }
    
    @Operation(summary = "管理员删除博客配置")
    @DeleteMapping("/admin/{key}")
    public Result<Void> deleteConfig(@PathVariable String key) {
        log.info("管理员删除博客配置, key={}", key);
        blogConfigService.deleteConfig(key);
        return Result.success();
    }
} 