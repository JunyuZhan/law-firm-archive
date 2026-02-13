package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.Asset;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 资产 Mapper */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {

  /**
   * 分页查询资产.
   *
   * @param page 分页参数
   * @param keyword 关键词
   * @param category 分类
   * @param status 状态
   * @param departmentId 部门ID
   * @param currentUserId 当前使用人ID
   * @return 资产分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM admin_asset WHERE deleted = false "
          + "<if test='keyword != null and keyword != \"\"'>"
          + "AND (name LIKE CONCAT('%', #{keyword}, '%') OR asset_no LIKE CONCAT('%', #{keyword}, '%')) "
          + "</if>"
          + "<if test='category != null and category != \"\"'> AND category = #{category} </if>"
          + "<if test='status != null and status != \"\"'> AND status = #{status} </if>"
          + "<if test='departmentId != null'> AND department_id = #{departmentId} </if>"
          + "<if test='currentUserId != null'> AND current_user_id = #{currentUserId} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<Asset> selectAssetPage(
      Page<Asset> page,
      @Param("keyword") String keyword,
      @Param("category") String category,
      @Param("status") String status,
      @Param("departmentId") Long departmentId,
      @Param("currentUserId") Long currentUserId);

  /**
   * 根据资产编号查询.
   *
   * @param assetNo 资产编号
   * @return 资产实体
   */
  @Select("SELECT * FROM admin_asset WHERE asset_no = #{assetNo} AND deleted = false")
  Asset selectByAssetNo(@Param("assetNo") String assetNo);

  /**
   * 查询闲置资产.
   *
   * @return 闲置资产列表
   */
  @Select("SELECT * FROM admin_asset WHERE status = 'IDLE' AND deleted = false ORDER BY name")
  List<Asset> selectIdleAssets();

  /**
   * 查询用户领用的资产.
   *
   * @param userId 用户ID
   * @return 用户领用的资产列表
   */
  @Select(
      "SELECT * FROM admin_asset WHERE current_user_id = #{userId} AND status = 'IN_USE' AND deleted = false")
  List<Asset> selectByCurrentUserId(@Param("userId") Long userId);

  /**
   * 统计各状态资产数量.
   *
   * @return 各状态资产数量统计
   */
  @Select("SELECT status, COUNT(*) as count FROM admin_asset WHERE deleted = false GROUP BY status")
  List<java.util.Map<String, Object>> countByStatus();

  /**
   * 统计各分类资产数量.
   *
   * @return 各分类资产数量统计
   */
  @Select(
      "SELECT category, COUNT(*) as count FROM admin_asset WHERE deleted = false GROUP BY category")
  List<java.util.Map<String, Object>> countByCategory();
}
