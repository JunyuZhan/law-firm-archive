package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.Supplier;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 供应商Mapper */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

  /**
   * 分页查询供应商.
   *
   * @param page 分页对象
   * @param keyword 关键词
   * @param supplierType 供应商类型
   * @param status 状态
   * @param rating 评级
   * @return 供应商分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM admin_supplier WHERE deleted = false "
          + "<if test='keyword != null and keyword != \"\"'>"
          + "AND (name LIKE CONCAT('%', #{keyword}, '%') "
          + "OR supplier_no LIKE CONCAT('%', #{keyword}, '%') "
          + "OR contact_person LIKE CONCAT('%', #{keyword}, '%')) "
          + "</if>"
          + "<if test='supplierType != null and supplierType != \"\"'>"
          + "AND supplier_type = #{supplierType} "
          + "</if>"
          + "<if test='status != null and status != \"\"'>"
          + "AND status = #{status} "
          + "</if>"
          + "<if test='rating != null and rating != \"\"'>"
          + "AND rating = #{rating} "
          + "</if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<Supplier> findPage(
      Page<Supplier> page,
      @Param("keyword") String keyword,
      @Param("supplierType") String supplierType,
      @Param("status") String status,
      @Param("rating") String rating);

  /**
   * 按状态统计.
   *
   * @return 按状态统计结果
   */
  @Select(
      "SELECT status, COUNT(*) as count FROM admin_supplier WHERE deleted = false GROUP BY status")
  List<Map<String, Object>> countByStatus();

  /**
   * 按评级统计.
   *
   * @return 按评级统计结果
   */
  @Select(
      "SELECT rating, COUNT(*) as count FROM admin_supplier WHERE deleted = false GROUP BY rating")
  List<Map<String, Object>> countByRating();
}
