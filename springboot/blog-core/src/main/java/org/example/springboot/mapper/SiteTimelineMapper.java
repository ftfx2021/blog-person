package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.SiteTimeline;

/**
 * 网站发展历程Mapper接口
 */
@Mapper
public interface SiteTimelineMapper extends BaseMapper<SiteTimeline> {
}
