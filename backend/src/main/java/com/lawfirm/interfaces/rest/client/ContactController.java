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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联系人管理 Controller
 */
@Tag(name = "联系人管理", description = "客户联系人管理相关接口")
@RestController
@RequestMapping("/client/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactAppService contactAppService;

    @Operation(summary = "获取客户联系人列表")
    @GetMapping("/client/{clientId}")
    @RequirePermission("client:contact:list")
    public Result<List<ContactDTO>> listContacts(@PathVariable Long clientId) {
        return Result.success(contactAppService.listContacts(clientId));
    }

    @Operation(summary = "获取联系人详情")
    @GetMapping("/{id}")
    @RequirePermission("client:contact:detail")
    public Result<ContactDTO> getContact(@PathVariable Long id) {
        return Result.success(contactAppService.getContactById(id));
    }

    @Operation(summary = "创建联系人")
    @PostMapping
    @RequirePermission("client:contact:create")
    @OperationLog(module = "客户管理", action = "创建联系人")
    public Result<ContactDTO> createContact(@RequestBody @Valid CreateContactCommand command) {
        return Result.success(contactAppService.createContact(command));
    }

    @Operation(summary = "更新联系人")
    @PutMapping("/{id}")
    @RequirePermission("client:contact:update")
    @OperationLog(module = "客户管理", action = "更新联系人")
    public Result<ContactDTO> updateContact(@PathVariable Long id, @RequestBody @Valid UpdateContactCommand command) {
        return Result.success(contactAppService.updateContact(id, command));
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/{id}")
    @RequirePermission("client:contact:delete")
    @OperationLog(module = "客户管理", action = "删除联系人")
    public Result<Void> deleteContact(@PathVariable Long id) {
        contactAppService.deleteContact(id);
        return Result.success();
    }

    @Operation(summary = "设置主要联系人")
    @PutMapping("/{id}/primary")
    @RequirePermission("client:contact:update")
    @OperationLog(module = "客户管理", action = "设置主要联系人")
    public Result<ContactDTO> setPrimaryContact(@PathVariable Long id) {
        return Result.success(contactAppService.setPrimaryContact(id));
    }
}

