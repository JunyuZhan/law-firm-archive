package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import com.archivesystem.dto.archive.ArchiveReceiveResponse;
import com.archivesystem.dto.borrow.BorrowLinkAccessResponse;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.BorrowLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 开放API控制器.
 * 供外部系统（如律所管理系统）调用，无需登录认证
 */
@Slf4j
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
@Tag(name = "开放接口", description = "供外部系统调用的接口")
public class OpenApiController {

    private final ArchiveService archiveService;
    private final BorrowLinkService borrowLinkService;

    /**
     * 接收档案.
     */
    @PostMapping("/archive/receive")
    @Operation(summary = "接收档案", description = "接收外部系统推送的归档档案")
    public Result<ArchiveReceiveResponse> receive(@Valid @RequestBody ArchiveReceiveRequest request) {
        log.info("接收档案请求: sourceType={}, sourceId={}, title={}", 
                request.getSourceType(), request.getSourceId(), request.getTitle());
        
        ArchiveReceiveResponse response = archiveService.receive(request);
        return Result.success("档案接收成功", response);
    }

    /**
     * 申请电子借阅链接.
     * 外部系统调用此接口获取档案的临时访问链接
     */
    @PostMapping("/borrow/apply")
    @Operation(summary = "申请电子借阅", description = "申请电子档案的临时访问链接，返回可直接访问的URL")
    public Result<BorrowLinkApplyResponse> applyBorrow(@Valid @RequestBody BorrowLinkApplyRequest request) {
        log.info("电子借阅申请: archiveId={}, archiveNo={}, userId={}, userName={}", 
                request.getArchiveId(), request.getArchiveNo(), request.getUserId(), request.getUserName());
        
        BorrowLinkApplyResponse response = borrowLinkService.applyLink(request);
        return Result.success("借阅链接生成成功", response);
    }

    /**
     * 公开访问档案.
     * 通过借阅链接访问档案，无需登录
     */
    @GetMapping("/borrow/access/{token}")
    @Operation(summary = "公开访问档案", description = "通过借阅链接访问档案内容，无需登录认证")
    public Result<BorrowLinkAccessResponse> accessArchive(
            @Parameter(description = "访问令牌") @PathVariable String token,
            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.info("公开访问档案: token={}, clientIp={}", token, clientIp);
        
        BorrowLinkAccessResponse response = borrowLinkService.validateAndAccess(token, clientIp);
        
        if (!Boolean.TRUE.equals(response.getValid())) {
            return Result.error(response.getInvalidReason());
        }
        
        return Result.success(response);
    }

    /**
     * 记录文件下载.
     */
    @PostMapping("/borrow/access/{token}/download/{fileId}")
    @Operation(summary = "记录下载", description = "记录通过借阅链接的文件下载行为")
    public Result<Void> recordDownload(
            @Parameter(description = "访问令牌") @PathVariable String token,
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        borrowLinkService.recordDownload(token, fileId);
        return Result.success();
    }

    /**
     * 健康检查.
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.success("ok");
    }

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
}
