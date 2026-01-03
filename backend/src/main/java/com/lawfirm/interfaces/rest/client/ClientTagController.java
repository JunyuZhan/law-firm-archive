package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.AssignClientTagsCommand;
import com.lawfirm.application.client.command.CreateClientTagCommand;
import com.lawfirm.application.client.command.UpdateClientTagCommand;
import com.lawfirm.application.client.dto.ClientTagDTO;
import com.lawfirm.application.client.service.ClientTagAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户标签管理Controller
 */
@RestController
@RequestMapping("/client/tag")
@RequiredArgsConstructor
public class ClientTagController {

    private final ClientTagAppService clientTagAppService;

    /**
     * 查询所有标签
     */
    @GetMapping("/list")
    @RequirePermission("crm:client:tag:list")
    @Operation(summary = "查询所有标签", description = "获取系统中所有客户标签列表")
    public Result<List<ClientTagDTO>> listTags() {
        List<ClientTagDTO> tags = clientTagAppService.listTags();
        return Result.success(tags);
    }

    /**
     * 根据ID查询标签
     */
    @GetMapping("/{id}")
    @RequirePermission("crm:client:tag:list")
    @Operation(summary = "查询标签详情", description = "根据ID查询标签详细信息")
    public Result<ClientTagDTO> getTag(@PathVariable Long id) {
        ClientTagDTO tag = clientTagAppService.getTagById(id);
        return Result.success(tag);
    }

    /**
     * 创建标签
     */
    @PostMapping
    @RequirePermission("crm:client:tag:create")
    @Operation(summary = "创建标签", description = "创建新的客户标签")
    @OperationLog(module = "客户管理", action = "创建标签")
    public Result<ClientTagDTO> createTag(@RequestBody @Valid CreateClientTagCommand command) {
        ClientTagDTO tag = clientTagAppService.createTag(command);
        return Result.success(tag);
    }

    /**
     * 更新标签
     */
    @PutMapping
    @RequirePermission("crm:client:tag:update")
    @Operation(summary = "更新标签", description = "更新客户标签信息")
    @OperationLog(module = "客户管理", action = "更新标签")
    public Result<ClientTagDTO> updateTag(@RequestBody @Valid UpdateClientTagCommand command) {
        ClientTagDTO tag = clientTagAppService.updateTag(command);
        return Result.success(tag);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @RequirePermission("crm:client:tag:delete")
    @Operation(summary = "删除标签", description = "删除客户标签")
    @OperationLog(module = "客户管理", action = "删除标签")
    public Result<Void> deleteTag(@PathVariable Long id) {
        clientTagAppService.deleteTag(id);
        return Result.success();
    }

    /**
     * 查询客户的标签列表
     */
    @GetMapping("/client/{clientId}")
    @RequirePermission("crm:client:list")
    @Operation(summary = "查询客户标签", description = "获取指定客户的所有标签")
    public Result<List<ClientTagDTO>> getClientTags(@PathVariable Long clientId) {
        List<ClientTagDTO> tags = clientTagAppService.getClientTags(clientId);
        return Result.success(tags);
    }

    /**
     * 为客户分配标签
     */
    @PostMapping("/assign")
    @RequirePermission("crm:client:update")
    @Operation(summary = "分配标签", description = "为客户分配标签")
    @OperationLog(module = "客户管理", action = "分配标签")
    public Result<Void> assignTags(@RequestBody @Valid AssignClientTagsCommand command) {
        clientTagAppService.assignTags(command);
        return Result.success();
    }

    /**
     * 移除客户的标签
     */
    @DeleteMapping("/client/{clientId}/tag/{tagId}")
    @RequirePermission("crm:client:update")
    @Operation(summary = "移除标签", description = "移除客户的指定标签")
    @OperationLog(module = "客户管理", action = "移除标签")
    public Result<Void> removeClientTag(@PathVariable Long clientId, @PathVariable Long tagId) {
        clientTagAppService.removeClientTag(clientId, tagId);
        return Result.success();
    }
}

