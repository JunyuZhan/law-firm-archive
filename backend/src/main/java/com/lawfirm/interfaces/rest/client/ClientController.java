package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.application.client.command.UpdateClientCommand;
import com.lawfirm.application.client.dto.ClientDTO;
import com.lawfirm.application.client.dto.ClientQueryDTO;
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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 客户管理 Controller
 */
@Tag(name = "客户管理", description = "客户管理相关接口")
@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientAppService clientAppService;

    /**
     * 分页查询客户列表
     */
    @Operation()
    @GetMapping("/list")
    @RequirePermission("client:list")
    public Result<PageResult<ClientDTO>> listClients(ClientQueryDTO query) {
        PageResult<ClientDTO> result = clientAppService.listClients(query);
        return Result.success(result);
    }

    /**
     * 获取客户详情
     */
    @Operation()
    @GetMapping("/{id}")
    @RequirePermission("client:list")
    public Result<ClientDTO> getClient(@PathVariable Long id) {
        ClientDTO client = clientAppService.getClientById(id);
        return Result.success(client);
    }

    /**
     * 创建客户
     */
    @Operation()
    @PostMapping
    @RequirePermission("client:create")
    @OperationLog(module = "客户管理", action = "创建客户")
    @RepeatSubmit(interval = 5000, message = "请勿重复提交客户信息")
    public Result<ClientDTO> createClient(@RequestBody @Valid CreateClientCommand command) {
        ClientDTO client = clientAppService.createClient(command);
        return Result.success(client);
    }

    /**
     * 更新客户
     */
    @PutMapping
    @RequirePermission("client:update")
    @OperationLog(module = "客户管理", action = "更新客户")
    public Result<ClientDTO> updateClient(@RequestBody @Valid UpdateClientCommand command) {
        ClientDTO client = clientAppService.updateClient(command);
        return Result.success(client);
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    @RequirePermission("client:delete")
    @OperationLog(module = "客户管理", action = "删除客户")
    public Result<Void> deleteClient(@PathVariable Long id) {
        clientAppService.deleteClient(id);
        return Result.success();
    }

    /**
     * 批量删除客户
     */
    @DeleteMapping("/batch")
    @RequirePermission("client:delete")
    @OperationLog(module = "客户管理", action = "批量删除客户")
    public Result<Void> deleteClients(@RequestBody @Valid BatchDeleteRequest request) {
        request.getIds().forEach(clientAppService::deleteClient);
        return Result.success();
    }

    /**
     * 修改客户状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("client:update")
    @OperationLog(module = "客户管理", action = "修改客户状态")
    public Result<Void> changeStatus(@PathVariable Long id,
                                      @RequestBody @Valid ChangeStatusRequest request) {
        clientAppService.changeStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 潜在客户转正式客户
     */
    @PostMapping("/{id}/convert")
    @RequirePermission("client:update")
    @OperationLog(module = "客户管理", action = "客户转正式")
    public Result<Void> convertToFormal(@PathVariable Long id) {
        clientAppService.convertToFormal(id);
        return Result.success();
    }

    /**
     * 导出客户信息（M2-008，P2）
     */
    @GetMapping("/export")
    @RequirePermission("client:export")
    @Operation(summary = "导出客户信息", description = "导出客户信息为Excel文件")
    @OperationLog(module = "客户管理", action = "导出客户")
    public ResponseEntity<InputStreamResource> exportClients(ClientQueryDTO query) throws IOException {
        ByteArrayInputStream inputStream = clientAppService.exportClients(query);
        
        String fileName = "客户信息_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    /**
     * 批量导入客户（M2-007，P2）
     */
    @PostMapping("/import")
    @RequirePermission("client:import")
    @Operation(summary = "批量导入客户", description = "从Excel文件批量导入客户信息")
    @OperationLog(module = "客户管理", action = "批量导入客户")
    public Result<Map<String, Object>> importClients(@RequestParam("file") MultipartFile file) throws IOException {
        // ✅ 安全验证：验证上传的Excel文件
        FileValidator.ValidationResult validationResult = FileValidator.validate(file);
        if (!validationResult.isValid()) {
            return Result.error(validationResult.getErrorMessage());
        }
        Map<String, Object> result = clientAppService.importClients(file);
        return Result.success(result);
    }

    /**
     * 利冲审查 - 搜索全所客户（用于对方当事人字段）
     * 数据权限：所有人都可以搜索全所客户用于利冲审查
     */
    @GetMapping("/search-for-conflict")
    @RequirePermission("conflict:check")
    @Operation(summary = "利冲审查客户搜索", description = "利冲审查时搜索全所客户，用于对方当事人字段")
    public Result<List<ClientDTO>> searchClientsForConflictCheck(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        List<ClientDTO> clients = clientAppService.searchClientsForConflictCheck(keyword, limit);
        return Result.success(clients);
    }

    // ========== Request DTOs ==========

    @Data
    public static class BatchDeleteRequest {
        @NotEmpty(message = "客户ID列表不能为空")
        private List<Long> ids;
    }

    @Data
    public static class ChangeStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }
}

