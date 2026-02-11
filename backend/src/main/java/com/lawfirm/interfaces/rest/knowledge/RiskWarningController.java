package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateRiskWarningCommand;
import com.lawfirm.application.knowledge.dto.RiskWarningDTO;
import com.lawfirm.application.knowledge.service.RiskWarningAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 风险预警接口（M10-033） */
@Tag(name = "风险预警", description = "项目风险预警相关接口")
@RestController
@RequestMapping("/knowledge/risk-warning")
@RequiredArgsConstructor
public class RiskWarningController {

  /** 风险预警应用服务. */
  private final RiskWarningAppService warningAppService;

  /**
   * 创建风险预警
   *
   * @param command 创建命令
   * @return 创建的预警
   */
  @Operation(summary = "创建风险预警")
  @PostMapping
  @RequirePermission("knowledge:quality:create")
  @OperationLog(module = "质量管理", action = "创建风险预警")
  public Result<RiskWarningDTO> createWarning(@RequestBody final CreateRiskWarningCommand command) {
    return Result.success(warningAppService.createWarning(command));
  }

  /**
   * 确认预警
   *
   * @param id 预警ID
   * @return 确认后的预警
   */
  @Operation(summary = "确认预警")
  @PostMapping("/{id}/acknowledge")
  @RequirePermission("knowledge:quality:update")
  @OperationLog(module = "质量管理", action = "确认风险预警")
  public Result<RiskWarningDTO> acknowledgeWarning(@PathVariable final Long id) {
    return Result.success(warningAppService.acknowledgeWarning(id));
  }

  /**
   * 解决预警
   *
   * @param id 预警ID
   * @return 解决后的预警
   */
  @Operation(summary = "解决预警")
  @PostMapping("/{id}/resolve")
  @RequirePermission("knowledge:quality:update")
  @OperationLog(module = "质量管理", action = "解决风险预警")
  public Result<RiskWarningDTO> resolveWarning(@PathVariable final Long id) {
    return Result.success(warningAppService.resolveWarning(id));
  }

  /**
   * 关闭预警
   *
   * @param id 预警ID
   * @return 关闭后的预警
   */
  @Operation(summary = "关闭预警")
  @PostMapping("/{id}/close")
  @RequirePermission("knowledge:quality:update")
  @OperationLog(module = "质量管理", action = "关闭风险预警")
  public Result<RiskWarningDTO> closeWarning(@PathVariable final Long id) {
    return Result.success(warningAppService.closeWarning(id));
  }

  /**
   * 获取预警详情
   *
   * @param id 预警ID
   * @return 预警详情
   */
  @Operation(summary = "获取预警详情")
  @GetMapping("/{id}")
  @RequirePermission("knowledge:quality:detail")
  public Result<RiskWarningDTO> getWarningById(@PathVariable final Long id) {
    return Result.success(warningAppService.getWarningById(id));
  }

  /**
   * 获取项目的所有预警
   *
   * @param matterId 项目ID
   * @return 预警列表
   */
  @Operation(summary = "获取项目的所有预警")
  @GetMapping("/matter/{matterId}")
  @RequirePermission("knowledge:quality:list")
  public Result<List<RiskWarningDTO>> getWarningsByMatterId(@PathVariable final Long matterId) {
    return Result.success(warningAppService.getWarningsByMatterId(matterId));
  }

  /**
   * 获取活跃的预警
   *
   * @return 活跃预警列表
   */
  @Operation(summary = "获取活跃的预警")
  @GetMapping("/active")
  @RequirePermission("knowledge:quality:list")
  public Result<List<RiskWarningDTO>> getActiveWarnings() {
    return Result.success(warningAppService.getActiveWarnings());
  }

  /**
   * 获取高风险预警
   *
   * @return 高风险预警列表
   */
  @Operation(summary = "获取高风险预警")
  @GetMapping("/high-risk")
  @RequirePermission("knowledge:quality:list")
  public Result<List<RiskWarningDTO>> getHighRiskWarnings() {
    return Result.success(warningAppService.getHighRiskWarnings());
  }
}
