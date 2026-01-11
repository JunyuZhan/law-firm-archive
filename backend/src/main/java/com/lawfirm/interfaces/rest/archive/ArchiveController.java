package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.dto.ArchiveQueryDTO;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveCheckResult;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveDataSnapshot;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 档案管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveAppService archiveAppService;
    private final ApproverService approverService;
    private final MinioService minioService;

    /**
     * 分页查询档案列表
     */
    @GetMapping("/list")
    @RequirePermission("archive:list")
    public Result<PageResult<ArchiveDTO>> listArchives(ArchiveQueryDTO query) {
        PageResult<ArchiveDTO> result = archiveAppService.listArchives(query);
        return Result.success(result);
    }

    /**
     * 获取档案详情
     */
    @GetMapping("/{id}")
    @RequirePermission("archive:list")
    public Result<ArchiveDTO> getArchive(@PathVariable Long id) {
        ArchiveDTO archive = archiveAppService.getArchiveById(id);
        return Result.success(archive);
    }

    /**
     * 获取待归档案件列表
     */
    @GetMapping("/pending-matters")
    @RequirePermission("archive:create")
    public Result<List<Object>> getPendingMatters() {
        List<Object> matters = archiveAppService.getPendingMatters();
        return Result.success(matters);
    }

    /**
     * 归档预检查
     * 检查项目是否满足归档条件
     */
    @GetMapping("/check/{matterId}")
    @RequirePermission("archive:create")
    @Operation(summary = "归档预检查")
    public Result<ArchiveCheckResult> checkArchiveRequirements(@PathVariable Long matterId) {
        ArchiveCheckResult result = archiveAppService.checkArchiveRequirements(matterId);
        return Result.success(result);
    }

    /**
     * 预览归档数据
     * 获取项目所有相关数据的预览
     */
    @GetMapping("/preview/{matterId}")
    @RequirePermission("archive:create")
    @Operation(summary = "预览归档数据")
    public Result<ArchiveDataSnapshot> previewArchiveData(@PathVariable Long matterId) {
        ArchiveDataSnapshot snapshot = archiveAppService.previewArchiveData(matterId);
        return Result.success(snapshot);
    }

    /**
     * 获取可用的归档数据源配置
     */
    @GetMapping("/data-sources")
    @RequirePermission("archive:create")
    @Operation(summary = "获取归档数据源配置")
    public Result<List<Map<String, Object>>> getAvailableDataSources() {
        return Result.success(archiveAppService.getAvailableDataSources());
    }

    /**
     * 创建档案
     */
    @PostMapping
    @RequirePermission("archive:create")
    @OperationLog(module = "档案管理", action = "创建档案")
    public Result<ArchiveDTO> createArchive(@RequestBody @Valid CreateArchiveCommand command) {
        ArchiveDTO archive = archiveAppService.createArchive(command);
        return Result.success(archive);
    }

    /**
     * 获取入库审批人列表
     * 规则：优先显示主任（DIRECTOR），其次是团队负责人（TEAM_LEADER）
     */
    @GetMapping("/store/approvers")
    @RequirePermission("archive:create")
    @Operation(summary = "获取入库审批人列表", description = "获取可选的入库审批人，优先推荐主任")
    public Result<List<Map<String, Object>>> getStoreApprovers() {
        List<Map<String, Object>> approvers = approverService.getArchiveStoreAvailableApprovers();
        return Result.success(approvers);
    }

    /**
     * 提交入库审批
     */
    @PostMapping("/{id}/submit-store")
    @RequirePermission("archive:create")
    @OperationLog(module = "档案管理", action = "提交入库审批")
    public Result<Void> submitStoreApproval(@PathVariable Long id) {
        archiveAppService.submitStoreApproval(id);
        return Result.success();
    }

    /**
     * 审批入库
     */
    @PostMapping("/{id}/approve-store")
    @RequirePermission("archive:store:approve")
    @OperationLog(module = "档案管理", action = "审批入库")
    public Result<Void> approveStore(@PathVariable Long id, @RequestBody ApproveRequest request) {
        archiveAppService.approveStore(id, request.getApproved(), request.getComment());
        return Result.success();
    }

    /**
     * 档案入库（实际入库操作）
     */
    @PostMapping("/store")
    @RequirePermission("archive:store")
    @OperationLog(module = "档案管理", action = "档案入库")
    public Result<Void> storeArchive(@RequestBody @Valid StoreArchiveCommand command) {
        archiveAppService.storeArchive(command);
        return Result.success();
    }

    /**
     * 申请迁移档案
     */
    @PostMapping("/{id}/apply-migrate")
    @RequirePermission("archive:migrate:apply")
    @OperationLog(module = "档案管理", action = "申请迁移")
    public Result<Void> applyMigrate(@PathVariable Long id, @RequestBody MigrateRequest request) {
        archiveAppService.applyMigrate(id, request.getReason(), request.getMigrateTarget());
        return Result.success();
    }

    /**
     * 审批迁移档案
     */
    @PostMapping("/{id}/approve-migrate")
    @RequirePermission("archive:migrate:approve")
    @OperationLog(module = "档案管理", action = "审批迁移")
    public Result<Void> approveMigrate(@PathVariable Long id, @RequestBody ApproveMigrateRequest request) {
        archiveAppService.approveMigrate(id, request.getApproved(), request.getComment(), 
                request.getDeleteFiles() != null && request.getDeleteFiles());
        return Result.success();
    }

    /**
     * 申请销毁档案（兼容旧接口，内部调用迁移）
     * @deprecated 请使用 /apply-migrate
     */
    @PostMapping("/{id}/apply-destroy")
    @RequirePermission("archive:migrate:apply")
    @OperationLog(module = "档案管理", action = "申请销毁")
    @Deprecated
    public Result<Void> applyDestroy(@PathVariable Long id, @RequestBody DestroyRequest request) {
        archiveAppService.applyDestroy(id, request.getReason());
        return Result.success();
    }

    /**
     * 审批销毁档案（兼容旧接口，内部调用迁移审批）
     * @deprecated 请使用 /approve-migrate
     */
    @PostMapping("/{id}/approve-destroy")
    @RequirePermission("archive:migrate:approve")
    @OperationLog(module = "档案管理", action = "审批销毁")
    @Deprecated
    public Result<Void> approveDestroy(@PathVariable Long id, @RequestBody ApproveDestroyRequest request) {
        archiveAppService.approveDestroy(id, request.getApproved(), request.getComment());
        return Result.success();
    }

    /**
     * 获取即将到期的档案（M7-041）
     */
    @GetMapping("/expiring")
    @RequirePermission("archive:list")
    public Result<List<ArchiveDTO>> getExpiringArchives(@RequestParam(defaultValue = "90") int days) {
        return Result.success(archiveAppService.getExpiringArchives(days));
    }

    /**
     * 按库位查看档案（M7-022）
     */
    @GetMapping("/location/{locationId}")
    @RequirePermission("archive:list")
    public Result<List<ArchiveDTO>> getArchivesByLocation(@PathVariable Long locationId) {
        return Result.success(archiveAppService.getArchivesByLocation(locationId));
    }

    /**
     * 设置档案保管期限（M7-040）
     */
    @PutMapping("/{id}/retention-period")
    @RequirePermission("archive:update")
    @OperationLog(module = "档案管理", action = "设置保管期限")
    public Result<ArchiveDTO> setRetentionPeriod(@PathVariable Long id, @RequestBody SetRetentionPeriodRequest request) {
        return Result.success(archiveAppService.setRetentionPeriod(id, request.getRetentionPeriod()));
    }

    /**
     * 销毁登记（M7-044）
     */
    @PostMapping("/{id}/register-destroy")
    @RequirePermission("archive:destroy")
    @OperationLog(module = "档案管理", action = "销毁登记")
    public Result<ArchiveDTO> registerDestroy(@PathVariable Long id, @RequestBody RegisterDestroyRequest request) {
        return Result.success(archiveAppService.registerDestroy(
                id, request.getDestroyMethod(), request.getDestroyLocation(), request.getWitness()));
    }

    @Data
    public static class DestroyRequest {
        private String reason;
    }

    @Data
    public static class ApproveDestroyRequest {
        private Boolean approved;
        private String comment;
    }

    @Data
    public static class ApproveRequest {
        private Boolean approved;
        private String comment;
    }

    @Data
    public static class MigrateRequest {
        private String reason;
        private String migrateTarget;
    }

    @Data
    public static class ApproveMigrateRequest {
        private Boolean approved;
        private String comment;
        private Boolean deleteFiles;
    }

    @Data
    public static class SetRetentionPeriodRequest {
        private String retentionPeriod;
    }

    @Data
    public static class RegisterDestroyRequest {
        private String destroyMethod;
        private String destroyLocation;
        private String witness;
    }

    /**
     * 下载卷宗封面
     */
    @GetMapping("/{id}/cover")
    @RequirePermission("archive:list")
    @Operation(summary = "下载卷宗封面")
    public void downloadCover(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) {
        try {
            ArchiveDTO archive = archiveAppService.getArchiveById(id);
            if (archive.getElectronicUrl() == null || archive.getElectronicUrl().isEmpty()) {
                response.sendError(404, "封面不存在");
                return;
            }
            
            // 从MinIO下载封面文件
            String objectName = minioService.extractObjectName(archive.getElectronicUrl());
            if (objectName == null) {
                response.sendError(404, "封面文件路径无效");
                return;
            }
            
            byte[] coverBytes = minioService.downloadFileAsBytes(objectName);
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(coverBytes.length);
            String fileName = (archive.getArchiveName() != null ? archive.getArchiveName() : "档案") + "_卷宗封面.pdf";
            response.setHeader("Content-Disposition", 
                    "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
            
            // 写入文件内容
            response.getOutputStream().write(coverBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("下载卷宗封面失败: archiveId={}", id, e);
            try {
                response.sendError(500, "下载封面失败: " + e.getMessage());
            } catch (Exception sendError) {
                log.debug("发送错误响应失败", sendError);
            }
        }
    }

    /**
     * 重新生成卷宗封面
     */
    @PostMapping("/{id}/regenerate-cover")
    @RequirePermission("archive:update")
    @OperationLog(module = "档案管理", action = "重新生成卷宗封面")
    @Operation(summary = "重新生成卷宗封面")
    public Result<ArchiveDTO> regenerateCover(@PathVariable Long id) {
        ArchiveDTO archive = archiveAppService.regenerateCover(id);
        return Result.success(archive);
    }
}

