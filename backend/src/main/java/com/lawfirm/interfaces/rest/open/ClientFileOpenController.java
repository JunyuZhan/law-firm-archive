package com.lawfirm.interfaces.rest.open;

import com.lawfirm.application.clientservice.dto.AccessLogCallbackRequest;
import com.lawfirm.application.clientservice.dto.ClientFileDTO;
import com.lawfirm.application.clientservice.dto.ClientFileReceiveRequest;
import com.lawfirm.application.clientservice.dto.DownloadLogCallbackRequest;
import com.lawfirm.application.clientservice.service.ClientAccessLogService;
import com.lawfirm.application.clientservice.service.ClientDownloadLogService;
import com.lawfirm.application.clientservice.service.ClientFileService;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 客户文件开放接口 供客服系统调用，接收客户上传的文件 */
@Slf4j
@Tag(name = "客户文件开放接口", description = "供客服系统调用，接收客户上传的文件信息")
@RestController
@RequestMapping("/open/client")
@RequiredArgsConstructor
public class ClientFileOpenController {

  /** API Key 配置，用于验证客服系统请求. */
  @Value("${client.service.api-key:}")
  private String apiKey;

  /** 客户文件服务. */
  private final ClientFileService clientFileService;

  /** 客户访问日志服务. */
  private final ClientAccessLogService clientAccessLogService;

  /** 客户下载日志服务. */
  private final ClientDownloadLogService clientDownloadLogService;

  /**
   * 验证 API Key.
   *
   * @param providedApiKey 提供的 API Key
   */
  private void validateApiKey(final String providedApiKey) {
    if (apiKey != null && !apiKey.isEmpty()) {
      if (providedApiKey == null || !apiKey.equals(providedApiKey)) {
        log.warn("客户文件开放接口认证失败: 无效的 API Key");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
      }
    }
  }

  /**
   * 接收客户上传的文件
   *
   * @param request 文件接收请求
   * @return 接收结果
   */
  @Operation(summary = "接收客户上传的文件", description = "客服系统调用此接口通知律所系统有新的客户文件")
  @PostMapping("/files")
  public Result<ClientFileDTO> receiveFile(
      @RequestHeader(value = "X-API-Key", required = false) final String providedApiKey,
      @Valid @RequestBody final ClientFileReceiveRequest request) {
    validateApiKey(providedApiKey);
    log.info("接收客户文件: matterId={}, fileName={}", request.getMatterId(), request.getFileName());
    return Result.success(clientFileService.receiveFile(request));
  }

  /**
   * 文件删除回调
   *
   * @param externalFileId 外部文件ID
   * @return 无返回
   */
  @Operation(summary = "文件删除回调", description = "客服系统删除文件后回调通知")
  @PostMapping("/files/deleted")
  public Result<Void> fileDeleted(
      @RequestHeader(value = "X-API-Key", required = false) final String providedApiKey,
      @RequestParam final String externalFileId) {
    validateApiKey(providedApiKey);
    log.info("收到文件删除回调: {}", externalFileId);
    // 这里可以更新本地记录状态
    return Result.success();
  }

  /**
   * 接收访问日志回调
   *
   * @param request 访问日志回调请求
   * @return 接收结果
   */
  @Operation(summary = "接收访问日志回调", description = "客户服务系统回调此接口通知客户访问行为")
  @PostMapping("/access-log")
  public Result<Void> receiveAccessLog(
      @RequestHeader(value = "X-API-Key", required = false) final String providedApiKey,
      @Valid @RequestBody final AccessLogCallbackRequest request) {
    validateApiKey(providedApiKey);
    log.info("收到访问日志回调: matterId={}, clientId={}, accessTime={}", 
        request.getMatterId(), request.getClientId(), request.getAccessTime());
    try {
      clientAccessLogService.saveAccessLog(request);
      return Result.success();
    } catch (Exception e) {
      log.error("处理访问日志回调失败: matterId={}", request.getMatterId(), e);
      return Result.error("处理访问日志回调失败");
    }
  }

  /**
   * 接收下载日志回调
   *
   * @param request 下载日志回调请求
   * @return 接收结果
   */
  @Operation(summary = "接收下载日志回调", description = "客户服务系统回调此接口通知客户下载行为")
  @PostMapping("/download-log")
  public Result<Void> receiveDownloadLog(
      @RequestHeader(value = "X-API-Key", required = false) final String providedApiKey,
      @Valid @RequestBody final DownloadLogCallbackRequest request) {
    validateApiKey(providedApiKey);
    log.info("收到下载日志回调: matterId={}, clientId={}, fileId={}, fileName={}, downloadTime={}", 
        request.getMatterId(), request.getClientId(), request.getFileId(), 
        request.getFileName(), request.getDownloadTime());
    try {
      clientDownloadLogService.saveDownloadLog(request);
      return Result.success();
    } catch (Exception e) {
      log.error("处理下载日志回调失败: matterId={}, fileId={}", 
          request.getMatterId(), request.getFileId(), e);
      return Result.error("处理下载日志回调失败");
    }
  }
}
