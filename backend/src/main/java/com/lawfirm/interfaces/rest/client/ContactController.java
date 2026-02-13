package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateContactCommand;
import com.lawfirm.application.client.command.UpdateContactCommand;
import com.lawfirm.application.client.dto.ContactDTO;
import com.lawfirm.application.client.service.ContactAppService;
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

/** 联系人管理 Controller */
@Tag(name = "联系人管理", description = "客户联系人管理相关接口")
@RestController
@RequestMapping("/client/contact")
@RequiredArgsConstructor
public class ContactController {

  /** 联系人服务. */
  private final ContactAppService contactAppService;

  /**
   * 获取客户联系人列表
   *
   * @param clientId 客户ID
   * @return 联系人列表
   */
  @Operation(summary = "获取客户联系人列表")
  @GetMapping("/client/{clientId}")
  @RequirePermission("client:contact:list")
  public Result<List<ContactDTO>> listContacts(@PathVariable final Long clientId) {
    return Result.success(contactAppService.listContacts(clientId));
  }

  /**
   * 获取联系人详情
   *
   * @param id 联系人ID
   * @return 联系人详情
   */
  @Operation(summary = "获取联系人详情")
  @GetMapping("/{id}")
  @RequirePermission("client:contact:detail")
  public Result<ContactDTO> getContact(@PathVariable final Long id) {
    return Result.success(contactAppService.getContactById(id));
  }

  /**
   * 创建联系人
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建联系人")
  @PostMapping
  @RequirePermission("client:contact:create")
  @OperationLog(module = "客户管理", action = "创建联系人")
  public Result<ContactDTO> createContact(@RequestBody @Valid final CreateContactCommand command) {
    return Result.success(contactAppService.createContact(command));
  }

  /**
   * 更新联系人
   *
   * @param id 联系人ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新联系人")
  @PutMapping("/{id}")
  @RequirePermission("client:contact:update")
  @OperationLog(module = "客户管理", action = "更新联系人")
  public Result<ContactDTO> updateContact(
      @PathVariable final Long id, @RequestBody @Valid final UpdateContactCommand command) {
    return Result.success(contactAppService.updateContact(id, command));
  }

  /**
   * 删除联系人
   *
   * @param id 联系人ID
   * @return 无返回
   */
  @Operation(summary = "删除联系人")
  @DeleteMapping("/{id}")
  @RequirePermission("client:contact:delete")
  @OperationLog(module = "客户管理", action = "删除联系人")
  public Result<Void> deleteContact(@PathVariable final Long id) {
    contactAppService.deleteContact(id);
    return Result.success();
  }

  /**
   * 设置主要联系人
   *
   * @param id 联系人ID
   * @return 更新后的联系人
   */
  @Operation(summary = "设置主要联系人")
  @PutMapping("/{id}/primary")
  @RequirePermission("client:contact:update")
  @OperationLog(module = "客户管理", action = "设置主要联系人")
  public Result<ContactDTO> setPrimaryContact(@PathVariable final Long id) {
    return Result.success(contactAppService.setPrimaryContact(id));
  }
}
