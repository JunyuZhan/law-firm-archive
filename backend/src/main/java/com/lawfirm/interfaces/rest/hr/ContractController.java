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

/** 劳动合同管理接口 */
@Tag(name = "劳动合同管理", description = "劳动合同管理相关接口")
@RestController("laborContractController")
@RequestMapping("/hr/contract")
@RequiredArgsConstructor
public class ContractController {

  /** 劳动合同服务. */
  private final ContractAppService contractAppService;

  /**
   * 分页查询劳动合同
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询劳动合同")
  @GetMapping
  @RequirePermission("hr:contract:list")
  public Result<PageResult<ContractDTO>> listContracts(final ContractQueryDTO query) {
    return Result.success(contractAppService.listContracts(query));
  }

  /**
   * 根据ID查询劳动合同
   *
   * @param id 合同ID
   * @return 合同详情
   */
  @Operation(summary = "根据ID查询劳动合同")
  @GetMapping("/{id}")
  @RequirePermission("hr:contract:detail")
  public Result<ContractDTO> getContract(@PathVariable final Long id) {
    return Result.success(contractAppService.getContractById(id));
  }

  /**
   * 根据员工ID查询所有合同
   *
   * @param employeeId 员工ID
   * @return 合同列表
   */
  @Operation(summary = "根据员工ID查询所有合同")
  @GetMapping("/employee/{employeeId}")
  @RequirePermission("hr:contract:list")
  public Result<List<ContractDTO>> getContractsByEmployeeId(@PathVariable final Long employeeId) {
    return Result.success(contractAppService.getContractsByEmployeeId(employeeId));
  }

  /**
   * 创建劳动合同
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建劳动合同")
  @PostMapping
  @RequirePermission("hr:contract:create")
  @OperationLog(module = "劳动合同管理", action = "创建劳动合同")
  public Result<ContractDTO> createContract(
      @Valid @RequestBody final CreateContractCommand command) {
    return Result.success(contractAppService.createContract(command));
  }

  /**
   * 更新劳动合同
   *
   * @param id 合同ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新劳动合同")
  @PutMapping("/{id}")
  @RequirePermission("hr:contract:update")
  @OperationLog(module = "劳动合同管理", action = "更新劳动合同")
  public Result<ContractDTO> updateContract(
      @PathVariable final Long id, @Valid @RequestBody final UpdateContractCommand command) {
    return Result.success(contractAppService.updateContract(id, command));
  }

  /**
   * 删除劳动合同
   *
   * @param id 合同ID
   * @return 无返回
   */
  @Operation(summary = "删除劳动合同")
  @DeleteMapping("/{id}")
  @RequirePermission("hr:contract:delete")
  @OperationLog(module = "劳动合同管理", action = "删除劳动合同")
  public Result<Void> deleteContract(@PathVariable final Long id) {
    contractAppService.deleteContract(id);
    return Result.success();
  }

  /**
   * 续签合同
   *
   * @param id 合同ID
   * @param newStartDate 新合同开始日期
   * @param newEndDate 新合同结束日期
   * @return 续签后的合同
   */
  @Operation(summary = "续签合同")
  @PostMapping("/{id}/renew")
  @RequirePermission("hr:contract:renew")
  @OperationLog(module = "劳动合同管理", action = "续签劳动合同")
  public Result<ContractDTO> renewContract(
      @PathVariable final Long id,
      @RequestParam final LocalDate newStartDate,
      @RequestParam final LocalDate newEndDate) {
    return Result.success(contractAppService.renewContract(id, newStartDate, newEndDate));
  }
}
