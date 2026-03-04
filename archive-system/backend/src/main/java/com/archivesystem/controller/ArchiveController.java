package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 档案控制器.
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
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<ArchiveDTO> create(@Valid @RequestBody ArchiveCreateRequest request) {
        ArchiveDTO archive = archiveService.create(request);
        return Result.success("创建成功", archive);
    }

    /**
     * 更新档案.
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
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
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新档案状态.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新档案状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Parameter(description = "状态：DRAFT/RECEIVED/CATALOGING/STORED/BORROWED") String status) {
        // 校验状态值
        if (!isValidStatus(status)) {
            return Result.error("无效的状态值: " + status);
        }
        archiveService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }
    
    /**
     * 校验档案状态值是否合法.
     */
    private boolean isValidStatus(String status) {
        return status != null && (
            "DRAFT".equals(status) || 
            "RECEIVED".equals(status) || 
            "CATALOGING".equals(status) || 
            "STORED".equals(status) || 
            "BORROWED".equals(status)
        );
    }

    /**
     * 补充上传（补充文件和纸质档案信息）.
     */
    @PostMapping("/{id}/supplement")
    @Operation(summary = "补充上传")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
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
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<DigitalFileDTO> uploadFile(
            @PathVariable Long archiveId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            return Result.error("文件大小不能超过100MB");
        }
        
        var digitalFile = fileStorageService.upload(file, archiveId, fileCategory);
        
        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(digitalFile.getId())
                .archiveId(digitalFile.getArchiveId())
                .fileName(digitalFile.getFileName())
                .originalName(digitalFile.getOriginalName())
                .fileSize(digitalFile.getFileSize())
                .mimeType(digitalFile.getMimeType())
                .build();
        
        return Result.success("上传成功", dto);
    }

    /**
     * 获取文件下载链接.
     */
    @GetMapping("/files/{fileId}/download-url")
    @Operation(summary = "获取文件下载链接")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getDownloadUrl(fileId);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 获取文件预览链接.
     */
    @GetMapping("/files/{fileId}/preview-url")
    @Operation(summary = "获取文件预览链接")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> getPreviewUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getPreviewUrl(fileId);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 删除文件.
     */
    @DeleteMapping("/files/{fileId}")
    @Operation(summary = "删除文件")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
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
        String clientIp = getClientIp(httpRequest);
        accessLogService.logSearch(request.getKeyword(), (int) result.getTotal(), duration, clientIp);
        
        return Result.success(result);
    }
    
    /**
     * 获取客户端IP.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> indexArchive(@PathVariable Long id) {
        archiveIndexService.indexArchive(id);
        return Result.success("索引已更新", null);
    }
}
