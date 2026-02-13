package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import org.apache.ibatis.annotations.Mapper;

/** 档案库位 Mapper */
@Mapper
public interface ArchiveLocationMapper extends BaseMapper<ArchiveLocation> {}
