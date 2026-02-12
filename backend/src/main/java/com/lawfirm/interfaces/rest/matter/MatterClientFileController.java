package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.clientservice.dto.ClientFileDTO;
import com.lawfirm.application.clientservice.dto.ClientFileSyncRequest;
import com.lawfirm.application.clientservice.service.ClientFileService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 项目客户文件控制器 管理客户通过客服系统上传的文件 */
@Tag(name = "项目客户文件", description = "管理客户通过客服系统上传的文件")
@Slf4j
@RestController
@RequestMapping("/matter/client-files")
@RequiredArgsConstructor
public class MatterClientFileController {

  /** 客户文件服务 */
  private final ClientFileService clientFileService;

  /**
   * 获取客户文件列表
   *
   * @param matterId 项目ID
   * @param status 状态: PENDING/SYNCED/DELETED（可选）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 分页结果
   */
  @Operation(summary = "获取客户文件列表", description = "获取项目中客户上传的文件列表")
  @GetMapping
  @RequirePermission("matter:clientService:list")
  public Result<PageResult<ClientFileDTO>> getClientFiles(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @Parameter(description = "状态: PENDING/SYNCED/DELETED") @RequestParam(required = false)
          final String status,
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") final int pageSize) {
    return Result.success(clientFileService.getClientFiles(matterId, status, pageNum, pageSize));
  }

  /**
   * 获取待同步文件列表
   *
   * @param matterId 项目ID
   * @return 待同步文件列表
   */
  @Operation(summary = "获取待同步文件列表", description = "获取项目中待同步的客户文件")
  @GetMapping("/pending")
  @RequirePermission("matter:clientService:list")
  public Result<List<ClientFileDTO>> getPendingFiles(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId) {
    return Result.success(clientFileService.getPendingFiles(matterId));
  }

  /**
   * 获取待同步文件数量
   *
   * @param matterId 项目ID
   * @return 文件数量统计
   */
  @Operation(summary = "获取待同步文件数量", description = "统计项目中待同步的客户文件数量")
  @GetMapping("/pending/count")
  @RequirePermission("matter:clientService:list")
  public Result<Map<String, Integer>> countPendingFiles(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId) {
    int count = clientFileService.countPendingFiles(matterId);
    return Result.success(Map.of("count", count));
  }

  /**
   * 同步文件到卷宗
   *
   * @param request 同步文件请求
   * @return 文件信息
   */
  @Operation(summary = "同步文件到卷宗", description = "将客户上传的文件同步到指定卷宗目录")
  @PostMapping("/sync")
  @RequirePermission("matter:clientService:create")
  @OperationLog(module = "项目管理", action = "同步客户文件到卷宗")
  public Result<ClientFileDTO> syncFile(@Valid @RequestBody final ClientFileSyncRequest request) {
    Long operatorId = SecurityUtils.getCurrentUserId();
    return Result.success(clientFileService.syncToFolder(request, operatorId));
  }

  /**
   * 批量同步文件
   *
   * @param requests 同步文件请求列表
   * @return 文件信息列表
   */
  @Operation(summary = "批量同步文件", description = "批量将客户上传的文件同步到卷宗")
  @PostMapping("/sync/batch")
  @RequirePermission("matter:clientService:create")
  @OperationLog(module = "项目管理", action = "批量同步客户文件")
  public Result<List<ClientFileDTO>> batchSync(
      @Valid @RequestBody final List<ClientFileSyncRequest> requests) {
    Long operatorId = SecurityUtils.getCurrentUserId();
    return Result.success(clientFileService.batchSync(requests, operatorId));
  }

  /**
   * 忽略文件
   *
   * @param fileId 文件ID
   * @return 空结果
   */
  @Operation(summary = "忽略文件", description = "忽略客户上传的文件（不同步到卷宗）")
  @PostMapping("/{fileId}/ignore")
  @RequirePermission("matter:clientService:create")
  @OperationLog(module = "项目管理", action = "忽略客户文件")
  public Result<Void> ignoreFile(@PathVariable final Long fileId) {
    Long operatorId = SecurityUtils.getCurrentUserId();
    clientFileService.ignoreFile(fileId, operatorId);
    return Result.success();
  }

  /**
   * 处理 OPTIONS 预检请求（CORS）
   *
   * @param response HTTP响应
   */
  @RequestMapping(value = "/{fileId}/proxy", method = RequestMethod.OPTIONS)
  public void proxyFileOptions(final HttpServletResponse response) {
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "*");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * 文件代理接口（解决跨域问题） 代理客户服务系统的文件，用于预览和下载
   *
   * @param fileId 文件ID
   * @param response HTTP响应
   */
  @Operation(summary = "文件代理", description = "代理客户服务系统的文件，解决跨域问题")
  @GetMapping("/{fileId}/proxy")
  @RequirePermission("matter:clientService:list")
  public void proxyFile(
      @Parameter(description = "文件ID", required = true) @PathVariable final Long fileId,
      final HttpServletResponse response) {
    try {
      clientFileService.proxyFile(fileId, response);
    } catch (IOException e) {
      try {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件代理失败: " + e.getMessage());
      } catch (IOException ioException) {
        log.warn("发送文件代理错误响应失败: {}", ioException.getMessage());
      }
    }
  }
}
