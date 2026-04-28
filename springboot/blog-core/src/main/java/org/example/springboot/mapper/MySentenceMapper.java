package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.MySentence;

/**
 * 我的句子 Mapper 接口
 */
@Mapper
public interface MySentenceMapper extends BaseMapper<MySentence> {
}
