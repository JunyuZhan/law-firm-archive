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

/** 客户关联企业 Controller */
@Tag(name = "客户关联企业", description = "客户关联企业管理相关接口")
@RestController
@RequestMapping("/client/related-company")
@RequiredArgsConstructor
public class ClientRelatedCompanyController {

  /** 客户关联企业服务. */
  private final ClientRelatedCompanyAppService relatedCompanyAppService;

  /**
   * 获取客户的关联企业列表
   *
   * @param clientId 客户ID
   * @return 关联企业列表
   */
  @Operation(summary = "获取客户的关联企业列表")
  @GetMapping("/client/{clientId}")
  @RequirePermission("client:related-company:list")
  public Result<List<ClientRelatedCompanyDTO>> getRelatedCompaniesByClientId(
      @PathVariable final Long clientId) {
    return Result.success(relatedCompanyAppService.getRelatedCompaniesByClientId(clientId));
  }

  /**
   * 创建关联企业
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建关联企业")
  @PostMapping
  @RequirePermission("client:related-company:create")
  @OperationLog(module = "客户管理", action = "创建关联企业")
  public Result<ClientRelatedCompanyDTO> createRelatedCompany(
      @RequestBody @Valid final CreateRelatedCompanyCommand command) {
    return Result.success(relatedCompanyAppService.createRelatedCompany(command));
  }

  /**
   * 更新关联企业
   *
   * @param id 关联企业ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新关联企业")
  @PutMapping("/{id}")
  @RequirePermission("client:related-company:update")
  @OperationLog(module = "客户管理", action = "更新关联企业")
  public Result<ClientRelatedCompanyDTO> updateRelatedCompany(
      @PathVariable final Long id, @RequestBody @Valid final UpdateRelatedCompanyCommand command) {
    command.setId(id);
    return Result.success(relatedCompanyAppService.updateRelatedCompany(command));
  }

  /**
   * 删除关联企业
   *
   * @param id 关联企业ID
   * @return 无返回
   */
  @Operation(summary = "删除关联企业")
  @DeleteMapping("/{id}")
  @RequirePermission("client:related-company:delete")
  @OperationLog(module = "客户管理", action = "删除关联企业")
  public Result<Void> deleteRelatedCompany(@PathVariable final Long id) {
    relatedCompanyAppService.deleteRelatedCompany(id);
    return Result.success();
  }
}
