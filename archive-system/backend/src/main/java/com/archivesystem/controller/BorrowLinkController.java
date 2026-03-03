package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.BorrowLink;
import com.archivesystem.service.BorrowLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 借阅链接管理控制器.
 */
@Slf4j
@RestController
@RequestMapping("/api/borrow-links")
@RequiredArgsConstructor
@Tag(name = "借阅链接管理", description = "电子借阅链接的管理接口")
public class BorrowLinkController {

    private final BorrowLinkService borrowLinkService;

    /**
     * 分页查询借阅链接列表.
     */
    @GetMapping
    @Operation(summary = "查询链接列表", description = "分页查询电子借阅链接列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<BorrowLink>> getList(
            @Parameter(description = "档案ID") @RequestParam(required = false) Long archiveId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<BorrowLink> result = borrowLinkService.getList(archiveId, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取链接详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取链接详情")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<BorrowLink> getById(@PathVariable Long id) {
        BorrowLink link = borrowLinkService.getById(id);
        return Result.success(link);
    }

    /**
     * 获取档案的有效链接列表.
     */
    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "获取档案的有效链接", description = "获取指定档案的所有有效借阅链接")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<BorrowLink>> getActiveByArchive(@PathVariable Long archiveId) {
        List<BorrowLink> links = borrowLinkService.getActiveByArchiveId(archiveId);
        return Result.success(links);
    }

    /**
     * 为借阅申请生成链接.
     */
    @PostMapping("/generate")
    @Operation(summary = "生成借阅链接", description = "为已审批通过的借阅申请生成访问链接")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<BorrowLink> generateLink(
            @Parameter(description = "借阅申请ID") @RequestParam Long borrowId,
            @Parameter(description = "有效期天数") @RequestParam(defaultValue = "7") Integer expireDays,
            @Parameter(description = "是否允许下载") @RequestParam(defaultValue = "true") Boolean allowDownload) {
        BorrowLink link = borrowLinkService.generateLinkForBorrow(borrowId, expireDays, allowDownload);
        return Result.success("链接生成成功", link);
    }

    /**
     * 撤销链接.
     */
    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤销链接", description = "撤销指定的借阅链接，使其立即失效")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<String> revoke(
            @PathVariable Long id,
            @Parameter(description = "撤销原因") @RequestParam(required = false) String reason) {
        borrowLinkService.revoke(id, reason);
        return Result.success("链接已撤销");
    }

    /**
     * 获取统计信息.
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取借阅链接的统计数据")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<BorrowLinkService.BorrowLinkStats> getStats() {
        BorrowLinkService.BorrowLinkStats stats = borrowLinkService.getStats();
        return Result.success(stats);
    }

    /**
     * 更新过期链接状态.
     */
    @PostMapping("/update-expired")
    @Operation(summary = "更新过期状态", description = "将所有已过期但状态未更新的链接标记为已过期")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Map<String, Integer>> updateExpired() {
        int count = borrowLinkService.updateExpiredLinks();
        return Result.success("更新完成", Map.of("updatedCount", count));
    }
}
