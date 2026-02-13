package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.ArchiveDTO;
import com.archivesystem.dto.ArchiveReceiveDTO;
import com.archivesystem.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 开放API控制器.
 * 提供给外部系统调用的接口。
 */
@Slf4j
@Tag(name = "开放API", description = "供外部系统调用的接口")
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenApiController {

    private final ArchiveService archiveService;

    /**
     * 接收律所系统的档案.
     * 这是专门给律所管理系统调用的接口。
     */
    @Operation(summary = "接收律所档案", description = "接收律所管理系统推送的归档档案")
    @PostMapping("/law-firm/archive/receive")
    public Result<Map<String, Object>> receiveLawFirmArchive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ArchiveReceiveDTO dto) {
        
        log.info("接收律所系统档案: sourceId={}, archiveName={}", dto.getSourceId(), dto.getArchiveName());
        
        // TODO: 验证API密钥
        // validateApiKey(authorization);
        
        try {
            // 设置来源类型为律所系统
            dto.setSourceType("LAW_FIRM");
            
            ArchiveDTO result = archiveService.receiveArchive(dto);
            
            // 返回扁平格式响应（与客户服务系统对接格式一致）
            return Result.success(Map.of(
                    "id", result.getId().toString(),
                    "archiveNo", result.getArchiveNo(),
                    "status", result.getStatus(),
                    "receivedAt", result.getReceivedAt().toString()
            ));
        } catch (Exception e) {
            log.error("接收律所档案失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 健康检查接口.
     */
    @Operation(summary = "健康检查", description = "用于检测系统是否正常运行")
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        return Result.success(Map.of(
                "status", "UP",
                "system", "Archive Management System",
                "version", "1.0.0"
        ));
    }

    /**
     * 查询档案状态.
     * 供外部系统查询档案接收状态。
     */
    @Operation(summary = "查询档案状态", description = "根据来源ID查询档案状态")
    @GetMapping("/archive/status")
    public Result<Map<String, Object>> getArchiveStatus(
            @RequestParam String sourceType,
            @RequestParam String sourceId) {
        
        try {
            // TODO: 实现根据来源ID查询档案
            return Result.success(Map.of(
                    "sourceType", sourceType,
                    "sourceId", sourceId,
                    "found", false,
                    "message", "功能开发中"
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
