package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.clientservice.entity.ClientAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户访问日志Mapper
 */
@Mapper
public interface ClientAccessLogMapper extends BaseMapper<ClientAccessLog> {
}
