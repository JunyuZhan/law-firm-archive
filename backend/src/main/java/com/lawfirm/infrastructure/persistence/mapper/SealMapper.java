package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.document.entity.Seal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 印章Mapper */
@Mapper
public interface SealMapper extends BaseMapper<Seal> {

  /**
   * 分页查询印章.
   *
   * @param page 分页参数
   * @param name 印章名称
   * @param sealType 印章类型
   * @param keeperId 保管人ID
   * @param status 状态
   * @return 印章分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM seal_info WHERE deleted = false "
          + "<if test='name != null and name != \"\"'> AND name LIKE CONCAT('%', #{name}, '%') </if>"
          + "<if test='sealType != null and sealType != \"\"'> AND seal_type = #{sealType} </if>"
          + "<if test='keeperId != null'> AND keeper_id = #{keeperId} </if>"
          + "<if test='status != null and status != \"\"'> AND status = #{status} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<Seal> selectSealPage(
      Page<Seal> page,
      @Param("name") String name,
      @Param("sealType") String sealType,
      @Param("keeperId") Long keeperId,
      @Param("status") String status);
}
