package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.FriendLink;

@Mapper
public interface FriendLinkMapper extends BaseMapper<FriendLink> {
    // 无需添加任何方法，使用MyBatis-Plus提供的基础方法即可
} 