package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.clientservice.entity.ClientDownloadLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户下载日志Mapper
 */
@Mapper
public interface ClientDownloadLogMapper extends BaseMapper<ClientDownloadLog> {
}
