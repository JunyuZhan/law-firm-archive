package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.clientservice.dto.ClientAccessLogDTO;
import com.lawfirm.application.clientservice.dto.ClientDownloadLogDTO;
import com.lawfirm.application.clientservice.dto.PushConfigDTO;
import com.lawfirm.application.clientservice.dto.PushRecordDTO;
import com.lawfirm.application.clientservice.dto.PushRequest;
import com.lawfirm.application.clientservice.service.ClientAccessLogService;
import com.lawfirm.application.clientservice.service.ClientDownloadLogService;
import com.lawfirm.application.clientservice.service.DataPushService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 项目客户服务控制器 管理项目数据推送到客户服务系统 */
@Tag(name = "项目客户服务", description = "项目数据推送到客户服务系统")
@RestController
@RequestMapping("/matter/client-service")
@RequiredArgsConstructor
public class MatterClientServiceController {

  /** 数据推送服务 */
  private final DataPushService dataPushService;

  /** 客户访问日志服务 */
  private final ClientAccessLogService clientAccessLogService;

  /** 客户下载日志服务 */
  private final ClientDownloadLogService clientDownloadLogService;

  /**
   * 推送项目数据
   *
   * @param request 推送请求
   * @return 推送记录
   */
  @Operation(summary = "推送项目数据", description = "将项目数据推送到客户服务系统，客户服务系统会自动通知客户")
  @PostMapping("/push")
  @RequirePermission("matter:clientService:create")
  @OperationLog(module = "项目管理", action = "推送项目数据到客户服务系统")
  public Result<PushRecordDTO> pushData(@Valid @RequestBody final PushRequest request) {
    Long operatorId = SecurityUtils.getCurrentUserId();
    return Result.success(dataPushService.pushMatterData(request, operatorId));
  }

  /**
   * 获取推送记录
   *
   * @param matterId 项目ID
   * @param status 状态（可选）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 分页结果
   */
  @Operation(summary = "获取推送记录", description = "获取项目的数据推送历史记录")
  @GetMapping("/records")
  @RequirePermission("matter:clientService:list")
  public Result<PageResult<PushRecordDTO>> getPushRecords(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @Parameter(description = "状态") @RequestParam(required = false) final String status,
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") final int pageSize) {
    return Result.success(
        dataPushService.getPushRecords(matterId, null, status, pageNum, pageSize));
  }

  /**
   * 获取推送记录详情
   *
   * @param id 推送记录ID
   * @return 推送记录详情
   */
  @Operation(summary = "获取推送记录详情")
  @GetMapping("/records/{id}")
  @RequirePermission("matter:clientService:list")
  public Result<PushRecordDTO> getPushRecord(@PathVariable final Long id) {
    return Result.success(dataPushService.getPushRecordById(id));
  }

  /**
   * 获取最近一次成功推送
   *
   * @param matterId 项目ID
   * @return 推送记录
   */
  @Operation(summary = "获取最近一次成功推送")
  @GetMapping("/latest")
  @RequirePermission("matter:clientService:list")
  public Result<PushRecordDTO> getLatestPush(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId) {
    return Result.success(dataPushService.getLatestPush(matterId));
  }

  /**
   * 获取推送配置
   *
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @return 推送配置
   */
  @Operation(summary = "获取推送配置", description = "获取或创建项目的推送配置")
  @GetMapping("/config")
  @RequirePermission("matter:clientService:list")
  public Result<PushConfigDTO> getConfig(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @Parameter(description = "客户ID", required = true) @RequestParam final Long clientId) {
    return Result.success(dataPushService.getOrCreateConfig(matterId, clientId));
  }

  /**
   * 更新推送配置
   *
   * @param matterId 项目ID
   * @param config 推送配置
   * @return 推送配置
   */
  @Operation(summary = "更新推送配置")
  @PutMapping("/config")
  @RequirePermission("matter:clientService:create")
  public Result<PushConfigDTO> updateConfig(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @RequestBody final PushConfigDTO config) {
    return Result.success(dataPushService.updateConfig(matterId, config));
  }

  /**
   * 获取推送统计
   *
   * @param matterId 项目ID
   * @return 推送统计信息
   */
  @Operation(summary = "获取推送统计", description = "获取项目的推送统计信息")
  @GetMapping("/statistics")
  @RequirePermission("matter:clientService:list")
  public Result<Map<String, Object>> getStatistics(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId) {
    return Result.success(dataPushService.getStatistics(matterId));
  }

  /**
   * 获取可推送的数据范围选项
   *
   * @return 数据范围选项列表
   */
  @Operation(summary = "获取可推送的数据范围选项")
  @GetMapping("/scopes")
  @RequirePermission("matter:clientService:list")
  public Result<List<ScopeOption>> getScopes() {
    return Result.success(
        List.of(
            new ScopeOption("MATTER_INFO", "项目基本信息", "项目名称、编号、类型、状态等"),
            new ScopeOption("MATTER_PROGRESS", "项目进度", "当前阶段、整体进度、最近更新时间"),
            new ScopeOption("LAWYER_INFO", "承办律师", "团队成员姓名、角色、联系方式（脱敏）"),
            new ScopeOption("DEADLINE_INFO", "关键期限", "诉讼时效、举证期限、开庭时间等"),
            new ScopeOption("TASK_LIST", "办理事项", "待办事项标题、状态、进度"),
            new ScopeOption("DOCUMENT_LIST", "文书目录", "文档名称列表（仅标题，不含文件）"),
            new ScopeOption("DOCUMENT_FILES", "文书文件", "推送选定的文档文件，客户可下载"),
            new ScopeOption("FEE_INFO", "费用信息", "合同金额、已收款、待收款")));
  }

  /**
   * 获取客户访问日志
   *
   * @param matterId 项目ID
   * @param clientId 客户ID（可选）
   * @param startTime 开始时间（可选）
   * @param endTime 结束时间（可选）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 分页结果
   */
  @Operation(summary = "获取客户访问日志", description = "查询客户访问项目链接的日志记录")
  @GetMapping("/access-logs")
  @RequirePermission("matter:clientService:list")
  public Result<PageResult<ClientAccessLogDTO>> getAccessLogs(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @Parameter(description = "客户ID") @RequestParam(required = false) final Long clientId,
      @Parameter(description = "开始时间") @RequestParam(required = false)
          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final LocalDateTime startTime,
      @Parameter(description = "结束时间") @RequestParam(required = false)
          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final LocalDateTime endTime,
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") final int pageSize) {
    return Result.success(
        clientAccessLogService.getAccessLogs(
            matterId, clientId, startTime, endTime, pageNum, pageSize));
  }

  /**
   * 获取客户下载日志
   *
   * @param matterId 项目ID
   * @param clientId 客户ID（可选）
   * @param fileId 文件ID（可选）
   * @param startTime 开始时间（可选）
   * @param endTime 结束时间（可选）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 分页结果
   */
  @Operation(summary = "获取客户下载日志", description = "查询客户下载文件的日志记录")
  @GetMapping("/download-logs")
  @RequirePermission("matter:clientService:list")
  public Result<PageResult<ClientDownloadLogDTO>> getDownloadLogs(
      @Parameter(description = "项目ID", required = true) @RequestParam final Long matterId,
      @Parameter(description = "客户ID") @RequestParam(required = false) final Long clientId,
      @Parameter(description = "文件ID") @RequestParam(required = false) final String fileId,
      @Parameter(description = "开始时间") @RequestParam(required = false)
          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final LocalDateTime startTime,
      @Parameter(description = "结束时间") @RequestParam(required = false)
          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final LocalDateTime endTime,
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") final int pageSize) {
    return Result.success(
        clientDownloadLogService.getDownloadLogs(
            matterId, clientId, fileId, startTime, endTime, pageNum, pageSize));
  }

  /**
   * 数据范围选项
   *
   * @param value 选项值
   * @param label 选项标签
   * @param description 选项描述
   */
  public record ScopeOption(String value, String label, String description) { }
}
