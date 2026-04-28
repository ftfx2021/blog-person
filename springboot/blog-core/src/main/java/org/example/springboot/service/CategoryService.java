package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.CategoryCreateDTO;
import org.example.springboot.dto.CategoryResponseDTO;
import org.example.springboot.dto.CategoryUpdateDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.Category;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.CategoryMapper;
import org.example.springboot.service.convert.CategoryConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    @Resource
    private CategoryMapper categoryMapper;
    
    @Resource
    private ArticleMapper articleMapper;
    
    /**
     * 获取所有分类（树形结构）
     */
    public List<CategoryResponseDTO> getAllCategories() {
        List<Category> allCategories = categoryMapper.selectList(null);
        
        // 设置每个分类的文章数量
        for (Category category : allCategories) {
            setArticleCount(category);
        }
        
        // 构建树形结构
        List<Category> categoryTree = buildCategoryTree(allCategories);
        
        // 累计父级包含子级的文章数量
        for (Category root : categoryTree) {
            accumulateArticleCount(root);
        }
        return CategoryConvert.toResponseDTOList(categoryTree);
    }
    
    /**
     * 获取所有分类（列表结构，包含文章数量）
     */
    public List<CategoryResponseDTO> getCategoryList() {
        List<Category> categories = categoryMapper.selectList(null);
        
        // 设置每个分类的文章数量
        for (Category category : categories) {
            setArticleCount(category);
        }
        
        // 按排序号排序
        List<Category> sortedCategories = categories.stream()
                .sorted(Comparator.comparing(Category::getOrderNum))
                .collect(Collectors.toList());
        
        return CategoryConvert.toResponseDTOList(sortedCategories);
    }
    
    /**
     * 获取顶级分类（包含子分类文章数量累计）
     */
    public List<CategoryResponseDTO> getTopLevelCategories() {
        // 获取所有分类
        List<Category> allCategories = categoryMapper.selectList(null);
        
        // 设置每个分类的文章数量
        for (Category category : allCategories) {
            setArticleCount(category);
        }
        
        // 获取顶级分类（parentId = 0）
        List<Category> topLevelCategories = allCategories.stream()
                .filter(category -> category.getParentId() == null || category.getParentId() == 0L)
                .sorted(Comparator.comparing(Category::getOrderNum))
                .collect(Collectors.toList());
        
        // 为每个顶级分类计算包含子分类的累计文章数量
        for (Category topCategory : topLevelCategories) {
            int totalArticleCount = calculateTotalArticleCount(topCategory.getId(), allCategories);
            topCategory.setArticleCount(totalArticleCount);
        }
        
        return CategoryConvert.toResponseDTOList(topLevelCategories);
    }
    
    /**
     * 根据ID获取分类
     */
    public CategoryResponseDTO getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 设置文章数量
        setArticleCount(category);
        
        return CategoryConvert.toResponseDTO(category);
    }
    
    /**
     * 新增分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(CategoryCreateDTO categoryCreateDTO) {
        // 转换DTO为实体
        Category category = CategoryConvert.toEntity(categoryCreateDTO);
        
        // 检查分类名称是否已存在
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, category.getName());
        if (categoryMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        
        // 设置默认值
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getOrderNum() == null) {
            category.setOrderNum(0);
        }
        
        // 验证分类层级限制（最多2级）
        validateCategoryLevel(category.getParentId());
        
        categoryMapper.insert(category);
    }
    
    /**
     * 更新分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryUpdateDTO categoryUpdateDTO) {
        // 转换DTO为实体
        Category category = CategoryConvert.toEntity(categoryUpdateDTO);
        
        // 验证分类是否存在
        Category existCategory = categoryMapper.selectById(category.getId());
        if (existCategory == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 检查分类名称是否已存在
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, category.getName())
                .ne(Category::getId, category.getId());
        if (categoryMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        
        // 验证分类层级限制（最多2级）
        validateCategoryLevel(category.getParentId());
        
        // 如果修改了父分类，需要检查是否会形成循环引用
        if (category.getParentId() != null && !category.getParentId().equals(existCategory.getParentId())) {
            validateCategoryCircularReference(category.getId(), category.getParentId());
        }
        
        categoryMapper.updateById(category);
    }
    
    /**
     * 删除分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        // 验证分类是否存在
        Category existCategory = categoryMapper.selectById(id);
        if (existCategory == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 检查分类及其子分类是否有关联文章
        checkCategoryAndChildrenForArticles(id);
        
        // 删除分类及其所有子分类
        deleteCategoryAndChildren(id);
    }
    
    /**
     * 检查分类及其子分类是否有关联文章
     */
    private void checkCategoryAndChildrenForArticles(Long categoryId) {
        // 检查当前分类是否有文章
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getCategoryId, categoryId);
        if (articleMapper.selectCount(articleWrapper) > 0) {
            throw new BusinessException("此分类下有文章，无法删除");
        }
        
        // 获取所有子分类
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(Category::getParentId, categoryId);
        List<Category> childCategories = categoryMapper.selectList(categoryWrapper);
        
        // 递归检查每个子分类是否有文章
        for (Category childCategory : childCategories) {
            checkCategoryAndChildrenForArticles(childCategory.getId());
        }
    }
    
    /**
     * 删除分类及其所有子分类
     */
    private void deleteCategoryAndChildren(Long categoryId) {
        // 获取所有子分类
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(Category::getParentId, categoryId);
        List<Category> childCategories = categoryMapper.selectList(categoryWrapper);
        
        // 递归删除所有子分类
        for (Category childCategory : childCategories) {
            deleteCategoryAndChildren(childCategory.getId());
        }
        
        // 删除当前分类
        categoryMapper.deleteById(categoryId);
    }
    
    /**
     * 设置分类的文章数量
     */
    private void setArticleCount(Category category) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getCategoryId, category.getId())
                .eq(Article::getStatus, 1); // 只统计已发布的文章
        
        Integer count = Math.toIntExact(articleMapper.selectCount(queryWrapper));
        category.setArticleCount(count);
    }
    
    /**
     * 构建分类树形结构
     */
    private List<Category> buildCategoryTree(List<Category> allCategories) {
        List<Category> result = new ArrayList<>();
        
        // 按父ID分组
        Map<Long, List<Category>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        
        // 获取顶级分类
        List<Category> rootCategories = parentMap.getOrDefault(0L, new ArrayList<>());
        
        // 排序
        rootCategories.sort(Comparator.comparing(Category::getOrderNum));
        
        // 设置子分类
        for (Category rootCategory : rootCategories) {
            rootCategory.setChildren(getChildCategories(rootCategory.getId(), parentMap));
            result.add(rootCategory);
        }
        
        return result;
    }
    
    /**
     * 递归获取子分类
     */
    private List<Category> getChildCategories(Long parentId, Map<Long, List<Category>> parentMap) {
        List<Category> children = parentMap.getOrDefault(parentId, new ArrayList<>());
        
        // 排序
        children.sort(Comparator.comparing(Category::getOrderNum));
        
        // 递归设置子分类的子分类
        for (Category child : children) {
            child.setChildren(getChildCategories(child.getId(), parentMap));
        }
        
        return children;
    }
    
    /**
     * 递归计算分类及其所有子分类的文章数量总和
     */
    private int calculateTotalArticleCount(Long categoryId, List<Category> allCategories) {
        int totalCount = 0;
        
        // 计算当前分类的文章数量
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getCategoryId, categoryId)
                .eq(Article::getStatus, 1); // 只统计已发布的文章
        
        totalCount += Math.toIntExact(articleMapper.selectCount(queryWrapper));
        
        // 递归计算所有子分类的文章数量
        List<Category> children = allCategories.stream()
                .filter(category -> category.getParentId() != null && category.getParentId().equals(categoryId))
                .collect(Collectors.toList());
        
        for (Category child : children) {
            totalCount += calculateTotalArticleCount(child.getId(), allCategories);
        }
        
        return totalCount;
    }

    /**
     * 在已构建的树上，累加父级包含子级的文章数量（仅内存处理，不额外查询数据库）。
     */
    private int accumulateArticleCount(Category node) {
        int self = node.getArticleCount() == null ? 0 : node.getArticleCount();
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            node.setArticleCount(self);
            return self;
        }
        int sum = self;
        for (Category child : node.getChildren()) {
            sum += accumulateArticleCount(child);
        }
        node.setArticleCount(sum);
        return sum;
    }
    
    /**
     * 验证分类层级限制（最多2级）
     * @param parentId 父分类ID
     */
    private void validateCategoryLevel(Long parentId) {
        if (parentId == null || parentId == 0L) {
            // 顶级分类，无需验证
            return;
        }
        
        // 查询父分类
        Category parentCategory = categoryMapper.selectById(parentId);
        if (parentCategory == null) {
            throw new BusinessException("父分类不存在");
        }
        
        // 检查父分类是否已经是二级分类（即父分类的parentId不为0）
        if (parentCategory.getParentId() != null && parentCategory.getParentId() != 0L) {
            throw new BusinessException("不能在二级分类下创建子分类，系统最多支持2级分类");
        }
    }
    
    /**
     * 验证分类循环引用
     * @param categoryId 当前分类ID
     * @param parentId 新的父分类ID
     */
    private void validateCategoryCircularReference(Long categoryId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            // 设置为顶级分类，无循环引用风险
            return;
        }
        
        // 检查是否将分类设置为自己的子分类的父分类（形成循环）
        if (isDescendant(categoryId, parentId)) {
            throw new BusinessException("不能将分类设置为其子分类的父分类，这会形成循环引用");
        }
    }
    
    /**
     * 检查targetId是否是ancestorId的后代
     * @param ancestorId 祖先分类ID
     * @param targetId 目标分类ID
     * @return 是否为后代关系
     */
    private boolean isDescendant(Long ancestorId, Long targetId) {
        if (targetId == null || targetId == 0L) {
            return false;
        }
        
        Category targetCategory = categoryMapper.selectById(targetId);
        if (targetCategory == null) {
            return false;
        }
        
        // 如果目标分类的父分类就是祖先分类，则存在后代关系
        if (ancestorId.equals(targetCategory.getParentId())) {
            return true;
        }
        
        // 递归检查目标分类的父分类
        return isDescendant(ancestorId, targetCategory.getParentId());
    }
} 