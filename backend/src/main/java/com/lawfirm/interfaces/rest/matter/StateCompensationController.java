package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateStateCompensationCommand;
import com.lawfirm.application.matter.command.UpdateStateCompensationCommand;
import com.lawfirm.application.matter.dto.StateCompensationDTO;
import com.lawfirm.application.matter.service.StateCompensationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 国家赔偿案件信息 Controller */
@RestController
@RequestMapping("/matter/{matterId}/state-compensation")
@RequiredArgsConstructor
@Tag(name = "国家赔偿管理", description = "国家赔偿案件业务信息管理接口")
public class StateCompensationController {

  /** 国家赔偿应用服务 */
  private final StateCompensationAppService stateCompensationAppService;

  /**
   * 获取案件的国家赔偿信息
   *
   * @param matterId 案件ID
   * @return 国家赔偿信息
   */
  @GetMapping
  @RequirePermission("matter:view")
  @Operation(summary = "获取国家赔偿信息", description = "根据案件ID获取国家赔偿业务信息")
  public Result<StateCompensationDTO> getByMatterId(@PathVariable final Long matterId) {
    StateCompensationDTO dto = stateCompensationAppService.getByMatterId(matterId);
    return Result.success(dto);
  }

  /**
   * 创建国家赔偿信息
   *
   * @param matterId 案件ID
   * @param command 创建国家赔偿命令
   * @return 国家赔偿信息
   */
  @PostMapping
  @RequirePermission("matter:update")
  @OperationLog(module = "国家赔偿管理", action = "创建国家赔偿信息")
  @Operation(summary = "创建国家赔偿信息", description = "为案件创建国家赔偿业务信息")
  public Result<StateCompensationDTO> create(
      @PathVariable final Long matterId,
      @RequestBody @Valid final CreateStateCompensationCommand command) {
    command.setMatterId(matterId);
    StateCompensationDTO dto = stateCompensationAppService.create(command);
    return Result.success(dto);
  }

  /**
   * 更新国家赔偿信息
   *
   * @param matterId 案件ID
   * @param id 国家赔偿信息ID
   * @param command 更新国家赔偿命令
   * @return 国家赔偿信息
   */
  @PutMapping("/{id}")
  @RequirePermission("matter:update")
  @OperationLog(module = "国家赔偿管理", action = "更新国家赔偿信息")
  @Operation(summary = "更新国家赔偿信息", description = "更新国家赔偿业务信息")
  public Result<StateCompensationDTO> update(
      @PathVariable final Long matterId,
      @PathVariable final Long id,
      @RequestBody @Valid final UpdateStateCompensationCommand command) {
    command.setId(id);
    StateCompensationDTO dto = stateCompensationAppService.update(command);
    return Result.success(dto);
  }

  /**
   * 删除国家赔偿信息
   *
   * @param matterId 案件ID
   * @param id 国家赔偿信息ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("matter:update")
  @OperationLog(module = "国家赔偿管理", action = "删除国家赔偿信息")
  @Operation(summary = "删除国家赔偿信息", description = "删除国家赔偿业务信息")
  public Result<Void> delete(@PathVariable final Long matterId, @PathVariable final Long id) {
    stateCompensationAppService.delete(id);
    return Result.success();
  }

  /**
   * 根据案件ID删除国家赔偿信息
   *
   * @param matterId 案件ID
   * @return 空结果
   */
  @DeleteMapping
  @RequirePermission("matter:update")
  @OperationLog(module = "国家赔偿管理", action = "删除国家赔偿信息")
  @Operation(summary = "根据案件ID删除国家赔偿信息", description = "删除指定案件的国家赔偿业务信息")
  public Result<Void> deleteByMatterId(@PathVariable final Long matterId) {
    stateCompensationAppService.deleteByMatterId(matterId);
    return Result.success();
  }
}
