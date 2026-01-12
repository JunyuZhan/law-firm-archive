package com.lawfirm.interfaces.rest.open;

import com.lawfirm.application.openapi.dto.ClientFileDTO;
import com.lawfirm.application.openapi.dto.ClientFileReceiveRequest;
import com.lawfirm.application.openapi.service.ClientFileService;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 客户文件开放接口
 * 供客服系统调用，接收客户上传的文件
 */
@Slf4j
@Tag(name = "客户文件开放接口", description = "供客服系统调用，接收客户上传的文件信息")
@RestController
@RequestMapping("/open/client")
@RequiredArgsConstructor
public class ClientFileOpenController {

    private final ClientFileService clientFileService;

    @Operation(summary = "接收客户上传的文件", description = "客服系统调用此接口通知律所系统有新的客户文件")
    @PostMapping("/files")
    public Result<ClientFileDTO> receiveFile(@Valid @RequestBody ClientFileReceiveRequest request) {
        log.info("接收客户文件: matterId={}, fileName={}", request.getMatterId(), request.getFileName());
        return Result.success(clientFileService.receiveFile(request));
    }

    @Operation(summary = "文件删除回调", description = "客服系统删除文件后回调通知")
    @PostMapping("/files/deleted")
    public Result<Void> fileDeleted(@RequestParam String externalFileId) {
        log.info("收到文件删除回调: {}", externalFileId);
        // 这里可以更新本地记录状态
        return Result.success();
    }
}
