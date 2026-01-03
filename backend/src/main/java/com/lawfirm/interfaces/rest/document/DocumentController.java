package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateDocumentCommand;
import com.lawfirm.application.document.command.DocumentAuditQueryCommand;
import com.lawfirm.application.document.command.UpdateDocumentCommand;
import com.lawfirm.application.document.command.UploadNewVersionCommand;
import com.lawfirm.application.document.dto.DocumentAuditStatisticsDTO;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentQueryDTO;
import com.lawfirm.application.document.service.DocAccessLogService;
import com.lawfirm.application.document.service.DocumentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.document.entity.DocAccessLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档管理接口
 */
@Tag(name = "文档管理", description = "文档管理相关接口")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentAppService documentAppService;
    private final DocAccessLogService accessLogService;

    /**
     * 分页查询文档
     */
    @GetMapping
    @RequirePermission("doc:list")
    public Result<PageResult<DocumentDTO>> list(DocumentQueryDTO query) {
        return Result.success(documentAppService.listDocuments(query));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @RequirePermission("doc:detail")
    public Result<DocumentDTO> getById(@PathVariable Long id, HttpServletRequest request) {
        // 记录查看日志
        accessLogService.logAccess(id, DocAccessLog.ACTION_VIEW, request);
        return Result.success(documentAppService.getDocumentById(id));
    }

    /**
     * 下载文档（记录日志）
     */
    @PostMapping("/{id}/download")
    @RequirePermission("doc:download")
    public Result<DocumentDTO> download(@PathVariable Long id, HttpServletRequest request) {
        accessLogService.logAccess(id, DocAccessLog.ACTION_DOWNLOAD, request);
        return Result.success(documentAppService.getDocumentById(id));
    }

    /**
     * 获取文档访问日志
     */
    @GetMapping("/{id}/access-logs")
    @RequirePermission("doc:detail")
    public Result<PageResult<DocAccessLog>> getAccessLogs(
            @PathVariable Long id,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(accessLogService.getAccessLogs(id, null, actionType, pageNum, pageSize));
    }

    /**
     * 创建文档
     */
    @PostMapping
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "上传文档")
    public Result<DocumentDTO> create(@Valid @RequestBody CreateDocumentCommand command) {
        return Result.success(documentAppService.createDocument(command));
    }

    /**
     * 更新文档信息
     */
    @PutMapping("/{id}")
    @RequirePermission("doc:edit")
    @OperationLog(module = "文档管理", action = "更新文档")
    public Result<DocumentDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentCommand command) {
        command.setId(id);
        return Result.success(documentAppService.updateDocument(command));
    }

    /**
     * 上传新版本
     */
    @PostMapping("/{id}/versions")
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "上传新版本")
    public Result<DocumentDTO> uploadNewVersion(@PathVariable Long id, @Valid @RequestBody UploadNewVersionCommand command) {
        command.setDocumentId(id);
        return Result.success(documentAppService.uploadNewVersion(command));
    }

    /**
     * 获取文档所有版本
     */
    @GetMapping("/{id}/versions")
    @RequirePermission("doc:detail")
    public Result<List<DocumentDTO>> getVersions(@PathVariable Long id) {
        return Result.success(documentAppService.getDocumentVersions(id));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @RequirePermission("doc:delete")
    @OperationLog(module = "文档管理", action = "删除文档")
    public Result<Void> delete(@PathVariable Long id) {
        documentAppService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 归档文档
     */
    @PostMapping("/{id}/archive")
    @RequirePermission("doc:archive")
    @OperationLog(module = "文档管理", action = "归档文档")
    public Result<Void> archive(@PathVariable Long id) {
        documentAppService.archiveDocument(id);
        return Result.success();
    }

    /**
     * 按案件查询文档
     */
    @GetMapping("/matter/{matterId}")
    @RequirePermission("doc:list")
    public Result<List<DocumentDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(documentAppService.getDocumentsByMatter(matterId));
    }

    /**
     * 获取文档审计统计（M5-044）
     */
    @PostMapping("/audit/statistics")
    @RequirePermission("doc:audit:view")
    @Operation(summary = "获取文档审计统计", description = "统计文档访问情况，包括按用户、文档、操作类型、时间等维度")
    public Result<DocumentAuditStatisticsDTO> getAuditStatistics(@RequestBody DocumentAuditQueryCommand command) {
        return Result.success(accessLogService.getAuditStatistics(command));
    }

    /**
     * 查询文档审计报告（M5-045）
     */
    @PostMapping("/audit/report")
    @RequirePermission("doc:audit:view")
    @Operation(summary = "查询文档审计报告", description = "查询详细的文档访问审计报告数据")
    public Result<List<Map<String, Object>>> queryAuditReport(@RequestBody DocumentAuditQueryCommand command) {
        return Result.success(accessLogService.queryAuditReport(command));
    }
}
