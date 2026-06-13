package org.example.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 搜索结果处理工具类
 * 用于处理高亮、字段折叠等复杂结果
 */
@Slf4j
public class EsResultUtil {

    /**
     * 处理搜索结果（含高亮）
     * 
     * @param searchHits ES 搜索结果
     * @param highlightFields 需要高亮的字段名
     * @param <T> 实体类型
     * @return 处理后的实体列表（高亮已替换）
     */
    public static <T> List<T> extractWithHighlight(SearchHits<T> searchHits, String... highlightFields) {
        return searchHits.stream()
            .map(hit -> processHighlight(hit, highlightFields))
            .collect(Collectors.toList());
    }

    /**
     * 处理搜索结果（含高亮 + innerHits）
     * 
     * @param searchHits ES 搜索结果
     * @param innerHitsName innerHits 的名称（如 "other_type"）
     * @param highlightFields 需要高亮的字段名
     * @param <T> 实体类型
     * @return 包含主结果和附加数据的 Map 列表
     */
    public static <T> List<Map<String, Object>> extractWithInnerHits(
            SearchHits<T> searchHits, 
            String innerHitsName,
            String... highlightFields) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SearchHit<T> hit : searchHits) {
            Map<String, Object> item = new HashMap<>();
            
            // 1. 主结果（带高亮）
            T mainData = processHighlight(hit, highlightFields);
            item.put("main", mainData);
            
            // 2. innerHits（折叠的其他数据，也需要处理高亮）
            Map<String, SearchHits<?>> innerHitsMap = hit.getInnerHits();
            if (innerHitsMap != null && innerHitsMap.containsKey(innerHitsName)) {
                SearchHits<?> innerHits = innerHitsMap.get(innerHitsName);
                

                List<?> innerList = innerHits.stream()
                    .map(innerHit -> processHighlight(innerHit, highlightFields))
                    .collect(Collectors.toList());
                
                item.put("innerHitsName", innerHitsName);  //
                item.put("innerHits", innerList);
                item.put("innerHitsTotal", innerHits.getTotalHits());
            } else {
                item.put("innerHitsName", innerHitsName);
                item.put("innerHits", Collections.emptyList());
                item.put("innerHitsTotal", 0L);
            }
            
            result.add(item);
        }
        
        return result;
    }

    /**
     * 提取高亮字段并替换到实体对象
     * 
     * @param hit 单个搜索命中结果
     * @param highlightFields 需要高亮的字段名
     * @param <T> 实体类型
     * @return 处理后的实体对象
     */
    public static <T> T processHighlight(SearchHit<T> hit, String... highlightFields) {
        T content = hit.getContent();
        
        if (highlightFields == null || highlightFields.length == 0) {
            return content;
        }
        
        for (String fieldName : highlightFields) {
            List<String> highlightValues = hit.getHighlightField(fieldName);
            
            if (highlightValues != null && !highlightValues.isEmpty()) {
                String highlightValue = highlightValues.get(0);
                setFieldValue(content, fieldName, highlightValue);
            }
        }
        
        return content;
    }

    /**
     * 构建分页结果（含高亮）
     * 
     * @param searchHits ES 搜索结果
     * @param highlightFields 需要高亮的字段名
     * @param <T> 实体类型
     * @return 分页数据 Map
     */
    public static <T> Map<String, Object> buildPageResultWithHighlight(SearchHits<T> searchHits, String... highlightFields) {
        Map<String, Object> result = new HashMap<>();
        
        List<T> records = extractWithHighlight(searchHits, highlightFields);
        
        result.put("records", records);
        result.put("total", searchHits.getTotalHits());
        result.put("size", records.size());
        
        return result;
    }

    /**
     * 构建分页结果（含高亮 + innerHits）
     * 
     * @param searchHits ES 搜索结果
     * @param innerHitsName innerHits 的名称
     * @param highlightFields 需要高亮的字段名
     * @param <T> 实体类型
     * @return 分页数据 Map
     */
    public static <T> Map<String, Object> buildPageResultWithHighlightAndInnerHits(
            SearchHits<T> searchHits,
            String innerHitsName,
            String... highlightFields) {
        
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> records = extractWithInnerHits(searchHits, innerHitsName, highlightFields);
        
        result.put("records", records);
        result.put("total", searchHits.getTotalHits());
        result.put("size", records.size());
        
        return result;
    }

    /**
     * 提取聚合结果
     *
     * @param searchHits ES 搜索结果
     * @param aggregationName 聚合名称
     * @return 聚合结果对象
     */
    public static Object extractAggregation(SearchHits<?> searchHits, String aggregationName) {
        if (searchHits.getAggregations() == null) {
            return null;
        }
        return searchHits.getAggregations().aggregations();
    }

    /**
     * 使用反射设置字段值
     * 
     * @param obj 对象
     * @param fieldName 字段名
     * @param value 值
     */
    private static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(obj, value);
            }
        } catch (Exception e) {
            log.warn("设置字段 {} 的值失败: {}", fieldName, e.getMessage());
        }
    }

    /**
     * 递归查找字段（支持父类）
     * 
     * @param clazz 类
     * @param fieldName 字段名
     * @return 字段对象
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
