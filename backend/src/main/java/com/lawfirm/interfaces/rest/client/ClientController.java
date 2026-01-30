package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.application.client.command.UpdateClientCommand;
import com.lawfirm.application.client.dto.ClientDTO;
import com.lawfirm.application.client.dto.ClientQueryDTO;
import com.lawfirm.application.client.dto.ClientSimpleDTO;
import com.lawfirm.application.client.service.ClientAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 客户管理 Controller */
@Tag(name = "客户管理", description = "客户管理相关接口")
@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

  /** 客户应用服务. */
  private final ClientAppService clientAppService;

  /** 活跃状态 */
  private static final String ACTIVE_STATUS = "ACTIVE";

  /**
   * 分页查询客户列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询客户列表")
  @GetMapping("/list")
  @RequirePermission("client:list")
  public Result<PageResult<ClientDTO>> listClients(final ClientQueryDTO query) {
    PageResult<ClientDTO> result = clientAppService.listClients(query);
    return Result.success(result);
  }

  /**
   * 获取客户选择列表（公共接口，无需权限） 用于其他模块的下拉选择框，返回简化的客户信息 所有登录用户都可以访问
   *
   * <p>安全说明：只返回必要字段（id、编号、名称），不包含联系方式等敏感信息
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "获取客户选择列表", description = "公共接口，返回客户ID和名称用于下拉选择")
  @GetMapping("/select-options")
  public Result<PageResult<ClientSimpleDTO>> getClientSelectOptions(final ClientQueryDTO query) {
    // 默认只查询正式客户，不包含潜在客户
    if (query.getStatus() == null) {
      query.setStatus(ACTIVE_STATUS);
    }
    PageResult<ClientDTO> result = clientAppService.listClients(query);

    // 转换为简单DTO，只保留必要字段
    List<ClientSimpleDTO> simpleList =
        result.getList().stream()
            .map(
                c ->
                    ClientSimpleDTO.builder()
                        .id(c.getId())
                        .clientNo(c.getClientNo())
                        .name(c.getName())
                        .clientType(c.getClientType())
                        .status(c.getStatus())
                        .build())
            .collect(Collectors.toList());

    return Result.success(
        PageResult.of(simpleList, result.getTotal(), result.getPageNum(), result.getPageSize()));
  }

  /**
   * 获取客户详情
   *
   * @param id 客户ID
   * @return 客户详情
   */
  @Operation()
  @GetMapping("/{id}")
  @RequirePermission("client:list")
  public Result<ClientDTO> getClient(@PathVariable final Long id) {
    ClientDTO client = clientAppService.getClientById(id);
    return Result.success(client);
  }

  /**
   * 创建客户
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation()
  @PostMapping
  @RequirePermission("client:create")
  @OperationLog(module = "客户管理", action = "创建客户")
  @RepeatSubmit(interval = 5000, message = "请勿重复提交客户信息")
  public Result<ClientDTO> createClient(@RequestBody @Valid final CreateClientCommand command) {
    ClientDTO client = clientAppService.createClient(command);
    return Result.success(client);
  }

  /**
   * 更新客户
   *
   * @param command 更新命令
   * @return 更新后的客户信息
   */
  @PutMapping
  @RequirePermission("client:update")
  @OperationLog(module = "客户管理", action = "更新客户")
  public Result<ClientDTO> updateClient(@RequestBody @Valid final UpdateClientCommand command) {
    ClientDTO client = clientAppService.updateClient(command);
    return Result.success(client);
  }

  /**
   * 删除客户
   *
   * @param id 客户ID
   * @return 操作结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("client:delete")
  @OperationLog(module = "客户管理", action = "删除客户")
  public Result<Void> deleteClient(@PathVariable final Long id) {
    clientAppService.deleteClient(id);
    return Result.success();
  }

  /**
   * 批量删除客户
   *
   * @param request 批量删除请求
   * @return 操作结果
   */
  @DeleteMapping("/batch")
  @RequirePermission("client:delete")
  @OperationLog(module = "客户管理", action = "批量删除客户")
  public Result<Void> deleteClients(@RequestBody @Valid final BatchDeleteRequest request) {
    request.getIds().forEach(clientAppService::deleteClient);
    return Result.success();
  }

  /**
   * 修改客户状态
   *
   * @param id 客户ID
   * @param request 状态修改请求
   * @return 操作结果
   */
  @PutMapping("/{id}/status")
  @RequirePermission("client:update")
  @OperationLog(module = "客户管理", action = "修改客户状态")
  public Result<Void> changeStatus(
      @PathVariable final Long id, @RequestBody @Valid final ChangeStatusRequest request) {
    clientAppService.changeStatus(id, request.getStatus());
    return Result.success();
  }

  /**
   * 潜在客户转正式客户
   *
   * @param id 客户ID
   * @return 操作结果
   */
  @PostMapping("/{id}/convert")
  @RequirePermission("client:update")
  @OperationLog(module = "客户管理", action = "客户转正式")
  public Result<Void> convertToFormal(@PathVariable final Long id) {
    clientAppService.convertToFormal(id);
    return Result.success();
  }

  /**
   * 导出客户信息（M2-008，P2）
   *
   * @param query 查询条件
   * @return Excel文件流
   * @throws IOException IO异常
   */
  @GetMapping("/export")
  @RequirePermission("client:export")
  @Operation(summary = "导出客户信息", description = "导出客户信息为Excel文件")
  @OperationLog(module = "客户管理", action = "导出客户")
  public ResponseEntity<InputStreamResource> exportClients(final ClientQueryDTO query)
      throws IOException {
    ByteArrayInputStream inputStream = clientAppService.exportClients(query);

    String fileName =
        "客户信息_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".xlsx";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
    headers.add(
        HttpHeaders.CONTENT_TYPE,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    return ResponseEntity.ok()
        .headers(headers)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new InputStreamResource(inputStream));
  }

  /**
   * 批量导入客户（M2-007，P2）
   *
   * @param file Excel文件
   * @return 导入结果
   * @throws IOException IO异常
   */
  @PostMapping("/import")
  @RequirePermission("client:import")
  @Operation(summary = "批量导入客户", description = "从Excel文件批量导入客户信息")
  @OperationLog(module = "客户管理", action = "批量导入客户")
  public Result<Map<String, Object>> importClients(@RequestParam("file") final MultipartFile file)
      throws IOException {
    // ✅ 安全验证：验证上传的Excel文件
    FileValidator.ValidationResult validationResult = FileValidator.validate(file);
    if (!validationResult.isValid()) {
      return Result.error(validationResult.getErrorMessage());
    }
    Map<String, Object> result = clientAppService.importClients(file);
    return Result.success(result);
  }

  /**
   * 利冲审查 - 搜索全所客户（用于对方当事人字段） 数据权限：所有人都可以搜索全所客户用于利冲审查
   *
   * @param keyword 搜索关键字
   * @param limit 返回数量限制
   * @return 客户列表
   */
  @GetMapping("/search-for-conflict")
  @RequirePermission("conflict:check")
  @Operation(summary = "利冲审查客户搜索", description = "利冲审查时搜索全所客户，用于对方当事人字段")
  public Result<List<ClientDTO>> searchClientsForConflictCheck(
      @RequestParam final String keyword, @RequestParam(defaultValue = "20") final int limit) {
    List<ClientDTO> clients = clientAppService.searchClientsForConflictCheck(keyword, limit);
    return Result.success(clients);
  }

  // ========== Request DTOs ==========

  /** 批量删除请求. */
  @Data
  public static class BatchDeleteRequest {
    /** 客户ID列表. */
    @NotEmpty(message = "客户ID列表不能为空")
    private List<Long> ids;
  }

  /** 修改状态请求. */
  @Data
  public static class ChangeStatusRequest {
    /** 状态. */
    @NotBlank(message = "状态不能为空")
    private String status;
  }
}
