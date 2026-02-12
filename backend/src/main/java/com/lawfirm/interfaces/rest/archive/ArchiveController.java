package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.dto.ArchiveQueryDTO;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveCheckResult;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveDataSnapshot;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 档案管理 Controller */
@Slf4j
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class ArchiveController {

  /** HTTP状态码：404 Not Found */
  private static final int HTTP_STATUS_NOT_FOUND = 404;

  /** HTTP状态码：500 Internal Server Error */
  private static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  /** PDF MIME类型 */
  private static final String PDF_MIME_TYPE = "application/pdf";

  /** 默认档案名称 */
  private static final String DEFAULT_ARCHIVE_NAME = "档案";

  /** 卷宗封面后缀 */
  private static final String COVER_SUFFIX = "_卷宗封面.pdf";

  /** 档案应用服务 */
  private final ArchiveAppService archiveAppService;

  /** 审批人服务 */
  private final ApproverService approverService;

  /** MinIO服务 */
  private final MinioService minioService;

  /**
   * 分页查询档案列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("archive:list")
  public Result<PageResult<ArchiveDTO>> listArchives(final ArchiveQueryDTO query) {
    PageResult<ArchiveDTO> result = archiveAppService.listArchives(query);
    return Result.success(result);
  }

  /**
   * 获取档案详情
   *
   * @param id 档案ID
   * @return 档案信息
   */
  @GetMapping("/{id}")
  @RequirePermission("archive:list")
  public Result<ArchiveDTO> getArchive(@PathVariable final Long id) {
    ArchiveDTO archive = archiveAppService.getArchiveById(id);
    return Result.success(archive);
  }

  /**
   * 获取待归档案件列表
   *
   * @return 待归档案件列表
   */
  @GetMapping("/pending-matters")
  @RequirePermission("archive:create")
  public Result<List<Object>> getPendingMatters() {
    List<Object> matters = archiveAppService.getPendingMatters();
    return Result.success(matters);
  }

  /**
   * 归档预检查 检查项目是否满足归档条件
   *
   * @param matterId 案件ID
   * @return 检查结果
   */
  @GetMapping("/check/{matterId}")
  @RequirePermission("archive:create")
  @Operation(summary = "归档预检查")
  public Result<ArchiveCheckResult> checkArchiveRequirements(@PathVariable final Long matterId) {
    ArchiveCheckResult result = archiveAppService.checkArchiveRequirements(matterId);
    return Result.success(result);
  }

  /**
   * 预览归档数据 获取项目所有相关数据的预览
   *
   * @param matterId 案件ID
   * @return 归档数据快照
   */
  @GetMapping("/preview/{matterId}")
  @RequirePermission("archive:create")
  @Operation(summary = "预览归档数据")
  public Result<ArchiveDataSnapshot> previewArchiveData(@PathVariable final Long matterId) {
    ArchiveDataSnapshot snapshot = archiveAppService.previewArchiveData(matterId);
    return Result.success(snapshot);
  }

  /**
   * 获取可用的归档数据源配置
   *
   * @return 数据源配置列表
   */
  @GetMapping("/data-sources")
  @RequirePermission("archive:create")
  @Operation(summary = "获取归档数据源配置")
  public Result<List<Map<String, Object>>> getAvailableDataSources() {
    return Result.success(archiveAppService.getAvailableDataSources());
  }

  /**
   * 创建档案
   *
   * @param command 创建档案命令
   * @return 档案信息
   */
  @PostMapping
  @RequirePermission("archive:create")
  @OperationLog(module = "档案管理", action = "创建档案")
  public Result<ArchiveDTO> createArchive(@RequestBody @Valid final CreateArchiveCommand command) {
    ArchiveDTO archive = archiveAppService.createArchive(command);
    return Result.success(archive);
  }

  /**
   * 获取入库审批人列表 规则：优先显示主任（DIRECTOR），其次是团队负责人（TEAM_LEADER）
   *
   * @return 审批人列表
   */
  @GetMapping("/store/approvers")
  @RequirePermission("archive:create")
  @Operation(summary = "获取入库审批人列表", description = "获取可选的入库审批人，优先推荐主任")
  public Result<List<Map<String, Object>>> getStoreApprovers() {
    List<Map<String, Object>> approvers = approverService.getArchiveStoreAvailableApprovers();
    return Result.success(approvers);
  }

  /**
   * 提交入库审批
   *
   * @param id 档案ID
   * @return 空结果
   */
  @PostMapping("/{id}/submit-store")
  @RequirePermission("archive:create")
  @OperationLog(module = "档案管理", action = "提交入库审批")
  public Result<Void> submitStoreApproval(@PathVariable final Long id) {
    archiveAppService.submitStoreApproval(id);
    return Result.success();
  }

  /**
   * 审批入库
   *
   * @param id 档案ID
   * @param request 审批请求
   * @return 空结果
   */
  @PostMapping("/{id}/approve-store")
  @RequirePermission("archive:store:approve")
  @OperationLog(module = "档案管理", action = "审批入库")
  public Result<Void> approveStore(
      @PathVariable final Long id, @RequestBody final ApproveRequest request) {
    archiveAppService.approveStore(id, request.getApproved(), request.getComment());
    return Result.success();
  }

  /**
   * 档案入库（实际入库操作）
   *
   * @param command 入库命令
   * @return 空结果
   */
  @PostMapping("/store")
  @RequirePermission("archive:store")
  @OperationLog(module = "档案管理", action = "档案入库")
  public Result<Void> storeArchive(@RequestBody @Valid final StoreArchiveCommand command) {
    archiveAppService.storeArchive(command);
    return Result.success();
  }

  /**
   * 申请迁移档案
   *
   * @param id 档案ID
   * @param request 迁移请求
   * @return 空结果
   */
  @PostMapping("/{id}/apply-migrate")
  @RequirePermission("archive:migrate:apply")
  @OperationLog(module = "档案管理", action = "申请迁移")
  public Result<Void> applyMigrate(
      @PathVariable final Long id, @RequestBody final MigrateRequest request) {
    archiveAppService.applyMigrate(id, request.getReason(), request.getMigrateTarget());
    return Result.success();
  }

  /**
   * 审批迁移档案
   *
   * @param id 档案ID
   * @param request 审批迁移请求
   * @return 空结果
   */
  @PostMapping("/{id}/approve-migrate")
  @RequirePermission("archive:migrate:approve")
  @OperationLog(module = "档案管理", action = "审批迁移")
  public Result<Void> approveMigrate(
      @PathVariable final Long id, @RequestBody final ApproveMigrateRequest request) {
    archiveAppService.approveMigrate(
        id,
        request.getApproved(),
        request.getComment(),
        request.getDeleteFiles() != null && request.getDeleteFiles());
    return Result.success();
  }

  /**
   * 申请销毁档案（兼容旧接口，内部调用迁移）
   *
   * @param id 档案ID
   * @param request 销毁请求
   * @return 空结果
   * @deprecated 请使用 /apply-migrate
   */
  @PostMapping("/{id}/apply-destroy")
  @RequirePermission("archive:migrate:apply")
  @OperationLog(module = "档案管理", action = "申请销毁")
  @Deprecated
  public Result<Void> applyDestroy(
      @PathVariable final Long id, @RequestBody final DestroyRequest request) {
    archiveAppService.applyDestroy(id, request.getReason());
    return Result.success();
  }

  /**
   * 审批销毁档案（兼容旧接口，内部调用迁移审批）
   *
   * @param id 档案ID
   * @param request 审批销毁请求
   * @return 空结果
   * @deprecated 请使用 /approve-migrate
   */
  @PostMapping("/{id}/approve-destroy")
  @RequirePermission("archive:migrate:approve")
  @OperationLog(module = "档案管理", action = "审批销毁")
  @Deprecated
  public Result<Void> approveDestroy(
      @PathVariable final Long id, @RequestBody final ApproveDestroyRequest request) {
    archiveAppService.approveDestroy(id, request.getApproved(), request.getComment());
    return Result.success();
  }

  /**
   * 获取即将到期的档案（M7-041）
   *
   * @param days 天数
   * @return 即将到期的档案列表
   */
  @GetMapping("/expiring")
  @RequirePermission("archive:list")
  public Result<List<ArchiveDTO>> getExpiringArchives(
      @RequestParam(defaultValue = "90") final int days) {
    return Result.success(archiveAppService.getExpiringArchives(days));
  }

  /**
   * 按库位查看档案（M7-022）
   *
   * @param locationId 库位ID
   * @return 档案列表
   */
  @GetMapping("/location/{locationId}")
  @RequirePermission("archive:list")
  public Result<List<ArchiveDTO>> getArchivesByLocation(@PathVariable final Long locationId) {
    return Result.success(archiveAppService.getArchivesByLocation(locationId));
  }

  /**
   * 设置档案保管期限（M7-040）
   *
   * @param id 档案ID
   * @param request 设置保管期限请求
   * @return 档案信息
   */
  @PutMapping("/{id}/retention-period")
  @RequirePermission("archive:update")
  @OperationLog(module = "档案管理", action = "设置保管期限")
  public Result<ArchiveDTO> setRetentionPeriod(
      @PathVariable final Long id, @RequestBody final SetRetentionPeriodRequest request) {
    return Result.success(archiveAppService.setRetentionPeriod(id, request.getRetentionPeriod()));
  }

  /**
   * 销毁登记（M7-044）
   *
   * @param id 档案ID
   * @param request 销毁登记请求
   * @return 档案信息
   */
  @PostMapping("/{id}/register-destroy")
  @RequirePermission("archive:destroy")
  @OperationLog(module = "档案管理", action = "销毁登记")
  public Result<ArchiveDTO> registerDestroy(
      @PathVariable final Long id, @RequestBody final RegisterDestroyRequest request) {
    return Result.success(
        archiveAppService.registerDestroy(
            id, request.getDestroyMethod(), request.getDestroyLocation(), request.getWitness()));
  }

  /** 销毁请求 */
  @Data
  public static class DestroyRequest {
    /** 销毁原因 */
    private String reason;
  }

  /** 批准销毁请求 */
  @Data
  public static class ApproveDestroyRequest {
    /** 是否批准 */
    private Boolean approved;

    /** 审批意见 */
    private String comment;
  }

  /** 审批请求 */
  @Data
  public static class ApproveRequest {
    /** 是否批准 */
    private Boolean approved;

    /** 审批意见 */
    private String comment;
  }

  /** 迁移请求 */
  @Data
  public static class MigrateRequest {
    /** 迁移原因 */
    private String reason;

    /** 迁移目标 */
    private String migrateTarget;
  }

  /** 批准迁移请求 */
  @Data
  public static class ApproveMigrateRequest {
    /** 是否批准 */
    private Boolean approved;

    /** 审批意见 */
    private String comment;

    /** 是否删除文件 */
    private Boolean deleteFiles;
  }

  /** 设置保留期限请求 */
  @Data
  public static class SetRetentionPeriodRequest {
    /** 保留期限 */
    private String retentionPeriod;
  }

  /** 登记销毁请求 */
  @Data
  public static class RegisterDestroyRequest {
    /** 销毁方式 */
    private String destroyMethod;

    /** 销毁地点 */
    private String destroyLocation;

    /** 见证人 */
    private String witness;
  }

  /**
   * 下载卷宗封面
   *
   * @param id 档案ID
   * @param response HTTP响应
   */
  @GetMapping("/{id}/cover")
  @RequirePermission("archive:list")
  @Operation(summary = "下载卷宗封面")
  public void downloadCover(
      @PathVariable final Long id, final jakarta.servlet.http.HttpServletResponse response) {
    try {
      ArchiveDTO archive = archiveAppService.getArchiveById(id);
      if (archive.getElectronicUrl() == null || archive.getElectronicUrl().isEmpty()) {
        response.sendError(HTTP_STATUS_NOT_FOUND, "封面不存在");
        return;
      }

      // 从MinIO下载封面文件
      String objectName = minioService.extractObjectName(archive.getElectronicUrl());
      if (objectName == null) {
        response.sendError(HTTP_STATUS_NOT_FOUND, "封面文件路径无效");
        return;
      }

      byte[] coverBytes = minioService.downloadFileAsBytes(objectName);

      // 设置响应头
      response.setContentType(PDF_MIME_TYPE);
      response.setContentLength(coverBytes.length);
      String fileName =
          (archive.getArchiveName() != null ? archive.getArchiveName() : DEFAULT_ARCHIVE_NAME)
              + COVER_SUFFIX;
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
      response.setHeader(
          "Content-Disposition",
          "attachment; filename*=UTF-8''" + encodedFileName);

      // 写入文件内容
      response.getOutputStream().write(coverBytes);
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("下载卷宗封面失败: archiveId={}", id, e);
      try {
        response.sendError(HTTP_STATUS_INTERNAL_SERVER_ERROR, "下载封面失败: " + e.getMessage());
      } catch (Exception sendError) {
        log.debug("发送错误响应失败", sendError);
      }
    }
  }

  /**
   * 重新生成卷宗封面
   *
   * @param id 档案ID
   * @return 档案信息
   */
  @PostMapping("/{id}/regenerate-cover")
  @RequirePermission("archive:update")
  @OperationLog(module = "档案管理", action = "重新生成卷宗封面")
  @Operation(summary = "重新生成卷宗封面")
  public Result<ArchiveDTO> regenerateCover(@PathVariable final Long id) {
    ArchiveDTO archive = archiveAppService.regenerateCover(id);
    return Result.success(archive);
  }
}
