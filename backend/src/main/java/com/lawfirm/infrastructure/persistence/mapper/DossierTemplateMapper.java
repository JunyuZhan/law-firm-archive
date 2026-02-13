package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DossierTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 卷宗目录模板 Mapper */
@Mapper
public interface DossierTemplateMapper extends BaseMapper<DossierTemplate> {

  /**
   * 根据案件类型查询默认模板.
   *
   * @param caseType 案件类型
   * @return 默认卷宗模板
   */
  @Select(
      "SELECT * FROM dossier_template WHERE case_type = #{caseType} AND is_default = true AND deleted = false LIMIT 1")
  DossierTemplate selectDefaultByCaseType(@Param("caseType") String caseType);

  /**
   * 查询所有模板.
   *
   * @return 所有卷宗模板列表
   */
  @Select("SELECT * FROM dossier_template WHERE deleted = false ORDER BY case_type, name")
  List<DossierTemplate> selectAllTemplates();
}
