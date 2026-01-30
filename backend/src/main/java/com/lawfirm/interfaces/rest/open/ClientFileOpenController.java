package com.lawfirm.interfaces.rest.open;

import com.lawfirm.application.clientservice.dto.ClientFileDTO;
import com.lawfirm.application.clientservice.dto.ClientFileReceiveRequest;
import com.lawfirm.application.clientservice.service.ClientFileService;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 客户文件开放接口 供客服系统调用，接收客户上传的文件 */
@Slf4j
@Tag(name = "客户文件开放接口", description = "供客服系统调用，接收客户上传的文件信息")
@RestController
@RequestMapping("/open/client")
@RequiredArgsConstructor
public class ClientFileOpenController {

  /** 客户文件服务. */
  private final ClientFileService clientFileService;

  /**
   * 接收客户上传的文件
   *
   * @param request 文件接收请求
   * @return 接收结果
   */
  @Operation(summary = "接收客户上传的文件", description = "客服系统调用此接口通知律所系统有新的客户文件")
  @PostMapping("/files")
  public Result<ClientFileDTO> receiveFile(
      @Valid @RequestBody final ClientFileReceiveRequest request) {
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
  public Result<Void> fileDeleted(@RequestParam final String externalFileId) {
    log.info("收到文件删除回调: {}", externalFileId);
    // 这里可以更新本地记录状态
    return Result.success();
  }
}
