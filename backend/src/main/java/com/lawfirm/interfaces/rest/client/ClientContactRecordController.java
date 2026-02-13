package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateContactRecordCommand;
import com.lawfirm.application.client.command.UpdateContactRecordCommand;
import com.lawfirm.application.client.dto.ClientContactRecordDTO;
import com.lawfirm.application.client.dto.ContactRecordQueryDTO;
import com.lawfirm.application.client.service.ClientContactRecordAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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

/** 客户联系记录 Controller */
@Tag(name = "客户联系记录", description = "客户联系记录管理相关接口")
@RestController
@RequestMapping("/client/contact-record")
@RequiredArgsConstructor
public class ClientContactRecordController {

  /** 客户联系记录服务. */
  private final ClientContactRecordAppService contactRecordAppService;

  /**
   * 分页查询联系记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询联系记录")
  @GetMapping
  @RequirePermission("client:contact-record:list")
  public Result<PageResult<ClientContactRecordDTO>> listContactRecords(
      final ContactRecordQueryDTO query) {
    return Result.success(contactRecordAppService.listContactRecords(query));
  }

  /**
   * 获取客户的联系记录列表
   *
   * @param clientId 客户ID
   * @return 联系记录列表
   */
  @Operation(summary = "获取客户的联系记录列表")
  @GetMapping("/client/{clientId}")
  @RequirePermission("client:contact-record:list")
  public Result<List<ClientContactRecordDTO>> getContactRecordsByClientId(
      @PathVariable final Long clientId) {
    return Result.success(contactRecordAppService.getContactRecordsByClientId(clientId));
  }

  /**
   * 创建联系记录
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建联系记录")
  @PostMapping
  @RequirePermission("client:contact-record:create")
  @OperationLog(module = "客户管理", action = "创建联系记录")
  public Result<ClientContactRecordDTO> createContactRecord(
      @RequestBody @Valid final CreateContactRecordCommand command) {
    return Result.success(contactRecordAppService.createContactRecord(command));
  }

  /**
   * 更新联系记录
   *
   * @param id 联系记录ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新联系记录")
  @PutMapping("/{id}")
  @RequirePermission("client:contact-record:update")
  @OperationLog(module = "客户管理", action = "更新联系记录")
  public Result<ClientContactRecordDTO> updateContactRecord(
      @PathVariable final Long id, @RequestBody @Valid final UpdateContactRecordCommand command) {
    command.setId(id);
    return Result.success(contactRecordAppService.updateContactRecord(command));
  }

  /**
   * 删除联系记录
   *
   * @param id 联系记录ID
   * @return 无返回
   */
  @Operation(summary = "删除联系记录")
  @DeleteMapping("/{id}")
  @RequirePermission("client:contact-record:delete")
  @OperationLog(module = "客户管理", action = "删除联系记录")
  public Result<Void> deleteContactRecord(@PathVariable final Long id) {
    contactRecordAppService.deleteContactRecord(id);
    return Result.success();
  }

  /**
   * 查询需要跟进的联系记录
   *
   * @param date 查询日期
   * @return 联系记录列表
   */
  @Operation(summary = "查询需要跟进的联系记录")
  @GetMapping("/follow-up")
  @RequirePermission("client:contact-record:list")
  public Result<List<ClientContactRecordDTO>> getFollowUpRecords(
      @RequestParam(required = false) final LocalDate date) {
    return Result.success(contactRecordAppService.getFollowUpRecords(date));
  }
}
