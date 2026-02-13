package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateShareholderCommand;
import com.lawfirm.application.client.command.UpdateShareholderCommand;
import com.lawfirm.application.client.dto.ClientShareholderDTO;
import com.lawfirm.application.client.service.ClientShareholderAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 客户股东信息 Controller */
@Tag(name = "客户股东信息", description = "客户股东信息管理相关接口")
@RestController
@RequestMapping("/client/shareholder")
@RequiredArgsConstructor
public class ClientShareholderController {

  /** 客户股东服务. */
  private final ClientShareholderAppService shareholderAppService;

  /**
   * 获取客户的股东列表
   *
   * @param clientId 客户ID
   * @return 股东列表
   */
  @Operation(summary = "获取客户的股东列表")
  @GetMapping("/client/{clientId}")
  @RequirePermission("client:shareholder:list")
  public Result<List<ClientShareholderDTO>> getShareholdersByClientId(
      @PathVariable final Long clientId) {
    return Result.success(shareholderAppService.getShareholdersByClientId(clientId));
  }

  /**
   * 创建股东信息
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建股东信息")
  @PostMapping
  @RequirePermission("client:shareholder:create")
  @OperationLog(module = "客户管理", action = "创建股东信息")
  public Result<ClientShareholderDTO> createShareholder(
      @RequestBody @Valid final CreateShareholderCommand command) {
    return Result.success(shareholderAppService.createShareholder(command));
  }

  /**
   * 更新股东信息
   *
   * @param id 股东ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新股东信息")
  @PutMapping("/{id}")
  @RequirePermission("client:shareholder:update")
  @OperationLog(module = "客户管理", action = "更新股东信息")
  public Result<ClientShareholderDTO> updateShareholder(
      @PathVariable final Long id, @RequestBody @Valid final UpdateShareholderCommand command) {
    command.setId(id);
    return Result.success(shareholderAppService.updateShareholder(command));
  }

  /**
   * 删除股东信息
   *
   * @param id 股东ID
   * @return 无返回
   */
  @Operation(summary = "删除股东信息")
  @DeleteMapping("/{id}")
  @RequirePermission("client:shareholder:delete")
  @OperationLog(module = "客户管理", action = "删除股东信息")
  public Result<Void> deleteShareholder(@PathVariable final Long id) {
    shareholderAppService.deleteShareholder(id);
    return Result.success();
  }
}
