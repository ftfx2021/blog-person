package org.example.springboot.controller.es;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.entity.es.ArticleDocument;
import org.example.springboot.service.es.ArticleEsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 文章搜索控制器
 */
@Tag(name = "文章搜索管理")
@RestController
@RequestMapping("/search/article")
@Slf4j
public class ArticleSearchController {

    @Autowired
    private ArticleEsService articleEsService;

    /**
     * 全文搜索
     */
    @Operation(summary = "全文搜索")
    @GetMapping("/query")
    public Result<Page<ArticleDocument>> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<ArticleDocument> result = articleEsService.searchDocuments(keyword, current, size);
            return Result.success(result);
        } catch (IOException e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 高级搜索
     */
    @Operation(summary = "高级搜索")
    @GetMapping("/advanced")
    public Result<Page<ArticleDocument>> advancedSearch(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类名称") @RequestParam(required = false) String categoryName,
            @Parameter(description = "标签列表") @RequestParam(required = false) List<String> tags,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<ArticleDocument> result = articleEsService.advancedSearch(keyword, categoryName, tags, current, size);
            return Result.success(result);
        } catch (IOException e) {
            log.error("高级搜索失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 同步数据到ES
     */
    @Operation(summary = "同步数据到ES")
    @PostMapping("/sync")
    public Result<Void> syncData() {
        try {
            articleEsService.syncAllFromDatabase();
            return Result.success();
        } catch (IOException e) {
            log.error("同步失败: {}", e.getMessage(), e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 重建索引
     */
    @Operation(summary = "重建索引")
    @PostMapping("/rebuild")
    public Result<Void> rebuildIndex() {
        try {
            articleEsService.rebuildIndex();
            return Result.success();
        } catch (IOException e) {
            log.error("重建索引失败: {}", e.getMessage(), e);
            return Result.error("重建索引失败: " + e.getMessage());
        }
    }

    /**
     * 创建索引
     */
    @Operation(summary = "创建索引")
    @PostMapping("/index/create")
    public Result<Void> createIndex() {
        try {
            articleEsService.createIndex();
            return Result.success();
        } catch (IOException e) {
            log.error("创建索引失败: {}", e.getMessage(), e);
            return Result.error("创建索引失败: " + e.getMessage());
        }
    }

    /**
     * 删除索引
     */
    @Operation(summary = "删除索引")
    @DeleteMapping("/index/delete")
    public Result<Void> deleteIndex() {
        try {
            articleEsService.deleteIndex();
            return Result.success();
        } catch (IOException e) {
            log.error("删除索引失败: {}", e.getMessage(), e);
            return Result.error("删除索引失败: " + e.getMessage());
        }
    }

    /**
     * 检查索引是否存在
     */
    @Operation(summary = "检查索引是否存在")
    @GetMapping("/index/exist")
    public Result<Boolean> indexExist() {
        try {
            boolean exists = articleEsService.indexExist();
            return Result.success(exists);
        } catch (IOException e) {
            log.error("检查索引失败: {}", e.getMessage(), e);
            return Result.error("检查索引失败: " + e.getMessage());
        }
    }
}
