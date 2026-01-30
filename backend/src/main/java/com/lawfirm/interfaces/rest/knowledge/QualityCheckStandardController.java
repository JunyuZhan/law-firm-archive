package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityCheckStandardCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckStandardDTO;
import com.lawfirm.application.knowledge.service.QualityCheckStandardAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 质量检查标准接口（M10-030） */
@Tag(name = "质量检查标准", description = "质量检查标准管理相关接口")
@RestController
@RequestMapping("/knowledge/quality-standard")
@RequiredArgsConstructor
public class QualityCheckStandardController {

  /** 质量检查标准应用服务. */
  private final QualityCheckStandardAppService standardAppService;

  /**
   * 分页查询检查标准
   *
   * @return 检查标准列表
   */
  @Operation(summary = "分页查询检查标准")
  @GetMapping
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityCheckStandardDTO>> listStandards() {
    return Result.success(standardAppService.getEnabledStandards());
  }

  /**
   * 获取所有启用的检查标准
   *
   * @return 启用标准列表
   */
  @Operation(summary = "获取所有启用的检查标准")
  @GetMapping("/enabled")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityCheckStandardDTO>> getEnabledStandards() {
    return Result.success(standardAppService.getEnabledStandards());
  }

  /**
   * 按分类查询检查标准
   *
   * @param category 分类
   * @return 检查标准列表
   */
  @Operation(summary = "按分类查询检查标准")
  @GetMapping("/category/{category}")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityCheckStandardDTO>> getStandardsByCategory(
      @PathVariable final String category) {
    return Result.success(standardAppService.getStandardsByCategory(category));
  }

  /**
   * 获取检查标准详情
   *
   * @param id 标准ID
   * @return 标准详情
   */
  @Operation(summary = "获取检查标准详情")
  @GetMapping("/{id}")
  @RequirePermission("knowledge:quality:detail")
  public Result<QualityCheckStandardDTO> getStandardById(@PathVariable final Long id) {
    return Result.success(standardAppService.getStandardById(id));
  }

  /**
   * 创建检查标准
   *
   * @param command 创建命令
   * @return 创建的标准
   */
  @Operation(summary = "创建检查标准")
  @PostMapping
  @RequirePermission("knowledge:quality:create")
  @OperationLog(module = "质量管理", action = "创建检查标准")
  public Result<QualityCheckStandardDTO> createStandard(
      @RequestBody final CreateQualityCheckStandardCommand command) {
    return Result.success(standardAppService.createStandard(command));
  }

  /**
   * 更新检查标准
   *
   * @param id 标准ID
   * @param command 更新命令
   * @return 更新后的标准
   */
  @Operation(summary = "更新检查标准")
  @PutMapping("/{id}")
  @RequirePermission("knowledge:quality:edit")
  @OperationLog(module = "质量管理", action = "更新检查标准")
  public Result<QualityCheckStandardDTO> updateStandard(
      @PathVariable final Long id, @RequestBody final CreateQualityCheckStandardCommand command) {
    return Result.success(standardAppService.updateStandard(id, command));
  }

  /**
   * 删除检查标准
   *
   * @param id 标准ID
   * @return 操作结果
   */
  @Operation(summary = "删除检查标准")
  @DeleteMapping("/{id}")
  @RequirePermission("knowledge:quality:delete")
  @OperationLog(module = "质量管理", action = "删除检查标准")
  public Result<Void> deleteStandard(@PathVariable final Long id) {
    standardAppService.deleteStandard(id);
    return Result.success();
  }
}
