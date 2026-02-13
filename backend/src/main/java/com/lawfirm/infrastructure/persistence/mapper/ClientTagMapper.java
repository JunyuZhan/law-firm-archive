package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ClientTag;
import org.apache.ibatis.annotations.Mapper;

/** 客户标签Mapper */
@Mapper
public interface ClientTagMapper extends BaseMapper<ClientTag> {}
