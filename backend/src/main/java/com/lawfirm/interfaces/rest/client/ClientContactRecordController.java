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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户联系记录 Controller
 */
@Tag(name = "客户联系记录", description = "客户联系记录管理相关接口")
@RestController
@RequestMapping("/client/contact-record")
@RequiredArgsConstructor
public class ClientContactRecordController {

    private final ClientContactRecordAppService contactRecordAppService;

    @Operation(summary = "分页查询联系记录")
    @GetMapping
    @RequirePermission("client:contact-record:list")
    public Result<PageResult<ClientContactRecordDTO>> listContactRecords(ContactRecordQueryDTO query) {
        return Result.success(contactRecordAppService.listContactRecords(query));
    }

    @Operation(summary = "获取客户的联系记录列表")
    @GetMapping("/client/{clientId}")
    @RequirePermission("client:contact-record:list")
    public Result<List<ClientContactRecordDTO>> getContactRecordsByClientId(@PathVariable Long clientId) {
        return Result.success(contactRecordAppService.getContactRecordsByClientId(clientId));
    }

    @Operation(summary = "创建联系记录")
    @PostMapping
    @RequirePermission("client:contact-record:create")
    @OperationLog(module = "客户管理", action = "创建联系记录")
    public Result<ClientContactRecordDTO> createContactRecord(@RequestBody @Valid CreateContactRecordCommand command) {
        return Result.success(contactRecordAppService.createContactRecord(command));
    }

    @Operation(summary = "更新联系记录")
    @PutMapping("/{id}")
    @RequirePermission("client:contact-record:update")
    @OperationLog(module = "客户管理", action = "更新联系记录")
    public Result<ClientContactRecordDTO> updateContactRecord(@PathVariable Long id,
                                                               @RequestBody @Valid UpdateContactRecordCommand command) {
        command.setId(id);
        return Result.success(contactRecordAppService.updateContactRecord(command));
    }

    @Operation(summary = "删除联系记录")
    @DeleteMapping("/{id}")
    @RequirePermission("client:contact-record:delete")
    @OperationLog(module = "客户管理", action = "删除联系记录")
    public Result<Void> deleteContactRecord(@PathVariable Long id) {
        contactRecordAppService.deleteContactRecord(id);
        return Result.success();
    }

    @Operation(summary = "查询需要跟进的联系记录")
    @GetMapping("/follow-up")
    @RequirePermission("client:contact-record:list")
    public Result<List<ClientContactRecordDTO>> getFollowUpRecords(@RequestParam(required = false) LocalDate date) {
        return Result.success(contactRecordAppService.getFollowUpRecords(date));
    }
}

