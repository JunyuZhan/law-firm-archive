package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.Migration;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 数据库迁移Mapper */
@Mapper
public interface MigrationMapper extends BaseMapper<Migration> {

  /**
   * 根据版本号查询.
   *
   * @param version 版本号
   * @return 数据库迁移记录
   */
  @Select("SELECT * FROM sys_migration WHERE schema_version = #{version} AND deleted = false")
  Migration selectByVersion(@Param("version") String version);

  /**
   * 批量根据版本号查询（避免N+1查询）.
   *
   * @param versions 版本号列表
   * @return 数据库迁移记录列表
   */
  @Select(
      "<script>"
          + "SELECT * FROM sys_migration WHERE schema_version IN "
          + "<foreach collection='versions' item='version' open='(' separator=',' close=')'>"
          + "#{version}"
          + "</foreach>"
          + " AND deleted = false"
          + "</script>")
  List<Migration> selectByVersions(@Param("versions") List<String> versions);

  /**
   * 查询所有已执行的迁移（按版本号排序）.
   *
   * @return 已执行的迁移列表
   */
  @Select(
      "SELECT * FROM sys_migration WHERE status = 'SUCCESS' AND deleted = false ORDER BY schema_version")
  List<Migration> selectExecutedMigrations();

  /**
   * 查询所有迁移记录（按创建时间排序）.
   *
   * @return 所有迁移记录列表
   */
  @Select("SELECT * FROM sys_migration WHERE deleted = false ORDER BY created_at DESC")
  List<Migration> selectAllMigrations();

  /**
   * 分页查询迁移记录.
   *
   * @param page 分页参数
   * @return 迁移记录分页结果
   */
  @Select(
      "SELECT * FROM sys_migration WHERE deleted = false ORDER BY executed_at DESC, created_at DESC")
  IPage<Migration> selectMigrationPage(Page<Migration> page);
}
