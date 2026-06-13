package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.TagCreateDTO;
import org.example.springboot.dto.TagResponseDTO;
import org.example.springboot.dto.TagUpdateDTO;
import org.example.springboot.entity.ArticleTag;
import org.example.springboot.entity.Tag;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleTagMapper;
import org.example.springboot.mapper.TagMapper;
import org.example.springboot.service.convert.TagConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {
    @Resource
    private TagMapper tagMapper;
    
    @Resource
    private ArticleTagMapper articleTagMapper;
    
    @Resource
    private TagConvert tagConvert;
    
    /**
     * 获取所有标签
     */
    public List<TagResponseDTO> getAllTags() {
        List<Tag> tags = tagMapper.selectList(null);
        
        // 设置每个标签的文章数量
        for (Tag tag : tags) {
            setArticleCount(tag);
        }
        
        return tagConvert.entityListToResponseDtoList(tags);
    }
    
    /**
     * 根据ID获取标签
     */
    public TagResponseDTO getTagById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 设置文章数量
        setArticleCount(tag);
        
        return tagConvert.entityToResponseDto(tag);
    }
    
    /**
     * 新增标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTag(TagCreateDTO createDTO) {
        // 检查标签名称是否已存在
        checkTagNameExists(createDTO.getName(), null);
        
        // 转换DTO为实体
        Tag tag = tagConvert.createDtoToEntity(createDTO);
        
        // 如果没有设置颜色，则设置默认颜色
        if (tag.getColor() == null || tag.getColor().isEmpty()) {
            tag.setColor("#409EFF");
        }
        
        // 如果没有设置文字颜色，则根据背景颜色自动设置合适的文字颜色
        if (tag.getTextColor() == null || tag.getTextColor().isEmpty()) {
            tag.setTextColor(getContrastTextColor(tag.getColor()));
        }
        
        tagMapper.insert(tag);
    }
    
    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(TagUpdateDTO updateDTO) {
        // 验证标签是否存在
        Tag existTag = tagMapper.selectById(updateDTO.getId());
        if (existTag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 检查标签名称是否已存在
        checkTagNameExists(updateDTO.getName(), updateDTO.getId());
        
        // 使用转换类更新实体
        tagConvert.updateEntityFromDto(existTag, updateDTO);
        
        tagMapper.updateById(existTag);
    }
    
    /**
     * 删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        // 验证标签是否存在
        Tag existTag = tagMapper.selectById(id);
        if (existTag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 检查关联数据
        checkRelatedData(id);
        
        tagMapper.deleteById(id);
    }
    
    /**
     * 设置标签的文章数量
     */
    private void setArticleCount(Tag tag) {
        LambdaQueryWrapper<ArticleTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleTag::getTagId, tag.getId());
        
        Integer count = Math.toIntExact(articleTagMapper.selectCount(queryWrapper));
        tag.setArticleCount(count);
    }

    /**
     * 检查标签名称是否已存在
     */
    private void checkTagNameExists(String name, Long excludeId) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getName, name);
        if (excludeId != null) {
            queryWrapper.ne(Tag::getId, excludeId);
        }
        if (tagMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("标签名称已存在");
        }
    }

    /**
     * 检查关联数据
     */
    private void checkRelatedData(Long tagId) {
        LambdaQueryWrapper<ArticleTag> articleTagWrapper = new LambdaQueryWrapper<>();
        articleTagWrapper.eq(ArticleTag::getTagId, tagId);
        if (articleTagMapper.selectCount(articleTagWrapper) > 0) {
            throw new BusinessException("此标签被文章使用，无法删除");
        }
    }

    /**
     * 根据背景颜色计算合适的文字颜色
     * @param bgColor 背景颜色
     * @return 对比度高的文字颜色
     */
    private String getContrastTextColor(String bgColor) {
        if (bgColor == null || bgColor.isEmpty()) {
            return "#2D3748"; // 默认深色文字
        }

        // 解析十六进制颜色
        String color = bgColor.replace("#", "");
        if (color.length() == 3) {
            // 将3位颜色转换为6位
            color = String.valueOf(color.charAt(0)) + color.charAt(0) +
                    color.charAt(1) + color.charAt(1) +
                    color.charAt(2) + color.charAt(2);
        }

        try {
            // 计算亮度
            int r = Integer.parseInt(color.substring(0, 2), 16);
            int g = Integer.parseInt(color.substring(2, 4), 16);
            int b = Integer.parseInt(color.substring(4, 6), 16);

            // 使用相对亮度公式计算
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

            // 亮度大于0.5使用深色文字，否则使用白色文字
            return luminance > 0.5 ? "#2D3748" : "#FFFFFF";
        } catch (Exception e) {
            // 解析失败时返回默认深色文字
            return "#2D3748";
        }
    }
} 