package org.example.springboot.service.convert;

import org.example.springboot.dto.DashboardStatsResponseDTO;
import org.example.springboot.dto.DashboardTrendResponseDTO;

import java.util.Map;

/**
 * Dashboard数据转换器
 */
public class DashboardConvert {
    
    /**
     * Map转换为仪表盘统计响应DTO
     */
    public static DashboardStatsResponseDTO mapToStatsResponseDTO(Map<String, Object> statsMap) {
        DashboardStatsResponseDTO dto = new DashboardStatsResponseDTO();
        dto.setArticleCount((Long) statsMap.get("articleCount"));
        dto.setCommentCount((Long) statsMap.get("commentCount"));
        dto.setUserCount((Long) statsMap.get("userCount"));
        dto.setViewCount((Long) statsMap.get("viewCount"));
        return dto;
    }
    
    /**
     * Map转换为趋势响应DTO
     */
    public static DashboardTrendResponseDTO mapToTrendResponseDTO(Map<String, Object> trendMap) {
        DashboardTrendResponseDTO dto = new DashboardTrendResponseDTO();
        dto.setDate((String) trendMap.get("date"));
        dto.setCount(((Number) trendMap.get("count")).longValue());
        return dto;
    }
}
