package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateRelatedCompanyCommand;
import com.lawfirm.application.client.command.UpdateRelatedCompanyCommand;
import com.lawfirm.application.client.dto.ClientRelatedCompanyDTO;
import com.lawfirm.application.client.service.ClientRelatedCompanyAppService;
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
 * 客户关联企业 Controller
 */
@Tag(name = "客户关联企业", description = "客户关联企业管理相关接口")
@RestController
@RequestMapping("/client/related-company")
@RequiredArgsConstructor
public class ClientRelatedCompanyController {

    private final ClientRelatedCompanyAppService relatedCompanyAppService;

    @Operation(summary = "获取客户的关联企业列表")
    @GetMapping("/client/{clientId}")
    @RequirePermission("client:related-company:list")
    public Result<List<ClientRelatedCompanyDTO>> getRelatedCompaniesByClientId(@PathVariable Long clientId) {
        return Result.success(relatedCompanyAppService.getRelatedCompaniesByClientId(clientId));
    }

    @Operation(summary = "创建关联企业")
    @PostMapping
    @RequirePermission("client:related-company:create")
    @OperationLog(module = "客户管理", action = "创建关联企业")
    public Result<ClientRelatedCompanyDTO> createRelatedCompany(@RequestBody @Valid CreateRelatedCompanyCommand command) {
        return Result.success(relatedCompanyAppService.createRelatedCompany(command));
    }

    @Operation(summary = "更新关联企业")
    @PutMapping("/{id}")
    @RequirePermission("client:related-company:update")
    @OperationLog(module = "客户管理", action = "更新关联企业")
    public Result<ClientRelatedCompanyDTO> updateRelatedCompany(@PathVariable Long id,
                                                                 @RequestBody @Valid UpdateRelatedCompanyCommand command) {
        command.setId(id);
        return Result.success(relatedCompanyAppService.updateRelatedCompany(command));
    }

    @Operation(summary = "删除关联企业")
    @DeleteMapping("/{id}")
    @RequirePermission("client:related-company:delete")
    @OperationLog(module = "客户管理", action = "删除关联企业")
    public Result<Void> deleteRelatedCompany(@PathVariable Long id) {
        relatedCompanyAppService.deleteRelatedCompany(id);
        return Result.success();
    }
}

