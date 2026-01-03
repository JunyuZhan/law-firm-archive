package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateContractCommand;
import com.lawfirm.application.hr.command.UpdateContractCommand;
import com.lawfirm.application.hr.dto.ContractDTO;
import com.lawfirm.application.hr.dto.ContractQueryDTO;
import com.lawfirm.application.hr.service.ContractAppService;
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
 * 劳动合同管理接口
 */
@Tag(name = "劳动合同管理", description = "劳动合同管理相关接口")
@RestController("laborContractController")
@RequestMapping("/hr/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractAppService contractAppService;

    @Operation(summary = "分页查询劳动合同")
    @GetMapping
    @RequirePermission("hr:contract:list")
    public Result<PageResult<ContractDTO>> listContracts(ContractQueryDTO query) {
        return Result.success(contractAppService.listContracts(query));
    }

    @Operation(summary = "根据ID查询劳动合同")
    @GetMapping("/{id}")
    @RequirePermission("hr:contract:detail")
    public Result<ContractDTO> getContract(@PathVariable Long id) {
        return Result.success(contractAppService.getContractById(id));
    }

    @Operation(summary = "根据员工ID查询所有合同")
    @GetMapping("/employee/{employeeId}")
    @RequirePermission("hr:contract:list")
    public Result<List<ContractDTO>> getContractsByEmployeeId(@PathVariable Long employeeId) {
        return Result.success(contractAppService.getContractsByEmployeeId(employeeId));
    }

    @Operation(summary = "创建劳动合同")
    @PostMapping
    @RequirePermission("hr:contract:create")
    @OperationLog(module = "劳动合同管理", action = "创建劳动合同")
    public Result<ContractDTO> createContract(@Valid @RequestBody CreateContractCommand command) {
        return Result.success(contractAppService.createContract(command));
    }

    @Operation(summary = "更新劳动合同")
    @PutMapping("/{id}")
    @RequirePermission("hr:contract:update")
    @OperationLog(module = "劳动合同管理", action = "更新劳动合同")
    public Result<ContractDTO> updateContract(@PathVariable Long id, @Valid @RequestBody UpdateContractCommand command) {
        return Result.success(contractAppService.updateContract(id, command));
    }

    @Operation(summary = "删除劳动合同")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:contract:delete")
    @OperationLog(module = "劳动合同管理", action = "删除劳动合同")
    public Result<Void> deleteContract(@PathVariable Long id) {
        contractAppService.deleteContract(id);
        return Result.success();
    }

    @Operation(summary = "续签合同")
    @PostMapping("/{id}/renew")
    @RequirePermission("hr:contract:renew")
    @OperationLog(module = "劳动合同管理", action = "续签劳动合同")
    public Result<ContractDTO> renewContract(
            @PathVariable Long id,
            @RequestParam LocalDate newStartDate,
            @RequestParam LocalDate newEndDate) {
        return Result.success(contractAppService.renewContract(id, newStartDate, newEndDate));
    }
}

