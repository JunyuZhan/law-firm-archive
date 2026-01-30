package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityCheckCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckDTO;
import com.lawfirm.application.knowledge.service.QualityCheckAppService;
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

/** 质量检查接口（M10-031） */
@Tag(name = "质量检查", description = "项目质量检查相关接口")
@RestController
@RequestMapping("/knowledge/quality-check")
@RequiredArgsConstructor
public class QualityCheckController {

  /** 质量检查应用服务. */
  private final QualityCheckAppService checkAppService;

  /**
   * 创建质量检查
   *
   * @param command 创建命令
   * @return 创建的检查
   */
  @Operation(summary = "创建质量检查")
  @PostMapping
  @RequirePermission("knowledge:quality:create")
  @OperationLog(module = "质量管理", action = "创建质量检查")
  public Result<QualityCheckDTO> createCheck(@RequestBody final CreateQualityCheckCommand command) {
    return Result.success(checkAppService.createCheck(command));
  }

  /**
   * 获取检查详情
   *
   * @param id 检查ID
   * @return 检查详情
   */
  @Operation(summary = "获取检查详情")
  @GetMapping("/{id}")
  @RequirePermission("knowledge:quality:detail")
  public Result<QualityCheckDTO> getCheckById(@PathVariable final Long id) {
    return Result.success(checkAppService.getCheckById(id));
  }

  /**
   * 获取项目的所有检查
   *
   * @param matterId 项目ID
   * @return 检查列表
   */
  @Operation(summary = "获取项目的所有检查")
  @GetMapping("/matter/{matterId}")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityCheckDTO>> getChecksByMatterId(@PathVariable final Long matterId) {
    return Result.success(checkAppService.getChecksByMatterId(matterId));
  }

  /**
   * 获取进行中的检查
   *
   * @return 进行中的检查列表
   */
  @Operation(summary = "获取进行中的检查")
  @GetMapping("/in-progress")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityCheckDTO>> getInProgressChecks() {
    return Result.success(checkAppService.getInProgressChecks());
  }
}
