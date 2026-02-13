package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import com.archivesystem.dto.archive.ArchiveReceiveResponse;
import com.archivesystem.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/open")
@RequiredArgsConstructor
@Tag(name = "开放接口", description = "供外部系统调用的接口")
public class OpenApiController {

    private final ArchiveService archiveService;

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
     * 健康检查.
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.success("ok");
    }
}
