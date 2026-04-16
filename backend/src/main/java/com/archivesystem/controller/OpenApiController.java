package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import com.archivesystem.dto.archive.ArchiveReceiveResponse;
import com.archivesystem.dto.borrow.BorrowLinkAccessResponse;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.entity.ExternalSource;
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
 * @author junyuzhan
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
    public Result<BorrowLinkApplyResponse> applyBorrow(@Valid @RequestBody BorrowLinkApplyRequest request,
                                                       HttpServletRequest httpRequest) {
        ExternalSource externalSource = requireExternalSource(httpRequest);
        if (externalSource == null) {
            return Result.error("401", "开放接口认证上下文缺失");
        }
        log.info("电子借阅申请: archiveId={}, archiveNo={}, userId={}, userName={}", 
                request.getArchiveId(), request.getArchiveNo(), request.getUserId(), request.getUserName());
        
        BorrowLinkApplyResponse response = borrowLinkService.applyLink(request, externalSource.getSourceCode());
        return Result.success("借阅链接生成成功", response);
    }

    /**
     * 撤销电子借阅链接.
     */
    @PostMapping("/borrow/revoke/{linkId}")
    @Operation(summary = "撤销电子借阅", description = "撤销已生成的电子借阅链接，使其立即失效")
    public Result<Void> revokeBorrow(
            @Parameter(description = "借阅链接ID") @PathVariable Long linkId,
            @Parameter(description = "撤销原因") @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        ExternalSource externalSource = requireExternalSource(request);
        if (externalSource == null) {
            return Result.error("401", "开放接口认证上下文缺失");
        }
        log.info("电子借阅撤销: linkId={}, reason={}", linkId, reason);
        borrowLinkService.revoke(linkId, reason, externalSource.getSourceCode());
        return Result.success("借阅链接已撤销", null);
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
        String clientIp = ClientIpUtils.resolve(request);
        log.info("公开访问档案: token={}, clientIp={}", maskToken(token), clientIp);
        
        BorrowLinkAccessResponse response = borrowLinkService.validateAndAccess(token, clientIp);
        
        if (!Boolean.TRUE.equals(response.getValid())) {
            return Result.error("403", "访问链接无效或已过期");
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
            @Parameter(description = "文件ID") @PathVariable Long fileId,
            HttpServletRequest request) {
        borrowLinkService.recordDownload(token, fileId, ClientIpUtils.resolve(request));
        return Result.success();
    }

    /**
     * 获取短时预览链接.
     */
    @GetMapping("/borrow/access/{token}/preview/{fileId}")
    @Operation(summary = "获取预览链接", description = "校验借阅链接后返回短时预览地址")
    public Result<String> getPreviewUrl(
            @Parameter(description = "访问令牌") @PathVariable String token,
            @Parameter(description = "文件ID") @PathVariable Long fileId,
            HttpServletRequest request) {
        return Result.success(borrowLinkService.getFileAccessUrl(token, fileId, false, ClientIpUtils.resolve(request)));
    }

    /**
     * 获取短时下载链接.
     */
    @GetMapping("/borrow/access/{token}/download-url/{fileId}")
    @Operation(summary = "获取下载链接", description = "校验借阅链接后返回短时下载地址")
    public Result<String> getDownloadUrl(
            @Parameter(description = "访问令牌") @PathVariable String token,
            @Parameter(description = "文件ID") @PathVariable Long fileId,
            HttpServletRequest request) {
        return Result.success(borrowLinkService.getFileAccessUrl(token, fileId, true, ClientIpUtils.resolve(request)));
    }

    /**
     * 健康检查.
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.success("ok");
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private ExternalSource requireExternalSource(HttpServletRequest request) {
        Object source = request.getAttribute("externalSource");
        return source instanceof ExternalSource externalSource ? externalSource : null;
    }
}
