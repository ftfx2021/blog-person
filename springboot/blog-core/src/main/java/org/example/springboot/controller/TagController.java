package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.springboot.dto.TagCreateDTO;
import org.example.springboot.dto.TagResponseDTO;
import org.example.springboot.dto.TagUpdateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "标签管理接口")
@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private TagService tagService;
    
    @Operation(summary = "获取所有标签")
    @GetMapping
    public Result<List<TagResponseDTO>> getAllTags() {
        List<TagResponseDTO> tags = tagService.getAllTags();
        return Result.success(tags);
    }
    
    @Operation(summary = "根据ID获取标签")
    @GetMapping("/{id}")
    public Result<TagResponseDTO> getTagById(@PathVariable Long id) {
        TagResponseDTO tag = tagService.getTagById(id);
        return Result.success(tag);
    }
    
    @Operation(summary = "新增标签")
    @PostMapping
    public Result<?> addTag(@Valid @RequestBody TagCreateDTO createDTO) {
        tagService.addTag(createDTO);
        return Result.success();
    }
    
    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<?> updateTag(@PathVariable Long id, @Valid @RequestBody TagUpdateDTO updateDTO) {
        updateDTO.setId(id);
        tagService.updateTag(updateDTO);
        return Result.success();
    }
    
    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<?> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
} 