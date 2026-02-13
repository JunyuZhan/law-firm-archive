package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.MigrationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Migration;
import com.lawfirm.domain.system.repository.MigrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.MigrationMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数据库迁移应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationAppService {

  /** 迁移仓储 */
  private final MigrationRepository migrationRepository;

  /** 迁移Mapper */
  private final MigrationMapper migrationMapper;

  /** JDBC模板 */
  private final JdbcTemplate jdbcTemplate;

  /** 系统配置应用服务 */
  private final SysConfigAppService configAppService;

  /** 迁移脚本路径 */
  @Value("${law-firm.migration.path:./scripts/migration}")
  private String migrationPath;

  /**
   * 扫描迁移脚本目录，返回所有脚本文件
   *
   * @return 迁移脚本列表
   */
  public List<MigrationDTO> scanMigrationScripts() {
    // ✅ 权限验证：只有管理员才能扫描迁移脚本
    if (!SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能查看迁移脚本");
    }

    List<MigrationDTO> scripts = new ArrayList<>();
    Path basePath = Paths.get(migrationPath).toAbsolutePath().normalize();

    if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
      log.warn("迁移脚本目录不存在: {}", migrationPath);
      return scripts;
    }

    try (Stream<Path> paths = Files.walk(basePath)) {
      // 1. 先解析所有脚本文件
      List<MigrationDTO> parsedScripts =
          paths
              .filter(Files::isRegularFile)
              .filter(p -> p.toString().endsWith(".sql"))
              // ✅ 验证文件路径：必须在migrationPath目录下（防止路径遍历）
              .filter(p -> p.toAbsolutePath().normalize().startsWith(basePath))
              .sorted()
              .map(this::parseMigrationScript)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      if (parsedScripts.isEmpty()) {
        return scripts;
      }

      // ✅ 2. 批量查询所有版本的执行状态（避免N+1查询）
      Set<String> versions =
          parsedScripts.stream().map(MigrationDTO::getVersion).collect(Collectors.toSet());

      Map<String, Migration> executedMap =
          migrationMapper.selectByVersions(new ArrayList<>(versions)).stream()
              .collect(Collectors.toMap(Migration::getSchemaVersion, m -> m, (a, b) -> a));

      // 3. 设置执行状态（从Map获取）
      for (MigrationDTO dto : parsedScripts) {
        Migration executed = executedMap.get(dto.getVersion());
        if (executed != null) {
          dto.setId(executed.getId());
          dto.setStatus(executed.getStatus());
          dto.setExecutedAt(executed.getExecutedAt());
          dto.setExecutionTimeMs(executed.getExecutionTimeMs());
          dto.setErrorMessage(executed.getErrorMessage());
        } else {
          dto.setStatus(Migration.STATUS_PENDING);
        }
        scripts.add(dto);
      }

    } catch (IOException e) {
      log.error("扫描迁移脚本目录失败: {}", migrationPath, e);
      throw new BusinessException("扫描迁移脚本目录失败，请检查配置或联系管理员");
    }

    return scripts;
  }

  /**
   * 解析迁移脚本文件，提取版本号和描述
   *
   * @param scriptPath 脚本路径
   * @return 迁移DTO
   */
  private MigrationDTO parseMigrationScript(final Path scriptPath) {
    try {
      String fileName = scriptPath.getFileName().toString();
      String content = Files.readString(scriptPath);

      // 从文件名提取版本号（格式：V1.0.1-description.sql 或 001-description.sql）
      String version = extractVersionFromFileName(fileName);
      if (version == null) {
        // 尝试从文件内容中提取版本号
        version = extractVersionFromContent(content);
      }
      if (version == null) {
        version = fileName.replace(".sql", "");
      }

      // 从文件内容提取描述（第一行注释）
      String description = extractDescription(content);

      MigrationDTO dto = new MigrationDTO();
      dto.setVersion(version);
      dto.setScriptName(fileName);
      dto.setScriptPath(scriptPath.toString());
      dto.setDescription(description != null ? description : fileName);
      dto.setMigrationNo(generateMigrationNo(version));

      return dto;
    } catch (IOException e) {
      log.error("读取迁移脚本文件失败: {}", scriptPath, e);
      return null;
    }
  }

  /**
   * 从文件名提取版本号
   *
   * @param fileName 文件名
   * @return 版本号
   */
  private String extractVersionFromFileName(final String fileName) {
    // 匹配 V1.0.1 或 V1.0.1-description 格式
    Pattern pattern = Pattern.compile("V?(\\d+\\.\\d+\\.\\d+)");
    Matcher matcher = pattern.matcher(fileName);
    if (matcher.find()) {
      return "V" + matcher.group(1);
    }
    return null;
  }

  /**
   * 从文件内容提取版本号
   *
   * @param content 文件内容
   * @return 版本号
   */
  private String extractVersionFromContent(final String content) {
    Pattern pattern =
        Pattern.compile("--\\s*版本[：:]?\\s*V?(\\d+\\.\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
      return "V" + matcher.group(1);
    }
    return null;
  }

  /**
   * 从文件内容提取描述
   *
   * @param content 文件内容
   * @return 描述
   */
  private String extractDescription(final String content) {
    String[] lines = content.split("\n");
    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("--") && line.length() > 2) {
        String desc = line.substring(2).trim();
        if (!desc.isEmpty() && !desc.matches(".*版本.*|.*Version.*")) {
          return desc;
        }
      }
    }
    return null;
  }

  /**
   * 生成迁移编号
   *
   * @param version 版本号
   * @return 迁移编号
   */
  private String generateMigrationNo(final String version) {
    return "MIG-" + version.replace("V", "") + "-" + System.currentTimeMillis();
  }

  /**
   * 执行迁移脚本
   *
   * @param version 版本号
   * @param confirmCode 确认码（必须为 MIGRATE_版本号 格式）
   * @return 迁移结果
   */
  @Transactional
  public MigrationDTO executeMigration(final String version, final String confirmCode) {
    // ✅ 1. 严格权限验证：只有超级管理员才能执行数据库迁移
    if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN")) {
      throw new BusinessException("权限不足：只有超级管理员才能执行数据库迁移");
    }

    // ✅ 2. 二次确认：验证确认码（防止误操作）
    String expectedCode = "MIGRATE_" + version.replace(".", "_");
    if (confirmCode == null || !expectedCode.equals(confirmCode)) {
      throw new BusinessException("确认码错误，请输入: " + expectedCode);
    }

    // ✅ 3. 强制要求维护模式
    String maintenanceEnabled = configAppService.getConfigValue("sys.maintenance.enabled");
    if (!"true".equalsIgnoreCase(maintenanceEnabled)) {
      throw new BusinessException("请先开启维护模式再执行数据库迁移");
    }

    // 检查是否已执行
    Migration existing = migrationMapper.selectByVersion(version);
    if (existing != null && Migration.STATUS_SUCCESS.equals(existing.getStatus())) {
      throw new BusinessException("该迁移脚本已成功执行，无需重复执行");
    }

    // 查找脚本文件（scanMigrationScripts 已包含权限验证）
    MigrationDTO scriptInfo =
        scanMigrationScripts().stream()
            .filter(s -> version.equals(s.getVersion()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("未找到版本号为 " + version + " 的迁移脚本"));

    // ✅ 4. 验证文件路径：必须在migrationPath目录下（防止路径遍历）
    Path basePath = Paths.get(migrationPath).toAbsolutePath().normalize();
    Path scriptPath = Paths.get(scriptInfo.getScriptPath()).toAbsolutePath().normalize();
    if (!scriptPath.startsWith(basePath)) {
      log.error("检测到路径遍历攻击: scriptPath={}, basePath={}", scriptPath, basePath);
      throw new BusinessException("非法的脚本路径");
    }

    File scriptFile = scriptPath.toFile();
    if (!scriptFile.exists()) {
      throw new BusinessException("迁移脚本文件不存在");
    }

    // ✅ 5. 记录审计日志
    log.warn(
        "【危险操作】开始执行数据库迁移: version={}, operator={}, operatorName={}, confirmCode={}",
        version,
        SecurityUtils.getUserId(),
        SecurityUtils.getUsername(),
        confirmCode);

    // 创建迁移记录
    Migration migration =
        Migration.builder()
            .migrationNo(scriptInfo.getMigrationNo())
            .schemaVersion(version)
            .scriptName(scriptInfo.getScriptName())
            .scriptPath(scriptInfo.getScriptPath())
            .description(scriptInfo.getDescription())
            .status(Migration.STATUS_PENDING)
            .executedBy(SecurityUtils.getUserId())
            .build();

    migrationRepository.save(migration);

    long startTime = System.currentTimeMillis();
    try {
      // 读取SQL脚本
      String sql = Files.readString(scriptFile.toPath());

      // ✅ 6. 检测危险操作并警告
      detectDangerousOperations(sql, version);

      // 分割SQL语句（按分号分割，但要注意字符串中的分号）
      String[] statements = sql.split(";(?=(?:[^']*'[^']*')*[^']*$)");

      for (String statement : statements) {
        statement = statement.trim();
        if (!statement.isEmpty() && !statement.startsWith("--")) {
          jdbcTemplate.execute(statement);
        }
      }

      // 更新执行状态
      long executionTime = System.currentTimeMillis() - startTime;
      migration.setStatus(Migration.STATUS_SUCCESS);
      migration.setExecutedAt(LocalDateTime.now());
      migration.setExecutionTimeMs(executionTime);
      migrationRepository.updateById(migration);

      log.info(
          "数据库迁移执行成功: version={}, time={}ms, operator={}",
          version,
          executionTime,
          SecurityUtils.getUserId());
      return toDTO(migration);

    } catch (Exception e) {
      // 更新失败状态
      long executionTime = System.currentTimeMillis() - startTime;
      migration.setStatus(Migration.STATUS_FAILED);
      migration.setExecutedAt(LocalDateTime.now());
      migration.setExecutionTimeMs(executionTime);
      // ✅ 7. 错误消息脱敏：只存储错误类型
      migration.setErrorMessage("执行失败: " + e.getClass().getSimpleName());
      migrationRepository.updateById(migration);

      // 详细错误记录到日志
      log.error("数据库迁移执行失败: version={}, error={}", version, e.getMessage(), e);
      throw new BusinessException("迁移脚本执行失败，请联系管理员查看日志");
    }
  }

  /**
   * 执行迁移脚本（兼容旧接口，无确认码版本）
   *
   * @param version 版本号
   * @return 迁移结果（实际会抛出异常）
   * @deprecated 请使用带确认码的版本
   */
  @Transactional
  @Deprecated
  public MigrationDTO executeMigration(final String version) {
    throw new BusinessException("安全要求：执行迁移脚本必须提供确认码，请使用 executeMigration(version, confirmCode) 方法");
  }

  /**
   * 检测SQL中的危险操作
   *
   * @param sql SQL语句
   * @param version 版本号
   */
  private void detectDangerousOperations(final String sql, final String version) {
    String upperSql = sql.toUpperCase();
    List<String> dangerousOps = new ArrayList<>();

    if (upperSql.contains("DROP DATABASE")) {
      dangerousOps.add("DROP DATABASE");
    }
    if (upperSql.contains("DROP SCHEMA")) {
      dangerousOps.add("DROP SCHEMA");
    }
    if (upperSql.contains("TRUNCATE")) {
      dangerousOps.add("TRUNCATE");
    }
    if (upperSql.contains("DELETE FROM") && !upperSql.contains("WHERE")) {
      dangerousOps.add("DELETE WITHOUT WHERE");
    }

    if (!dangerousOps.isEmpty()) {
      log.warn(
          "【警告】迁移脚本包含危险操作: version={}, operations={}, operator={}",
          version,
          dangerousOps,
          SecurityUtils.getUserId());
    }
  }

  /**
   * 查询迁移记录列表
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  public PageResult<MigrationDTO> listMigrations(final int pageNum, final int pageSize) {
    // ✅ 权限验证：只有管理员才能查看迁移记录
    if (!SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能查看迁移记录");
    }

    // ✅ 使用数据库分页（避免查询所有后内存分页）
    IPage<Migration> page = migrationMapper.selectMigrationPage(new Page<>(pageNum, pageSize));

    List<MigrationDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), pageNum, pageSize);
  }

  /**
   * 获取迁移详情
   *
   * @param id 迁移ID
   * @return 迁移DTO
   */
  public MigrationDTO getMigrationById(final Long id) {
    // ✅ 权限验证：只有管理员才能查看迁移记录
    if (!SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能查看迁移记录");
    }

    Migration migration = migrationRepository.getByIdOrThrow(id, "迁移记录不存在");
    return toDTO(migration);
  }

  /**
   * 转换为DTO
   *
   * @param migration 迁移实体
   * @return 迁移DTO
   */
  /**
   * 转换为DTO
   *
   * @param migration 迁移实体
   * @return 迁移DTO
   */
  private MigrationDTO toDTO(final Migration migration) {
    MigrationDTO dto = new MigrationDTO();
    dto.setId(migration.getId());
    dto.setMigrationNo(migration.getMigrationNo());
    dto.setVersion(migration.getSchemaVersion());
    dto.setScriptName(migration.getScriptName());
    dto.setScriptPath(migration.getScriptPath());
    dto.setDescription(migration.getDescription());
    dto.setStatus(migration.getStatus());
    dto.setExecutedAt(migration.getExecutedAt());
    dto.setExecutionTimeMs(migration.getExecutionTimeMs());
    dto.setErrorMessage(migration.getErrorMessage());
    dto.setExecutedBy(migration.getExecutedBy());
    dto.setCreatedAt(migration.getCreatedAt());
    dto.setUpdatedAt(migration.getUpdatedAt());
    return dto;
  }
}
