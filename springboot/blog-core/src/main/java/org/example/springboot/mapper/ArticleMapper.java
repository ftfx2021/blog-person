package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    // 使用MyBatis-Plus提供的基础方法即可
    // 总访问量统计应该在Service层通过LambdaQueryWrapper实现
} 