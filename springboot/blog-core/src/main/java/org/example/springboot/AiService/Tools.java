package org.example.springboot.AiService;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.Category;
import org.example.springboot.entity.Tag;
import org.example.springboot.mapper.CategoryMapper;
import org.example.springboot.mapper.TagMapper;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI服务工具函数定义
 * 用于AI服务中需要的数据库查询和其他工具操作
 */
@Slf4j
@Component
public class Tools {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private CategoryMapper categoryMapper;

    /**
     * 获取所有可用的标签列表
     * 
     * @return 标签详细信息的JSON格式字符串
     */
    @Tool(
        name = "getAllTags",
        description = "获取系统中所有可用的标签列表，用于标签推荐时提供现有标签参考。返回格式为JSON字符串，包含每个标签的ID、名称、颜色等完整信息。推荐标签时必须从这个列表中选择存在的标签。",
        returnDirect = false
    )
    public String getAllTags() {
        try {
            List<Tag> tags = tagMapper.selectList(null);
            if (tags.isEmpty()) {
                return "[]";
            }
            
            // 构建标签信息的JSON字符串
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);
                if (i > 0) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append(String.format(
                    "{\"id\":%d,\"name\": \"%s\",\"color\":\"%s\"}", 
                    tag.getId(), 
                    tag.getName(), 
                    tag.getColor() != null ? tag.getColor() : "#409EFF"
                ));
            }
            jsonBuilder.append("]");
            
            String result = jsonBuilder.toString();
            log.info("获取标签列表成功，共{}个标签", tags.size());
            return result;
        } catch (Exception e) {
            log.error("获取标签列表失败: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * 获取所有可用的分类列表
     * 
     * @return 分类详细信息的JSON格式字符串
     */
    @Tool(
        name = "getAllCategories",
        description = "获取系统中所有可用的分类列表，用于分类推荐时提供现有分类参考。返回格式为JSON字符串，包含每个分类的ID、名称、描述等完整信息。推荐分类时必须从这个列表中选择存在的分类。",
        returnDirect = false
    )
    public String getAllCategories() {
        try {
            List<Category> categories = categoryMapper.selectList(null);
            if (categories.isEmpty()) {
                return "[]";
            }
            
            // 构建分类信息的JSON字符串
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                if (i > 0) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append(String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\"}", 
                    category.getId(), 
                    category.getName(), 
                    category.getDescription() != null ? category.getDescription() : ""
                ));
            }
            jsonBuilder.append("]");
            
            String result = jsonBuilder.toString();
            log.info("获取分类列表成功，共{}个分类", categories.size());
            return result;
        } catch (Exception e) {
            log.error("获取分类列表失败: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * 根据标签名称查找标签详细信息
     * 
     * @param tagName 标签名称
     * @return 标签详细信息
     */
    @Tool(
        name = "getTagByName",
        description = "根据标签名称查找标签的详细信息，包括ID、名称、颜色等。用于验证推荐的标签是否存在于系统中。",
        returnDirect = false
    )
    public String getTagByName(@ToolParam(description = "要查找的标签名称") String tagName) {
        try {
            List<Tag> tags = tagMapper.selectList(null);
            Tag foundTag = tags.stream()
                .filter(tag -> tag.getName().equals(tagName))
                .findFirst()
                .orElse(null);
            
            if (foundTag != null) {
                return String.format("标签存在 - ID: %d, 名称: %s, 颜色: %s", 
                    foundTag.getId(), foundTag.getName(), foundTag.getColor());
            } else {
                return "标签不存在";
            }
        } catch (Exception e) {
            log.error("查找标签失败: {}", e.getMessage(), e);
            return "查找标签失败";
        }
    }

    /**
     * 根据分类名称查找分类详细信息
     * 
     * @param categoryName 分类名称
     * @return 分类详细信息
     */
    @Tool(
        name = "getCategoryByName",
        description = "根据分类名称查找分类的详细信息，包括ID、名称、描述等。用于验证推荐的分类是否存在于系统中。",
        returnDirect = false
    )
    public String getCategoryByName(@ToolParam(description = "要查找的分类名称") String categoryName) {
        try {
            List<Category> categories = categoryMapper.selectList(null);
            Category foundCategory = categories.stream()
                .filter(category -> category.getName().equals(categoryName))
                .findFirst()
                .orElse(null);
            
            if (foundCategory != null) {
                return String.format("分类存在 - ID: %d, 名称: %s, 描述: %s", 
                    foundCategory.getId(), foundCategory.getName(), 
                    foundCategory.getDescription() != null ? foundCategory.getDescription() : "无描述");
            } else {
                return "分类不存在";
            }
        } catch (Exception e) {
            log.error("查找分类失败: {}", e.getMessage(), e);
            return "查找分类失败";
        }
    }
}
