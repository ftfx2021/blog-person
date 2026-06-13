package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.DownloadRecord;

/**
 * 下载记录Mapper接口
 */
@Mapper
public interface DownloadRecordMapper extends BaseMapper<DownloadRecord> {
}
