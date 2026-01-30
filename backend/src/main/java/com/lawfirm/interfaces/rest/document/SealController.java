package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateSealCommand;
import com.lawfirm.application.document.command.UpdateSealCommand;
import com.lawfirm.application.document.dto.SealDTO;
import com.lawfirm.application.document.dto.SealQueryDTO;
import com.lawfirm.application.document.service.SealAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 印章管理接口 */
@RestController
@RequestMapping("/document/seal")
@RequiredArgsConstructor
public class SealController {

  /** 印章应用服务 */
  private final SealAppService sealAppService;

  /**
   * 分页查询印章 允许有印章管理权限(doc:seal:list)或用印申请权限(doc:seal:apply)的用户查看
   *
   * @param query 查询条件
   * @return 印章分页结果
   */
  @GetMapping
  @RequirePermission(
      value = {"doc:seal:list", "doc:seal:apply"},
      logical = RequirePermission.Logical.OR)
  public Result<PageResult<SealDTO>> list(final SealQueryDTO query) {
    return Result.success(sealAppService.listSeals(query));
  }

  /**
   * 获取印章详情 允许有印章管理权限(doc:seal:list)或用印申请权限(doc:seal:apply)的用户查看
   *
   * @param id 印章ID
   * @return 印章详情
   */
  @GetMapping("/{id}")
  @RequirePermission(
      value = {"doc:seal:list", "doc:seal:apply"},
      logical = RequirePermission.Logical.OR)
  public Result<SealDTO> getById(@PathVariable final Long id) {
    return Result.success(sealAppService.getSealById(id));
  }

  /**
   * 创建印章
   *
   * @param command 创建印章命令
   * @return 印章信息
   */
  @PostMapping
  @RequirePermission("doc:seal:list")
  @OperationLog(module = "印章管理", action = "创建印章")
  public Result<SealDTO> create(@Valid @RequestBody final CreateSealCommand command) {
    return Result.success(sealAppService.createSeal(command));
  }

  /**
   * 更新印章
   *
   * @param id 印章ID
   * @param command 更新印章命令
   * @return 印章信息
   */
  @PutMapping("/{id}")
  @RequirePermission("doc:seal:list")
  @OperationLog(module = "印章管理", action = "更新印章")
  public Result<SealDTO> update(
      @PathVariable final Long id, @Valid @RequestBody final UpdateSealCommand command) {
    return Result.success(sealAppService.updateSeal(id, command));
  }

  /**
   * 变更印章状态
   *
   * @param id 印章ID
   * @param status 状态
   * @return 空结果
   */
  @PutMapping("/{id}/status")
  @RequirePermission("doc:seal:list")
  @OperationLog(module = "印章管理", action = "变更印章状态")
  public Result<Void> changeStatus(@PathVariable final Long id, @RequestParam final String status) {
    sealAppService.changeSealStatus(id, status);
    return Result.success();
  }

  /**
   * 删除印章
   *
   * @param id 印章ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("doc:seal:list")
  @OperationLog(module = "印章管理", action = "删除印章")
  public Result<Void> delete(@PathVariable final Long id) {
    sealAppService.deleteSeal(id);
    return Result.success();
  }
}
