package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.DashboardStatsResponseDTO;
import org.example.springboot.dto.DashboardTrendResponseDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.convert.DashboardConvert;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 获取仪表盘统计数据
     * @return 统计数据DTO
     */
    public DashboardStatsResponseDTO getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取文章总数
        Long articleCount = articleMapper.selectCount(null);
        stats.put("articleCount", articleCount);
        
        // 获取评论总数
        Long commentCount = commentMapper.selectCount(null);
        stats.put("commentCount", commentCount);
        
        // 获取用户总数
        Long userCount = userMapper.selectCount(null);
        stats.put("userCount", userCount);
        
        // 获取总访问量（所有文章的访问量总和）
        Long viewCount = getTotalViewCount();
        stats.put("viewCount", viewCount);
        
        return DashboardConvert.mapToStatsResponseDTO(stats);
    }

    /**
     * 使用MyBatis-Plus计算总访问量
     */
    private Long getTotalViewCount() {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Article::getViewCount);
        List<Article> articles = articleMapper.selectList(queryWrapper);
        
        return articles.stream()
                .mapToLong(article -> article.getViewCount() != null ? article.getViewCount() : 0L)
                .sum();
    }
    
    /**
     * 获取文章发布趋势数据
     * @param days 天数
     * @return 趋势数据列表
     */
    public List<DashboardTrendResponseDTO> getArticleTrend(Integer days) {
        List<DashboardTrendResponseDTO> result = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 生成日期范围
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            
            // 使用MyBatis-Plus查询该日期发布的文章数量
            LocalDateTime startDateTime = date.atStartOfDay();
            LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
            
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ge(Article::getCreateTime, startDateTime)
                       .lt(Article::getCreateTime, endDateTime);
            
            Long count = articleMapper.selectCount(queryWrapper);
            
            DashboardTrendResponseDTO item = new DashboardTrendResponseDTO();
            item.setDate(dateStr);
            item.setCount(count != null ? count : 0L);
            
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * 获取访问量趋势数据
     * 注意：当前系统暂未实现访问日志记录功能，返回空数据
     * @param days 天数
     * @return 趋势数据列表
     */
    public List<DashboardTrendResponseDTO> getViewTrend(Integer days) {
        List<DashboardTrendResponseDTO> result = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 生成日期范围，但访问量统计为0（需要实现访问日志记录功能）
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            
            DashboardTrendResponseDTO item = new DashboardTrendResponseDTO();
            item.setDate(dateStr);
            item.setCount(0L); // 暂时返回0，待实现访问日志记录功能
            
            result.add(item);
        }
        
        return result;
    }
} 