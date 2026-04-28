package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.MySentenceCreateDTO;
import org.example.springboot.dto.MySentenceResponseDTO;
import org.example.springboot.dto.MySentenceUpdateDTO;
import org.example.springboot.service.MySentenceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的句子 Controller
 */
@Tag(name = "句子管理接口")
@RestController
@RequestMapping("/sentence")
@Slf4j
public class MySentenceController {

    @Resource
    private MySentenceService mySentenceService;

    @Operation(summary = "分页查询句子列表")
    @GetMapping("/page")
    public Result<Page<MySentenceResponseDTO>> getPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否首页展示：0-否，1-是") @RequestParam(required = false) Integer isHomepage) {

        log.info("分页查询句子列表: current={}, size={}, keyword={}, isHomepage={}", current, size, keyword, isHomepage);
        Page<MySentenceResponseDTO> page = mySentenceService.getPage(current, size, keyword, isHomepage);
        return Result.success(page);
    }

    @Operation(summary = "获取句子详情")
    @GetMapping("/{id}")
    public Result<MySentenceResponseDTO> getById(@PathVariable Integer id) {
        log.info("获取句子详情: id={}", id);
        MySentenceResponseDTO dto = mySentenceService.getById(id);
        return Result.success(dto);
    }

    @Operation(summary = "创建句子")
    @PostMapping
    public Result<?> create(@RequestBody MySentenceCreateDTO createDTO) {
        log.info("创建句子: {}", createDTO);
        mySentenceService.create(createDTO);
        return Result.success();
    }

    @Operation(summary = "更新句子")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Integer id, @RequestBody MySentenceUpdateDTO updateDTO) {
        log.info("更新句子: id={}, dto={}", id, updateDTO);
        mySentenceService.update(id, updateDTO);
        return Result.success();
    }

    @Operation(summary = "删除句子")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        log.info("删除句子: id={}", id);
        mySentenceService.delete(id);
        return Result.success();
    }

    @Operation(summary = "切换首页展示状态")
    @PutMapping("/{id}/homepage")
    public Result<?> toggleHomepage(
            @PathVariable Integer id,
            @Parameter(description = "是否首页展示：0-否，1-是") @RequestParam Integer isHomepage) {
        log.info("切换首页展示状态: id={}, isHomepage={}", id, isHomepage);
        mySentenceService.toggleHomepage(id, isHomepage);
        return Result.success();
    }

    @Operation(summary = "获取随机首页句子")
    @GetMapping("/random/homepage")
    public Result<MySentenceResponseDTO> getRandomHomepageSentence() {
        log.info("获取随机首页句子");
        MySentenceResponseDTO dto = mySentenceService.getRandomHomepageSentence();
        return Result.success(dto);
    }
}
