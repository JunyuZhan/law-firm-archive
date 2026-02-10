package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateSealApplicationCommand;
import com.lawfirm.application.document.dto.SealApplicationDTO;
import com.lawfirm.application.document.dto.SealApplicationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.SealApplicationStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.domain.document.entity.SealApplication;
import com.lawfirm.domain.document.repository.SealApplicationRepository;
import com.lawfirm.domain.document.repository.SealRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.SealApplicationMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 用印申请应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealApplicationAppService {

  /** 用印申请仓储. */
  private final SealApplicationRepository applicationRepository;

  /** 印章仓储. */
  private final SealRepository sealRepository;

  /** 用印申请Mapper. */
  private final SealApplicationMapper applicationMapper;

  /** 审批服务. */
  private final ApprovalService approvalService;

  /** 审批人服务. */
  private final ApproverService approverService;

  /** MinIO服务. */
  private final MinioService minioService;

  /** 印章每日最大使用次数 */
  private static final int MAX_DAILY_USAGE_COUNT = 50;

  /**
   * 分页查询用印申请。
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<SealApplicationDTO> listApplications(final SealApplicationQueryDTO query) {
    IPage<SealApplication> page =
        applicationMapper.selectApplicationPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getApplicantId(),
            query.getSealId(),
            query.getMatterId(),
            query.getStatus(),
            query.getKeeperId());

    List<SealApplicationDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建用印申请。
   *
   * @param command 创建命令
   * @return 用印申请DTO
   */
  @Transactional
  public SealApplicationDTO createApplication(final CreateSealApplicationCommand command) {
    // 验证印章
    Seal seal = sealRepository.getByIdOrThrow(command.getSealId(), "印章不存在");
    if (!SealApplicationStatus.SEAL_ACTIVE.equals(seal.getStatus())) {
      throw new BusinessException("印章不可用");
    }

    // 验证用印份数
    Integer copies = command.getCopies() != null ? command.getCopies() : 1;
    if (copies <= 0) {
      throw new BusinessException("用印份数必须大于0");
    }
    if (copies > 100) {
      throw new BusinessException("单次用印份数不能超过100份，如有特殊需求请分多次申请");
    }

    // 可选: 检查该印章的使用频率 (防止滥用)
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SealApplication> wrapper =
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
    wrapper
        .eq(SealApplication::getSealId, command.getSealId())
        .ge(SealApplication::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        .ne(SealApplication::getStatus, SealApplicationStatus.CANCELLED)
        .eq(SealApplication::getDeleted, false);
    long todayUsageCount = applicationRepository.count(wrapper);

    if (todayUsageCount >= MAX_DAILY_USAGE_COUNT) {
      log.warn("印章{}今日使用次数过多: {}次", seal.getName(), todayUsageCount);
    }

    String applicationNo = generateApplicationNo();
    Long userId = SecurityUtils.getUserId();

    SealApplication application =
        SealApplication.builder()
            .applicationNo(applicationNo)
            .applicantId(userId)
            .applicantName(SecurityUtils.getUsername())
            .sealId(command.getSealId())
            .sealName(seal.getName())
            .matterId(command.getMatterId())
            .documentName(command.getDocumentName())
            .documentType(command.getDocumentType())
            .copies(copies)
            .usePurpose(command.getUsePurpose())
            .expectedUseDate(command.getExpectedUseDate())
            .status(SealApplicationStatus.PENDING)
            .build();

    applicationRepository.save(application);

    // 验证审批人是否存在且有审批权限
    Long approverId = command.getApproverId();
    if (approverId == null) {
      throw new BusinessException("审批人不能为空");
    }

    // 验证审批人是否在可选审批人列表中（确保审批人有效）
    List<Map<String, Object>> availableApprovers =
        approverService.getSealApplicationAvailableApprovers(userId);
    boolean isValidApprover =
        availableApprovers.stream().anyMatch(approver -> approverId.equals(approver.get("id")));
    if (!isValidApprover) {
      throw new BusinessException("选择的审批人无效，请从可选审批人列表中选择");
    }

    // 创建审批记录（使用用户选择的审批人）
    approvalService.createApproval(
        "SEAL_APPLICATION",
        application.getId(),
        application.getApplicationNo(),
        application.getDocumentName(),
        approverId,
        "MEDIUM",
        "NORMAL",
        null // businessSnapshot
        );

    log.info(
        "用印申请创建成功: {} ({}) (审批人: {})",
        application.getDocumentName(),
        application.getApplicationNo(),
        approverId);
    return toDTO(application);
  }

  /**
   * 获取申请详情。
   *
   * @param id 申请ID
   * @return 用印申请DTO
   */
  public SealApplicationDTO getApplicationById(final Long id) {
    SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
    return toDTO(application);
  }

  /**
   * 审批通过。
   *
   * @param id 申请ID
   * @param comment 审批意见
   * @return 用印申请DTO
   */
  @Transactional
  public SealApplicationDTO approve(final Long id, final String comment) {
    SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");

    if (!SealApplicationStatus.PENDING.equals(application.getStatus())) {
      throw new BusinessException("只能审批待审批的申请");
    }

    application.setStatus(SealApplicationStatus.APPROVED);
    application.setApprovedBy(SecurityUtils.getUserId());
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovalComment(comment);

    applicationRepository.updateById(application);
    log.info("用印申请审批通过: {}", application.getApplicationNo());
    return toDTO(application);
  }

  /**
   * 审批拒绝。
   *
   * @param id 申请ID
   * @param comment 审批意见
   * @return 用印申请DTO
   */
  @Transactional
  public SealApplicationDTO reject(final Long id, final String comment) {
    SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");

    if (!SealApplicationStatus.PENDING.equals(application.getStatus())) {
      throw new BusinessException("只能审批待审批的申请");
    }

    application.setStatus(SealApplicationStatus.REJECTED);
    application.setApprovedBy(SecurityUtils.getUserId());
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovalComment(comment);

    applicationRepository.updateById(application);
    log.info("用印申请审批拒绝: {}", application.getApplicationNo());
    return toDTO(application);
  }

  /**
   * 登记用印（仅保管人可以操作）。
   *
   * @param id 申请ID
   * @param remark 用印备注
   * @return 用印申请DTO
   */
  @Transactional
  public SealApplicationDTO registerUsage(final Long id, final String remark) {
    SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 检查是否已经登记过用印，防止重复登记
    if (SealApplicationStatus.USED.equals(application.getStatus())) {
      throw new BusinessException("该申请已经登记过用印，不能重复登记");
    }

    if (!SealApplicationStatus.APPROVED.equals(application.getStatus())) {
      throw new BusinessException("只能对已批准的申请登记用印");
    }

    // 验证当前用户是否是印章保管人
    validateKeeperPermission(id);

    application.setStatus(SealApplicationStatus.USED);
    application.setUsedBy(SecurityUtils.getUserId());
    application.setUsedAt(LocalDateTime.now());
    application.setActualUseDate(LocalDate.now());
    application.setUseRemark(remark);

    applicationRepository.updateById(application);
    log.info("用印登记成功: {} (保管人: {})", application.getApplicationNo(), SecurityUtils.getUserId());
    return toDTO(application);
  }

  /**
   * 取消申请。
   *
   * @param id 申请ID
   */
  @Transactional
  public void cancelApplication(final Long id) {
    SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");

    if (!SealApplicationStatus.PENDING.equals(application.getStatus())) {
      throw new BusinessException("只能取消待审批的申请");
    }

    // 验证是否是申请人本人
    if (!application.getApplicantId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能取消自己的申请");
    }

    application.setStatus(SealApplicationStatus.CANCELLED);
    applicationRepository.updateById(application);
    log.info("用印申请已取消: {}", application.getApplicationNo());
  }

  /**
   * 获取待审批列表。
   *
   * @return 待审批申请列表
   */
  public List<SealApplicationDTO> getPendingApplications() {
    List<SealApplication> applications = applicationRepository.findPendingApplications();
    return applications.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取保管人待办理的申请（审批通过且印章的保管人是当前用户）。
   *
   * @param keeperId 保管人ID
   * @return 待办理申请列表
   */
  public List<SealApplicationDTO> getPendingForKeeper(final Long keeperId) {
    List<SealApplication> applications = applicationMapper.selectPendingForKeeper(keeperId);
    return applications.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取保管人已办理的申请（已用印且印章的保管人是当前用户）。
   *
   * @param keeperId 保管人ID
   * @return 已办理申请列表
   */
  public List<SealApplicationDTO> getProcessedByKeeper(final Long keeperId) {
    List<SealApplication> applications = applicationMapper.selectProcessedByKeeper(keeperId);
    return applications.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 检查当前用户是否是某个印章的保管人。
   *
   * @param sealId 印章ID
   * @return 是否是保管人
   */
  public boolean isKeeperOfSeal(final Long sealId) {
    Long userId = SecurityUtils.getUserId();
    Seal seal = sealRepository.getById(sealId);
    return seal != null && userId.equals(seal.getKeeperId());
  }

  /**
   * 检查当前用户是否是任何印章的保管人。
   *
   * @return 是否是任何印章的保管人
   */
  public boolean isAnySealKeeper() {
    Long userId = SecurityUtils.getUserId();
    // 查询是否有印章的保管人是当前用户
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seal> wrapper =
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
    wrapper.eq(Seal::getKeeperId, userId).eq(Seal::getDeleted, false);
    return sealRepository.count(wrapper) > 0;
  }

  /**
   * 验证当前用户是否是申请中印章的保管人。
   *
   * @param applicationId 申请ID
   */
  public void validateKeeperPermission(final Long applicationId) {
    SealApplication application = applicationRepository.getByIdOrThrow(applicationId, "申请不存在");
    Seal seal = sealRepository.getByIdOrThrow(application.getSealId(), "印章不存在");
    Long userId = SecurityUtils.getUserId();

    if (!userId.equals(seal.getKeeperId())) {
      throw new BusinessException("您不是该印章的保管人，无权操作");
    }
  }

  /**
   * 生成申请编号。
   *
   * @return 申请编号
   */
  private String generateApplicationNo() {
    String prefix = "SA" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取状态名称。
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    return SealApplicationStatus.getStatusName(status);
  }

  /**
   * Entity 转 DTO。
   *
   * @param app 用印申请实体
   * @return 用印申请DTO
   */
  private SealApplicationDTO toDTO(final SealApplication app) {
    SealApplicationDTO dto = new SealApplicationDTO();
    dto.setId(app.getId());
    dto.setApplicationNo(app.getApplicationNo());
    dto.setApplicantId(app.getApplicantId());
    dto.setApplicantName(app.getApplicantName());
    dto.setDepartmentId(app.getDepartmentId());
    dto.setSealId(app.getSealId());
    dto.setSealName(app.getSealName());
    dto.setMatterId(app.getMatterId());
    dto.setMatterName(app.getMatterName());
    dto.setDocumentName(app.getDocumentName());
    dto.setDocumentType(app.getDocumentType());
    dto.setCopies(app.getCopies());
    dto.setUsePurpose(app.getUsePurpose());
    dto.setExpectedUseDate(app.getExpectedUseDate());
    dto.setActualUseDate(app.getActualUseDate());
    dto.setStatus(app.getStatus());
    dto.setStatusName(getStatusName(app.getStatus()));
    dto.setApprovedBy(app.getApprovedBy());
    dto.setApprovedAt(app.getApprovedAt());
    dto.setApprovalComment(app.getApprovalComment());
    dto.setUsedBy(app.getUsedBy());
    dto.setUsedAt(app.getUsedAt());
    dto.setUseRemark(app.getUseRemark());
    dto.setAttachmentUrl(app.getAttachmentUrl());
    dto.setBucketName(app.getBucketName());
    dto.setStoragePath(app.getStoragePath());
    dto.setPhysicalName(app.getPhysicalName());
    dto.setFileHash(app.getFileHash());
    dto.setCreatedAt(app.getCreatedAt());
    dto.setUpdatedAt(app.getUpdatedAt());
    return dto;
  }

  /**
   * 上传用印附件文件
   *
   * @param file 文件
   * @param applicationId 用印申请ID
   * @return 文件URL
   */
  @Transactional
  public String uploadAttachmentFile(final MultipartFile file, final Long applicationId) {
    // 文件安全校验
    FileValidator.ValidationResult validationResult = FileValidator.validate(file);
    if (!validationResult.isValid()) {
      throw new BusinessException(validationResult.getErrorMessage());
    }

    SealApplication application = applicationRepository.getByIdOrThrow(applicationId, "用印申请不存在");

    // 使用FileAccessService上传文件
    String storagePath =
        MinioPathGenerator.generateStandardPath(
            MinioPathGenerator.FileType.SEAL, application.getMatterId(), "用印附件");

    // 生成物理文件名
    String originalFilename = file.getOriginalFilename();
    String physicalName = MinioPathGenerator.generatePhysicalName(originalFilename);
    String objectName = MinioPathGenerator.buildObjectName(storagePath, physicalName);

    try {
      // 计算文件Hash
      String fileHash = com.lawfirm.common.util.FileHashUtil.calculateHash(file);

      // 上传到MinIO
      String fileUrl =
          minioService.uploadFile(file.getInputStream(), objectName, file.getContentType());

      // 设置存储信息
      application.setAttachmentUrl(fileUrl);
      application.setBucketName(minioService.getBucketName());
      application.setStoragePath(storagePath);
      application.setPhysicalName(physicalName);
      application.setFileHash(fileHash);

      applicationRepository.updateById(application);

      log.info(
          "用印附件文件上传成功: applicationId={}, fileName={}, storagePath={}",
          applicationId,
          originalFilename,
          storagePath);

      return fileUrl;
    } catch (Exception e) {
      log.error("用印附件文件上传失败", e);
      throw new BusinessException("文件上传失败: " + e.getMessage());
    }
  }
}
