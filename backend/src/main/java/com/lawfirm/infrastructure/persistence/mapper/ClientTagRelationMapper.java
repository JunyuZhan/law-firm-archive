package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ClientTagRelation;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 客户标签关联Mapper */
@Mapper
public interface ClientTagRelationMapper extends BaseMapper<ClientTagRelation> {

  /**
   * 根据客户ID查询标签ID列表.
   *
   * @param clientId 客户ID
   * @return 标签ID列表
   */
  @Select("SELECT tag_id FROM crm_client_tag_relation WHERE client_id = #{clientId}")
  List<Long> selectTagIdsByClientId(@Param("clientId") Long clientId);

  /**
   * 根据标签ID查询客户ID列表.
   *
   * @param tagId 标签ID
   * @return 客户ID列表
   */
  @Select("SELECT client_id FROM crm_client_tag_relation WHERE tag_id = #{tagId}")
  List<Long> selectClientIdsByTagId(@Param("tagId") Long tagId);

  /**
   * 删除客户的所有标签.
   *
   * @param clientId 客户ID
   */
  @Delete("DELETE FROM crm_client_tag_relation WHERE client_id = #{clientId}")
  void deleteByClientId(@Param("clientId") Long clientId);

  /**
   * 删除标签的所有关联.
   *
   * @param tagId 标签ID
   */
  @Delete("DELETE FROM crm_client_tag_relation WHERE tag_id = #{tagId}")
  void deleteByTagId(@Param("tagId") Long tagId);
}
