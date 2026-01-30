package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.LetterTemplate;
import org.apache.ibatis.annotations.Mapper;

/** 出函模板Mapper */
@Mapper
public interface LetterTemplateMapper extends BaseMapper<LetterTemplate> {}
