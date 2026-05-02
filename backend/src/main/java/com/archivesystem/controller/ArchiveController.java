package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.config.MetricsConfig;
import com.archivesystem.dto.archive.*;
import com.archivesystem.service.AccessLogService;
import com.archivesystem.service.ArchiveIndexService;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 档案控制器.
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
@Tag(name = "档案管理", description = "档案CRUD、文件上传下载")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final FileStorageService fileStorageService;
    private final ArchiveIndexService archiveIndexService;
    private final MetricsConfig metricsConfig;
    private final AccessLogService accessLogService;

    /**
     * 创建档案.
     */
    @PostMapping
    @Operation(summary = "创建档案")
    @PreAuthorize("isAuthenticated()")
    public Result<ArchiveDTO> create(@Valid @RequestBody ArchiveCreateRequest request) {
        ArchiveDTO archive = archiveService.create(request);
        return Result.success("创建成功", archive);
    }

    /**
     * 更新档案.
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<ArchiveDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveCreateRequest request) {
        ArchiveDTO archive = archiveService.update(id, request);
        return Result.success("更新成功", archive);
    }

    /**
     * 获取档案详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取档案详情")
    @PreAuthorize("isAuthenticated()")
    public Result<ArchiveDTO> getById(@PathVariable Long id) {
        ArchiveDTO archive = archiveService.getById(id);
        return Result.success(archive);
    }

    /**
     * 根据档案号获取详情.
     */
    @GetMapping("/no/{archiveNo}")
    @Operation(summary = "根据档案号获取详情")
    @PreAuthorize("isAuthenticated()")
    public Result<ArchiveDTO> getByArchiveNo(@PathVariable String archiveNo) {
        ArchiveDTO archive = archiveService.getByArchiveNo(archiveNo);
        return Result.success(archive);
    }

    /**
     * 分页查询档案列表.
     */
    @GetMapping
    @Operation(summary = "查询档案列表")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ArchiveDTO>> query(ArchiveQueryRequest request) {
        PageResult<ArchiveDTO> result = archiveService.query(request);
        return Result.success(result);
    }

    /**
     * 删除档案.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新档案状态.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新档案状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Parameter(description = "状态：DRAFT/PENDING_REVIEW/RECEIVED/CATALOGING/STORED/BORROWED") String status) {
        archiveService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    /**
     * 审核通过并正式入库.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过并正式入库")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER')")
    public Result<Void> approve(@PathVariable Long id) {
        archiveService.approve(id);
        return Result.success("审核通过，档案已正式入库", null);
    }

    /**
     * 补充上传（补充文件和纸质档案信息）.
     */
    @PostMapping("/{id}/supplement")
    @Operation(summary = "补充上传")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<ArchiveDTO> supplement(
            @PathVariable Long id,
            @RequestBody ArchiveSupplementRequest request) {
        ArchiveDTO archive = archiveService.supplement(id, request);
        return Result.success("补充成功", archive);
    }

    /**
     * 上传文件到档案.
     */
    @PostMapping("/{archiveId}/files")
    @Operation(summary = "上传文件到档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<DigitalFileDTO> uploadFile(
            @PathVariable Long archiveId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory,
            @Parameter(description = "案卷卷号") @RequestParam(required = false) Integer volumeNo,
            @Parameter(description = "案卷分段类型") @RequestParam(required = false) String sectionType,
            @Parameter(description = "件号/文号") @RequestParam(required = false) String documentNo,
            @Parameter(description = "起始页码") @RequestParam(required = false) Integer pageStart,
            @Parameter(description = "截止页码") @RequestParam(required = false) Integer pageEnd,
            @Parameter(description = "版本标识") @RequestParam(required = false) String versionLabel,
            @Parameter(description = "文件来源类型") @RequestParam(required = false) String fileSourceType,
            @Parameter(description = "扫描批次号") @RequestParam(required = false) String scanBatchNo,
            @Parameter(description = "扫描操作人") @RequestParam(required = false) String scanOperator,
            @Parameter(description = "扫描时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanTime,
            @Parameter(description = "扫描复核状态") @RequestParam(required = false) String scanCheckStatus,
            @Parameter(description = "扫描复核人") @RequestParam(required = false) String scanCheckBy,
            @Parameter(description = "扫描复核时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanCheckTime) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            return Result.error("400", "文件不能为空");
        }
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            return Result.error("400", "文件大小不能超过100MB");
        }
        
        var digitalFile = fileStorageService.upload(file, archiveId, fileCategory,
                volumeNo, sectionType, documentNo, pageStart, pageEnd, versionLabel,
                fileSourceType, scanBatchNo, scanOperator, scanTime,
                scanCheckStatus, scanCheckBy, scanCheckTime);

        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(digitalFile.getId())
                .archiveId(digitalFile.getArchiveId())
                .fileName(digitalFile.getFileName())
                .originalName(digitalFile.getOriginalName())
                .build();

        return Result.success("上传成功", dto);
    }

    /**
     * 获取文件下载链接.
     */
    @GetMapping("/files/{fileId}/download-url")
    @Operation(summary = "获取文件下载链接")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUrlResponse> getDownloadUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getDownloadUrl(fileId);
        return Result.success(FileUrlResponse.of(url));
    }

    /**
     * 获取文件预览链接.
     */
    @GetMapping("/files/{fileId}/preview-url")
    @Operation(summary = "获取文件预览链接")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUrlResponse> getPreviewUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getPreviewUrl(fileId);
        return Result.success(FileUrlResponse.of(url));
    }

    /**
     * 删除文件.
     */
    @DeleteMapping("/files/{fileId}")
    @Operation(summary = "删除文件")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        fileStorageService.delete(fileId);
        return Result.success("删除成功", null);
    }

    // ===== 全文检索接口 =====

    /**
     * 全文检索档案.
     */
    @GetMapping("/search")
    @Operation(summary = "全文检索", description = "支持关键词搜索、筛选、高亮、聚合统计")
    @PreAuthorize("isAuthenticated()")
    public Result<ArchiveSearchResult> search(ArchiveSearchRequest request, HttpServletRequest httpRequest) {
        metricsConfig.recordSearchRequest();
        long startTime = System.currentTimeMillis();
        
        ArchiveSearchResult result = archiveIndexService.search(request);
        
        // 记录搜索审计日志
        long duration = System.currentTimeMillis() - startTime;
        String clientIp = ClientIpUtils.resolve(httpRequest);
        accessLogService.logSearch(request.getKeyword(), (int) result.getTotal(), duration, clientIp);
        
        return Result.success(result);
    }

    /**
     * 获取搜索聚合统计.
     */
    @GetMapping("/search/aggregations")
    @Operation(summary = "获取搜索聚合统计", description = "按档案类型、年份、状态等维度统计")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getSearchAggregations() {
        Map<String, Object> aggregations = archiveIndexService.getAggregations();
        return Result.success(aggregations);
    }

    /**
     * 重建搜索索引.
     */
    @PostMapping("/search/reindex")
    @Operation(summary = "重建搜索索引", description = "全量重建Elasticsearch索引（管理员操作）")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> rebuildIndex() {
        archiveIndexService.rebuildAllIndexes();
        return Result.success("索引重建已启动", null);
    }

    /**
     * 索引单个档案.
     */
    @PostMapping("/{id}/index")
    @Operation(summary = "索引档案", description = "将指定档案同步到搜索索引")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> indexArchive(@PathVariable Long id) {
        archiveIndexService.indexArchive(id);
        return Result.success("索引已更新", null);
    }
}
