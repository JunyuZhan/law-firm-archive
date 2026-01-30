package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.ConfirmHandoverCommand;
import com.lawfirm.application.system.command.CreateHandoverCommand;
import com.lawfirm.application.system.dto.DataHandoverDTO;
import com.lawfirm.application.system.dto.DataHandoverPreviewDTO;
import com.lawfirm.application.system.dto.DataHandoverQueryDTO;
import com.lawfirm.application.system.service.DataHandoverService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 数据交接 Controller */
@Tag(name = "数据交接", description = "用户数据交接管理")
@RestController
@RequestMapping("/system/data-handover")
@RequiredArgsConstructor
public class DataHandoverController {

  /** 数据交接服务 */
  private final DataHandoverService dataHandoverService;

  /**
   * 预览离职交接数据
   *
   * @param userId 用户ID
   * @return 预览数据
   */
  @GetMapping("/preview/{userId}")
  @RequirePermission("sys:handover:view")
  @Operation(summary = "预览交接数据", description = "预览指定用户的待交接数据")
  public Result<DataHandoverPreviewDTO> previewHandover(@PathVariable final Long userId) {
    DataHandoverPreviewDTO preview = dataHandoverService.previewResignationHandover(userId);
    return Result.success(preview);
  }

  /**
   * 创建离职交接
   *
   * @param command 创建交接命令
   * @return 交接单信息
   */
  @PostMapping("/resignation")
  @RequirePermission("sys:handover:create")
  @Operation(summary = "创建离职交接", description = "创建离职交接单，一次性移交所有数据")
  public Result<DataHandoverDTO> createResignationHandover(
      @RequestBody @Valid final CreateHandoverCommand command) {
    command.setHandoverType("RESIGNATION");
    DataHandoverDTO result = dataHandoverService.createResignationHandover(command);
    return Result.success(result);
  }

  /**
   * 创建项目移交
   *
   * @param command 创建交接命令
   * @return 交接单信息
   */
  @PostMapping("/project")
  @RequirePermission("sys:handover:create")
  @Operation(summary = "创建项目移交", description = "创建项目移交单，移交指定项目")
  public Result<DataHandoverDTO> createProjectHandover(
      @RequestBody @Valid final CreateHandoverCommand command) {
    command.setHandoverType("PROJECT");
    DataHandoverDTO result = dataHandoverService.createMatterHandover(command);
    return Result.success(result);
  }

  /**
   * 创建客户移交
   *
   * @param command 创建交接命令
   * @return 交接单信息
   */
  @PostMapping("/client")
  @RequirePermission("sys:handover:create")
  @Operation(summary = "创建客户移交", description = "创建客户移交单，移交指定客户")
  public Result<DataHandoverDTO> createClientHandover(
      @RequestBody @Valid final CreateHandoverCommand command) {
    command.setHandoverType("CLIENT");
    DataHandoverDTO result = dataHandoverService.createClientHandover(command);
    return Result.success(result);
  }

  /**
   * 确认交接
   *
   * @param id 交接单ID
   * @param command 确认交接命令（可选）
   * @return 空结果
   */
  @PostMapping("/{id}/confirm")
  @RequirePermission("sys:handover:confirm")
  @Operation(summary = "确认交接", description = "确认交接单，执行数据迁移")
  public Result<Void> confirmHandover(
      @PathVariable final Long id,
      @RequestBody(required = false) final ConfirmHandoverCommand command) {
    dataHandoverService.confirmHandover(id);
    return Result.success();
  }

  /**
   * 取消交接
   *
   * @param id 交接单ID
   * @param request 取消请求
   * @return 空结果
   */
  @PostMapping("/{id}/cancel")
  @RequirePermission("sys:handover:cancel")
  @Operation(summary = "取消交接", description = "取消待确认的交接单")
  public Result<Void> cancelHandover(
      @PathVariable final Long id, @RequestBody final CancelRequest request) {
    dataHandoverService.cancelHandover(id, request.getReason());
    return Result.success();
  }

  /**
   * 获取交接单详情
   *
   * @param id 交接单ID
   * @return 交接单详情
   */
  @GetMapping("/{id}")
  @RequirePermission("sys:handover:view")
  @Operation(summary = "获取交接单详情", description = "获取交接单及其明细")
  public Result<DataHandoverDTO> getHandover(@PathVariable final Long id) {
    DataHandoverDTO result = dataHandoverService.getHandoverById(id);
    return Result.success(result);
  }

  /**
   * 分页查询交接单
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("sys:handover:list")
  @Operation(summary = "分页查询交接单", description = "分页查询数据交接记录")
  public Result<PageResult<DataHandoverDTO>> listHandovers(final DataHandoverQueryDTO query) {
    PageResult<DataHandoverDTO> result = dataHandoverService.listHandovers(query);
    return Result.success(result);
  }

  /** 取消请求 */
  @Data
  public static class CancelRequest {
    /** 取消原因 */
    private String reason;
  }
}
