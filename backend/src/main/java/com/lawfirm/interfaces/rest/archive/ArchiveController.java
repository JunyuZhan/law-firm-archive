package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.dto.ArchiveQueryDTO;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案管理 Controller
 */
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveAppService archiveAppService;

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
     * 档案入库
     */
    @PostMapping("/store")
    @RequirePermission("archive:store")
    @OperationLog(module = "档案管理", action = "档案入库")
    public Result<Void> storeArchive(@RequestBody @Valid StoreArchiveCommand command) {
        archiveAppService.storeArchive(command);
        return Result.success();
    }

    /**
     * 申请销毁档案
     */
    @PostMapping("/{id}/apply-destroy")
    @RequirePermission("archive:destroy")
    @OperationLog(module = "档案管理", action = "申请销毁")
    public Result<Void> applyDestroy(@PathVariable Long id, @RequestBody DestroyRequest request) {
        archiveAppService.applyDestroy(id, request.getReason());
        return Result.success();
    }

    /**
     * 审批销毁档案
     */
    @PostMapping("/{id}/approve-destroy")
    @RequirePermission("archive:destroy:approve")
    @OperationLog(module = "档案管理", action = "审批销毁")
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
    public static class SetRetentionPeriodRequest {
        private String retentionPeriod;
    }

    @Data
    public static class RegisterDestroyRequest {
        private String destroyMethod;
        private String destroyLocation;
        private String witness;
    }
}

