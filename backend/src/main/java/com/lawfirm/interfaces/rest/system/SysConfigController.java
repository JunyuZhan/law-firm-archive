package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.finance.service.ContractNumberGenerator;
import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.VersionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 系统配置接口 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Slf4j
public class SysConfigController {

  /** 系统配置应用服务 */
  private final SysConfigAppService configAppService;

  /** 合同编号生成器 */
  private final ContractNumberGenerator contractNumberGenerator;

  /** 环境配置 */
  private final Environment environment;

  /**
   * 获取所有配置
   *
   * @param keyPrefix 配置键前缀
   * @return 配置列表
   */
  @Operation(summary = "获取所有配置")
  @GetMapping
  @RequirePermission("sys:config:list")
  public Result<List<SysConfigDTO>> listConfigs(
      @RequestParam(required = false) final String keyPrefix) {
    if (keyPrefix != null && !keyPrefix.isEmpty()) {
      return Result.success(configAppService.listConfigsByPrefix(keyPrefix));
    }
    return Result.success(configAppService.listConfigs());
  }

  /**
   * 根据键获取配置
   *
   * @param key 配置键
   * @return 配置详情
   */
  @Operation(summary = "根据键获取配置")
  @GetMapping("/key/{key}")
  @RequirePermission("sys:config:list")
  public Result<SysConfigDTO> getConfigByKey(@PathVariable final String key) {
    return Result.success(configAppService.getConfigByKey(key));
  }

  /**
   * 批量获取配置
   *
   * @param keys 配置键列表
   * @return 配置映射
   */
  @Operation(summary = "批量获取配置")
  @PostMapping("/batch")
  @RequirePermission("sys:config:list")
  public Result<Map<String, String>> getConfigBatch(@RequestBody final List<String> keys) {
    return Result.success(configAppService.getConfigMap(keys));
  }

  /**
   * 创建配置
   *
   * @param command 创建命令
   * @return 配置详情
   */
  @Operation(summary = "创建配置")
  @PostMapping
  @RequirePermission("sys:config:create")
  @OperationLog(module = "系统配置", action = "创建配置")
  public Result<SysConfigDTO> createConfig(@Valid @RequestBody final UpdateConfigCommand command) {
    return Result.success(configAppService.createConfig(command));
  }

  /**
   * 更新配置
   *
   * @param id 配置ID
   * @param command 更新命令
   * @return 无返回
   */
  @Operation(summary = "更新配置")
  @PutMapping("/{id}")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "系统配置", action = "更新配置")
  public Result<Void> updateConfig(
      @PathVariable final Long id, @Valid @RequestBody final UpdateConfigCommand command) {
    command.setId(id);
    configAppService.updateConfig(command);
    return Result.success();
  }

  /**
   * 根据键更新配置值
   *
   * @param key 配置键
   * @param body 配置值
   * @return 无返回
   */
  @Operation(summary = "根据键更新配置值")
  @PutMapping("/key/{key}")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "系统配置", action = "更新配置值")
  public Result<Void> updateConfigByKey(
      @PathVariable final String key, @RequestBody final Map<String, String> body) {
    String value = body.get("value");
    if (value == null) {
      // 兼容直接传字符串的情况
      value = body.toString();
    }
    configAppService.updateConfigByKey(key, value);
    return Result.success();
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   * @return 无返回
   */
  @Operation(summary = "删除配置")
  @DeleteMapping("/{id}")
  @RequirePermission("sys:config:delete")
  @OperationLog(module = "系统配置", action = "删除配置")
  public Result<Void> deleteConfig(@PathVariable final Long id) {
    configAppService.deleteConfig(id);
    return Result.success();
  }

  // ============ 合同编号配置相关接口 ============

  /**
   * 预览合同编号规则
   *
   * @param params 预览参数
   * @return 预览结果
   */
  @Operation(summary = "预览合同编号规则")
  @PostMapping("/contract-number/preview")
  @RequirePermission("sys:config:list")
  public Result<List<Map<String, String>>> previewContractNumber(
      @RequestBody final Map<String, Object> params) {
    String pattern = (String) params.get("pattern");
    String prefix = (String) params.get("prefix");
    Integer sequenceLength =
        params.get("sequenceLength") != null
            ? Integer.valueOf(params.get("sequenceLength").toString())
            : null;
    String caseType = (String) params.get("caseType");
    String feeType = (String) params.get("feeType");

    List<Map<String, String>> previews =
        contractNumberGenerator.previewPattern(pattern, prefix, sequenceLength, caseType, feeType);
    return Result.success(previews);
  }

  /**
   * 获取合同编号支持的变量
   *
   * @return 变量列表
   */
  @Operation(summary = "获取合同编号支持的变量")
  @GetMapping("/contract-number/variables")
  public Result<List<Map<String, String>>> getContractNumberVariables() {
    return Result.success(contractNumberGenerator.getSupportedVariables());
  }

  /**
   * 获取推荐的合同编号规则模板
   *
   * @return 模板列表
   */
  @Operation(summary = "获取推荐的合同编号规则模板")
  @GetMapping("/contract-number/patterns")
  public Result<List<Map<String, String>>> getRecommendedPatterns() {
    return Result.success(contractNumberGenerator.getRecommendedPatterns());
  }

  /**
   * 获取案件类型选项
   *
   * @return 选项列表
   */
  @Operation(summary = "获取案件类型选项")
  @GetMapping("/contract-number/case-types")
  public Result<List<Map<String, String>>> getCaseTypeOptions() {
    return Result.success(contractNumberGenerator.getCaseTypeOptions());
  }

  // ============ 系统维护模式相关接口 ============

  /**
   * 获取维护模式状态
   *
   * @return 状态信息
   */
  @Operation(summary = "获取维护模式状态")
  @GetMapping("/maintenance/status")
  @RequirePermission("sys:config:list")
  public Result<Map<String, Object>> getMaintenanceStatus() {
    Map<String, Object> status = new java.util.HashMap<>();
    String enabled = configAppService.getConfigValue("sys.maintenance.enabled");
    String message = configAppService.getConfigValue("sys.maintenance.message");
    status.put("enabled", "true".equalsIgnoreCase(enabled));
    status.put("message", message != null ? message : "系统正在维护中，请稍后再试");
    return Result.success(status);
  }

  /**
   * 开启维护模式
   *
   * @param params 维护信息
   * @return 无返回
   */
  @Operation(summary = "开启维护模式")
  @PostMapping("/maintenance/enable")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "系统维护", action = "开启维护模式")
  public Result<Void> enableMaintenanceMode(
      @RequestBody(required = false) final Map<String, String> params) {
    String message = params != null ? params.get("message") : null;
    if (message == null || message.isEmpty()) {
      message = "系统正在维护中，预计维护时间：30分钟，请稍后再试";
    }
    configAppService.updateConfigByKey("sys.maintenance.enabled", "true");
    configAppService.updateConfigByKey("sys.maintenance.message", message);
    return Result.success();
  }

  /**
   * 关闭维护模式
   *
   * @return 无返回
   */
  @Operation(summary = "关闭维护模式")
  @PostMapping("/maintenance/disable")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "系统维护", action = "关闭维护模式")
  public Result<Void> disableMaintenanceMode() {
    configAppService.updateConfigByKey("sys.maintenance.enabled", "false");
    return Result.success();
  }

  // ============ 前端公共配置接口（无需认证） ============

  /**
   * 获取前端公共配置（无需登录） 用于获取 ICP 备案号、系统名称等公开信息
   *
   * @return 公共配置
   */
  @Operation(summary = "获取前端公共配置")
  @GetMapping("/public")
  public Result<Map<String, String>> getPublicConfig() {
    Map<String, String> config = new HashMap<>();
    // 系统名称
    config.put("name", configAppService.getConfigValue("sys.name"));
    // 版权信息
    config.put("copyright", configAppService.getConfigValue("sys.copyright"));
    // ICP 备案号
    config.put("icp", configAppService.getConfigValue("sys.icp"));
    // ICP 链接
    config.put("icpLink", configAppService.getConfigValue("sys.icpLink"));
    return Result.success(config);
  }

  // ============ 系统版本信息相关接口 ============

  /**
   * 获取系统版本信息
   *
   * @return 版本信息
   */
  @Operation(summary = "获取系统版本信息")
  @GetMapping("/version")
  public Result<Map<String, Object>> getVersionInfo() {
    VersionUtils.VersionInfo versionInfo = VersionUtils.getVersionInfo();
    String[] activeProfiles = environment.getActiveProfiles();

    // 优先从数据库读取版本号（如果配置了 sys.version）
    // 这样可以支持更灵活的版本号格式，如 0.4、1.2 等简单数字格式
    String dbVersion = null;
    try {
      dbVersion = configAppService.getConfigValue("sys.version");
    } catch (Exception e) {
      // 忽略错误，使用构建时的版本号
      log.debug("读取数据库版本配置失败，将使用构建时版本号: {}", e.getMessage());
    }

    Map<String, Object> info = new HashMap<>();
    // 如果数据库中有版本号配置，优先使用数据库版本（支持简单格式如 0.4）
    // 否则使用构建时的版本号（如 1.0.0-SNAPSHOT）
    info.put(
        "version",
        dbVersion != null && !dbVersion.isEmpty() ? dbVersion : versionInfo.getVersion());
    info.put("buildVersion", versionInfo.getVersion()); // 保留构建版本号
    info.put("buildTime", versionInfo.getBuildTime());
    info.put("gitCommit", versionInfo.getGitCommit());
    info.put("profile", activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default");
    info.put("javaVersion", System.getProperty("java.version"));
    info.put("javaVendor", System.getProperty("java.vendor"));
    info.put("osName", System.getProperty("os.name"));
    info.put("osVersion", System.getProperty("os.version"));
    info.put(
        "serverTime",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

    return Result.success(info);
  }
}
