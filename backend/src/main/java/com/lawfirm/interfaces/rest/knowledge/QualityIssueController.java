package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityIssueCommand;
import com.lawfirm.application.knowledge.dto.QualityIssueDTO;
import com.lawfirm.application.knowledge.service.QualityIssueAppService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 问题整改接口（M10-032） */
@Tag(name = "问题整改", description = "问题整改管理相关接口")
@RestController
@RequestMapping("/knowledge/quality-issue")
@RequiredArgsConstructor
public class QualityIssueController {

  /** 问题整改应用服务. */
  private final QualityIssueAppService issueAppService;

  /**
   * 查询问题列表
   *
   * @return 问题列表
   */
  @Operation(summary = "查询问题列表")
  @GetMapping
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityIssueDTO>> listIssues() {
    return Result.success(issueAppService.getPendingIssues());
  }

  /**
   * 创建问题
   *
   * @param command 创建命令
   * @return 创建的问题
   */
  @Operation(summary = "创建问题")
  @PostMapping
  @RequirePermission("knowledge:quality:create")
  @OperationLog(module = "质量管理", action = "创建问题")
  public Result<QualityIssueDTO> createIssue(@RequestBody final CreateQualityIssueCommand command) {
    return Result.success(issueAppService.createIssue(command));
  }

  /**
   * 更新问题状态
   *
   * @param id 问题ID
   * @param status 状态
   * @param resolution 解决方案
   * @return 更新后的问题
   */
  @Operation(summary = "更新问题状态")
  @PutMapping("/{id}/status")
  @RequirePermission("knowledge:quality:update")
  @OperationLog(module = "质量管理", action = "更新问题状态")
  public Result<QualityIssueDTO> updateIssueStatus(
      @PathVariable final Long id,
      @RequestParam final String status,
      @RequestParam(required = false) final String resolution) {
    return Result.success(issueAppService.updateIssueStatus(id, status, resolution));
  }

  /**
   * 获取问题详情
   *
   * @param id 问题ID
   * @return 问题详情
   */
  @Operation(summary = "获取问题详情")
  @GetMapping("/{id}")
  @RequirePermission("knowledge:quality:detail")
  public Result<QualityIssueDTO> getIssueById(@PathVariable final Long id) {
    return Result.success(issueAppService.getIssueById(id));
  }

  /**
   * 获取项目的所有问题
   *
   * @param matterId 项目ID
   * @return 问题列表
   */
  @Operation(summary = "获取项目的所有问题")
  @GetMapping("/matter/{matterId}")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityIssueDTO>> getIssuesByMatterId(@PathVariable final Long matterId) {
    return Result.success(issueAppService.getIssuesByMatterId(matterId));
  }

  /**
   * 获取待整改的问题
   *
   * @return 待整改问题列表
   */
  @Operation(summary = "获取待整改的问题")
  @GetMapping("/pending")
  @RequirePermission("knowledge:quality:list")
  public Result<List<QualityIssueDTO>> getPendingIssues() {
    return Result.success(issueAppService.getPendingIssues());
  }
}
