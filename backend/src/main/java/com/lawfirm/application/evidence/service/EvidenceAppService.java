package com.lawfirm.application.evidence.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceCrossExam;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.file.FileTypeService;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceCrossExamMapper;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 证据应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceAppService {

  /** 证据仓储 */
  private final EvidenceRepository evidenceRepository;

  /** 证据Mapper */
  private final EvidenceMapper evidenceMapper;

  /** 质证Mapper */
  private final EvidenceCrossExamMapper crossExamMapper;

  /** 文件类型服务 */
  private final FileTypeService fileTypeService;

  /** MinIO服务 */
  private final MinioService minioService;

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** 项目应用服务 */
  private MatterAppService matterAppService;

  /**
   * 设置项目应用服务.
   *
   * @param matterAppService 项目应用服务
   */
  @org.springframework.beans.factory.annotation.Autowired
  @Lazy
  public void setMatterAppService(final MatterAppService matterAppService) {
    this.matterAppService = matterAppService;
  }

  /** 不可编辑证据的项目状态 */
  private static final Set<String> READONLY_MATTER_STATUSES = Set.of("ARCHIVED", "CLOSED");

  /** 最大排序号 */
  private static final int MAX_SORT_ORDER = 9999;

  /** 1KB字节数 */
  private static final int KB_BYTES = 1024;

  /** 1MB字节数 */
  private static final int MB_BYTES = 1024 * 1024;

  /**
   * 分页查询证据.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<EvidenceDTO> listEvidence(final EvidenceQueryDTO query) {
    // 根据用户权限过滤数据
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    // 获取可访问的项目ID列表
    List<Long> accessibleMatterIds =
        matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    // 如果返回空列表，表示没有权限，返回空结果
    if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    // 如果query中指定了matterId，需要验证是否有权限访问该项目
    if (query.getMatterId() != null && accessibleMatterIds != null) {
      if (!accessibleMatterIds.contains(query.getMatterId())) {
        // 没有权限访问指定的项目，返回空结果
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
      }
    }

    IPage<Evidence> page =
        evidenceMapper.selectEvidencePage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getMatterId(),
            query.getName(),
            query.getEvidenceType(),
            query.getGroupName(),
            query.getCrossExamStatus(),
            accessibleMatterIds // null表示可以访问所有项目的证据（ALL权限）
            );

    List<EvidenceDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建证据.
   *
   * @param command 创建命令
   * @return 证据DTO
   */
  @Transactional
  public EvidenceDTO createEvidence(final CreateEvidenceCommand command) {
    // 检查项目状态权限
    checkMatterEditPermission(command.getMatterId());

    String evidenceNo = generateEvidenceNo();

    // 获取排序号
    Integer maxSort =
        evidenceRepository.getMaxSortOrder(command.getMatterId(), command.getGroupName());
    int sortOrder = (maxSort != null ? maxSort : 0) + 1;

    // 处理文件信息（如果从上传接口获取了新字段）
    // 注意：前端上传时返回的新字段需要传递到CreateEvidenceCommand
    // 当前CreateEvidenceCommand没有新字段，需要从fileUrl解析或前端传递

    Evidence evidence =
        Evidence.builder()
            .evidenceNo(evidenceNo)
            .matterId(command.getMatterId())
            .name(command.getName())
            .evidenceType(command.getEvidenceType())
            .source(command.getSource())
            .groupName(command.getGroupName())
            .sortOrder(sortOrder)
            .provePurpose(command.getProvePurpose())
            .description(command.getDescription())
            .isOriginal(command.getIsOriginal() != null ? command.getIsOriginal() : false)
            .originalCount(command.getOriginalCount() != null ? command.getOriginalCount() : 0)
            .copyCount(command.getCopyCount() != null ? command.getCopyCount() : 0)
            .pageStart(command.getPageStart())
            .pageEnd(command.getPageEnd())
            // 新字段（如果fileUrl包含新路径信息，需要解析）
            // 注意：当前前端可能还未传递新字段，这里先保持兼容
            .fileUrl(command.getFileUrl())
            .fileName(command.getFileName())
            .fileSize(command.getFileSize())
            .fileType(command.getFileType())
            .thumbnailUrl(command.getThumbnailUrl())
            .documentId(command.getDocumentId()) // 卷宗文件引用
            .crossExamStatus("PENDING")
            .status("ACTIVE")
            .build();

    // 如果fileUrl存在，尝试从URL解析新字段（兼容处理）
    if (command.getFileUrl() != null && !command.getFileUrl().isEmpty()) {
      String objectName = minioService.extractObjectName(command.getFileUrl());
      if (objectName != null) {
        // 检查是否是新路径格式：evidence/M_{matterId}/{YYYY-MM}/{folder}/{YYYYMMDD_uuid_filename}
        if (objectName.startsWith("evidence/M_")) {
          // 解析新路径
          String[] parts = objectName.split("/");
          // 路径格式: evidence/M_{matterId}/{YYYY-MM}/{folder}/{physicalName}
          // 需要至少5个部分才能正确解析
          if (parts.length >= 5) {
            String folder = parts[3];
            String physicalName = parts[parts.length - 1];
            String storagePath = String.join("/", parts[0], parts[1], parts[2], folder) + "/";

            // 验证存储路径格式（确保包含项目ID）
            MinioPathGenerator.validateStoragePath(
                storagePath, MinioPathGenerator.FileType.EVIDENCE, command.getMatterId());

            evidence.setBucketName(minioService.getBucketName());
            evidence.setStoragePath(storagePath);
            evidence.setPhysicalName(physicalName);
            // fileHash需要从上传接口获取，这里暂时不设置
          }
        }
      }
    }

    evidenceRepository.save(evidence);
    log.info("证据创建成功: {} ({})", evidence.getName(), evidence.getEvidenceNo());
    return toDTO(evidence);
  }

  /**
   * 获取证据详情 ✅ 修复问题577: 添加权限验证.
   *
   * @param id 证据ID
   * @return 证据DTO
   */
  public EvidenceDTO getEvidenceById(final Long id) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
    return toDTO(evidence);
  }

  /**
   * 获取证据实体（用于FileAccessService等需要实体对象的场景）.
   *
   * @param id 证据ID
   * @return 证据实体
   */
  public Evidence getEvidenceEntityById(final Long id) {
    return evidenceRepository.getByIdOrThrow(id, "证据不存在");
  }

  /**
   * 更新证据.
   *
   * @param id 证据ID
   * @param command 更新命令
   * @return 证据DTO
   */
  @Transactional
  public EvidenceDTO updateEvidence(final Long id, final UpdateEvidenceCommand command) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");

    // 检查项目状态权限
    checkMatterEditPermission(evidence.getMatterId());

    if (StringUtils.hasText(command.getName())) {
      evidence.setName(command.getName());
    }
    if (StringUtils.hasText(command.getEvidenceType())) {
      evidence.setEvidenceType(command.getEvidenceType());
    }
    if (command.getSource() != null) {
      evidence.setSource(command.getSource());
    }
    if (command.getGroupName() != null) {
      evidence.setGroupName(command.getGroupName());
    }
    if (command.getProvePurpose() != null) {
      evidence.setProvePurpose(command.getProvePurpose());
    }
    if (command.getDescription() != null) {
      evidence.setDescription(command.getDescription());
    }
    if (command.getIsOriginal() != null) {
      evidence.setIsOriginal(command.getIsOriginal());
    }
    if (command.getOriginalCount() != null) {
      evidence.setOriginalCount(command.getOriginalCount());
    }
    if (command.getCopyCount() != null) {
      evidence.setCopyCount(command.getCopyCount());
    }
    if (command.getPageStart() != null) {
      evidence.setPageStart(command.getPageStart());
    }
    if (command.getPageEnd() != null) {
      evidence.setPageEnd(command.getPageEnd());
    }

    evidenceRepository.updateById(evidence);
    log.info("证据更新成功: {}", evidence.getName());
    return toDTO(evidence);
  }

  /**
   * 删除证据.
   *
   * @param id 证据ID
   */
  @Transactional
  public void deleteEvidence(final Long id) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");

    // 检查项目状态权限
    checkMatterEditPermission(evidence.getMatterId());

    evidenceRepository.removeById(id);
    log.info("证据删除成功: {}", evidence.getName());
  }

  /**
   * 调整证据排序.
   *
   * @param id 证据ID
   * @param sortOrder 排序号
   */
  @Transactional
  public void updateSortOrder(final Long id, final Integer sortOrder) {
    // ✅ 参数验证
    if (sortOrder == null || sortOrder < 0) {
      throw new BusinessException("排序号不能为空且必须大于等于0");
    }
    if (sortOrder > MAX_SORT_ORDER) {
      throw new BusinessException("排序号不能超过" + MAX_SORT_ORDER);
    }

    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");

    // 检查项目状态权限
    checkMatterEditPermission(evidence.getMatterId());

    evidence.setSortOrder(sortOrder);
    evidenceRepository.updateById(evidence);
  }

  /**
   * 批量调整分组. 问题295修复：先验证所有证据，再批量更新，确保原子操作
   *
   * @param ids 证据ID列表
   * @param groupName 分组名称
   */
  @Transactional
  public void batchUpdateGroup(final List<Long> ids, final String groupName) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException("请选择要分组的证据");
    }

    // 第1阶段：批量查询所有证据（避免N+1查询）
    List<Evidence> evidences = evidenceRepository.listByIds(ids);
    if (evidences.size() != ids.size()) {
      // 找出缺失的证据ID
      Set<Long> foundIds =
          evidences.stream().map(Evidence::getId).collect(java.util.stream.Collectors.toSet());
      List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new BusinessException("证据不存在: " + missingIds);
    }

    // 验证所有证据的项目权限（批量收集项目ID，减少重复检查）
    Set<Long> checkedMatterIds = new java.util.HashSet<>();
    for (Evidence evidence : evidences) {
      if (!checkedMatterIds.contains(evidence.getMatterId())) {
        checkMatterEditPermission(evidence.getMatterId());
        checkedMatterIds.add(evidence.getMatterId());
      }
    }

    // 第2阶段：批量更新（验证全部通过后）
    evidences.forEach(e -> e.setGroupName(groupName));
    evidenceRepository.updateBatchById(evidences);

    log.info("批量调整证据分组成功，共{}条", ids.size());
  }

  /**
   * 添加质证记录.
   *
   * @param command 质证命令
   * @return 质证DTO
   */
  @Transactional
  public EvidenceCrossExamDTO addCrossExam(final CreateCrossExamCommand command) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(command.getEvidenceId(), "证据不存在");

    // ✅ 检查项目状态权限（已归档或已结案的项目不允许添加质证）
    checkMatterEditPermission(evidence.getMatterId());

    // 检查是否已有该方的质证记录
    EvidenceCrossExam existing =
        crossExamMapper.selectByEvidenceIdAndParty(command.getEvidenceId(), command.getExamParty());
    if (existing != null) {
      throw new BusinessException("该方已有质证记录，请编辑现有记录");
    }

    EvidenceCrossExam crossExam =
        EvidenceCrossExam.builder()
            .evidenceId(command.getEvidenceId())
            .examParty(command.getExamParty())
            .authenticityOpinion(command.getAuthenticityOpinion())
            .authenticityReason(command.getAuthenticityReason())
            .legalityOpinion(command.getLegalityOpinion())
            .legalityReason(command.getLegalityReason())
            .relevanceOpinion(command.getRelevanceOpinion())
            .relevanceReason(command.getRelevanceReason())
            .overallOpinion(command.getOverallOpinion())
            .courtOpinion(command.getCourtOpinion())
            .courtAccepted(command.getCourtAccepted())
            .createdBy(SecurityUtils.getUserId())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    crossExamMapper.insert(crossExam);

    // 更新证据质证状态
    evidence.setCrossExamStatus("IN_PROGRESS");
    evidenceRepository.updateById(evidence);

    log.info("质证记录添加成功: 证据{}, 质证方{}", evidence.getName(), command.getExamParty());
    return toCrossExamDTO(crossExam);
  }

  /**
   * 完成质证.
   *
   * @param evidenceId 证据ID
   */
  @Transactional
  public void completeCrossExam(final Long evidenceId) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(evidenceId, "证据不存在");

    // ✅ 验证项目编辑权限（只有项目成员才能完成质证）
    checkMatterEditPermission(evidence.getMatterId());

    evidence.setCrossExamStatus("COMPLETED");
    evidenceRepository.updateById(evidence);
    log.info("证据质证完成: {}, 操作人: {}", evidence.getName(), SecurityUtils.getUserId());
  }

  /**
   * 按案件获取证据列表.
   *
   * @param matterId 项目ID
   * @return 证据列表
   */
  public List<EvidenceDTO> getEvidenceByMatter(final Long matterId) {
    // ✅ 验证项目访问权限
    validateMatterAccess(matterId);

    List<Evidence> evidences = evidenceRepository.findByMatterId(matterId);
    return evidences.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取案件的证据分组.
   *
   * @param matterId 项目ID
   * @return 分组列表
   */
  public List<String> getEvidenceGroups(final Long matterId) {
    // ✅ 验证项目访问权限
    validateMatterAccess(matterId);

    return evidenceRepository.findGroupsByMatterId(matterId);
  }

  /**
   * 生成证据编号.
   *
   * @return 证据编号
   */
  /**
   * 生成证据编号.
   *
   * @return 证据编号
   */
  private String generateEvidenceNo() {
    return com.lawfirm.common.util.NumberGenerator.generateEvidenceNo();
  }

  /**
   * 检查项目编辑权限.
   *
   * <p>已归档或已结案的项目不允许编辑证据
   *
   * @param matterId 项目ID
   */
  private void checkMatterEditPermission(final Long matterId) {
    if (matterId == null) {
      return;
    }

    // 验证用户是否是项目负责人或参与者（只有项目成员才能编辑证据）
    matterAppService.validateMatterOwnership(matterId);

    Matter matter = matterRepository.findById(matterId);
    if (matter != null && READONLY_MATTER_STATUSES.contains(matter.getStatus())) {
      String statusName = "ARCHIVED".equals(matter.getStatus()) ? "已归档" : "已结案";
      throw new BusinessException("该项目" + statusName + "，无法编辑证据");
    }
  }

  /**
   * 验证项目访问权限.
   *
   * @param matterId 项目ID
   */
  private void validateMatterAccess(final Long matterId) {
    if (matterId == null) {
      return;
    }

    String dataScope = SecurityUtils.getDataScope();
    // ALL权限可以访问所有项目
    if ("ALL".equals(dataScope)) {
      return;
    }

    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    List<Long> accessibleMatterIds =
        matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    // 如果返回null表示ALL权限，可以访问所有
    if (accessibleMatterIds == null) {
      return;
    }

    // 检查是否有权限访问
    if (!accessibleMatterIds.contains(matterId)) {
      throw new BusinessException("权限不足：无法访问该项目的证据");
    }
  }

  /**
   * 检查项目是否可编辑 ✅ 修复问题602: 添加用户权限检查
   *
   * @param matterId 项目ID
   * @return 是否可编辑
   */
  public boolean canEditEvidence(final Long matterId) {
    if (matterId == null) {
      return true;
    }

    // ✅ 检查用户是否有权限访问该项目
    try {
      validateMatterAccess(matterId);
    } catch (BusinessException e) {
      return false; // 没有访问权限
    }

    Matter matter = matterRepository.findById(matterId);
    return matter == null || !READONLY_MATTER_STATUSES.contains(matter.getStatus());
  }

  /**
   * 格式化文件大小
   *
   * @param size 文件大小（字节）
   * @return 格式化后的文件大小字符串
   */
  private String formatFileSize(final Long size) {
    if (size == null) {
      return null;
    }
    if (size < KB_BYTES) {
      return size + " B";
    }
    if (size < MB_BYTES) {
      return String.format("%.1f KB", size / (double) KB_BYTES);
    }
    return String.format("%.1f MB", size / (double) MB_BYTES);
  }

  /**
   * 获取文件类型
   *
   * @param fileName 文件名
   * @return 文件类型
   */
  private String getFileType(final String fileName) {
    return fileTypeService.getFileTypeInfo(fileName).getType();
  }

  /**
   * 判断是否为图片文件
   *
   * @param fileName 文件名
   * @return 是否为图片文件
   */
  private boolean isImageFile(final String fileName) {
    return fileTypeService.isImageFile(fileName);
  }

  /**
   * 获取证据类型名称
   *
   * @param type 证据类型
   * @return 类型名称
   */
  private String getEvidenceTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "DOCUMENTARY" -> "书证";
      case "PHYSICAL" -> "物证";
      case "AUDIO_VISUAL" -> "视听资料";
      case "ELECTRONIC" -> "电子数据";
      case "WITNESS" -> "证人证言";
      case "EXPERT" -> "鉴定意见";
      case "INSPECTION" -> "勘验笔录";
      default -> type;
    };
  }

  /**
   * 获取质证状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getCrossExamStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "PENDING" -> "待质证";
      case "IN_PROGRESS" -> "质证中";
      case "COMPLETED" -> "已质证";
      default -> status;
    };
  }

  /**
   * 获取质证方名称
   *
   * @param party 质证方
   * @return 质证方名称
   */
  private String getExamPartyName(final String party) {
    if (party == null) {
      return null;
    }
    return switch (party) {
      case "OUR_SIDE" -> "我方";
      case "OPPOSITE" -> "对方";
      case "COURT" -> "法院";
      default -> party;
    };
  }

  /**
   * 获取意见名称
   *
   * @param opinion 意见
   * @return 意见名称
   */
  private String getOpinionName(final String opinion) {
    if (opinion == null) {
      return null;
    }
    return switch (opinion) {
      case "ACCEPT" -> "认可";
      case "PARTIAL" -> "部分认可";
      case "REJECT" -> "不认可";
      default -> opinion;
    };
  }

  /**
   * Entity 转 DTO
   *
   * @param evidence 证据实体
   * @return 证据DTO
   */
  /**
   * 转换为DTO.
   *
   * @param evidence 证据实体
   * @return 证据DTO
   */
  private EvidenceDTO toDTO(final Evidence evidence) {
    EvidenceDTO dto = new EvidenceDTO();
    dto.setId(evidence.getId());
    dto.setEvidenceNo(evidence.getEvidenceNo());
    dto.setMatterId(evidence.getMatterId());
    dto.setName(evidence.getName());
    dto.setEvidenceType(evidence.getEvidenceType());
    dto.setEvidenceTypeName(getEvidenceTypeName(evidence.getEvidenceType()));
    dto.setSource(evidence.getSource());
    dto.setGroupName(evidence.getGroupName());
    dto.setSortOrder(evidence.getSortOrder());
    dto.setProvePurpose(evidence.getProvePurpose());
    dto.setDescription(evidence.getDescription());
    dto.setIsOriginal(evidence.getIsOriginal());
    dto.setOriginalCount(evidence.getOriginalCount());
    dto.setCopyCount(evidence.getCopyCount());
    dto.setPageStart(evidence.getPageStart());
    dto.setPageEnd(evidence.getPageEnd());
    if (evidence.getPageStart() != null && evidence.getPageEnd() != null) {
      dto.setPageRange(evidence.getPageStart() + "-" + evidence.getPageEnd());
    }
    dto.setFileUrl(evidence.getFileUrl());
    dto.setFileName(evidence.getFileName());
    dto.setFileSize(evidence.getFileSize());
    dto.setFileSizeDisplay(formatFileSize(evidence.getFileSize()));
    // 设置文件类型（优先使用存储的值，否则根据文件名判断）
    if (evidence.getFileType() != null) {
      dto.setFileType(evidence.getFileType());
    } else if (evidence.getFileName() != null) {
      dto.setFileType(getFileType(evidence.getFileName()));
    }
    // 设置缩略图URL（优先使用存储的值，否则对图片使用原文件URL）
    // 转换缩略图 URL 为浏览器可访问的 URL
    if (evidence.getThumbnailUrl() != null) {
      dto.setThumbnailUrl(minioService.getBrowserAccessibleUrl(evidence.getThumbnailUrl()));
    } else if (isImageFile(evidence.getFileName()) && evidence.getFileUrl() != null) {
      dto.setThumbnailUrl(minioService.getBrowserAccessibleUrl(evidence.getFileUrl()));
    }
    dto.setCrossExamStatus(evidence.getCrossExamStatus());
    dto.setCrossExamStatusName(getCrossExamStatusName(evidence.getCrossExamStatus()));
    dto.setStatus(evidence.getStatus());
    dto.setCreatedBy(evidence.getCreatedBy());
    dto.setCreatedAt(evidence.getCreatedAt());
    dto.setUpdatedAt(evidence.getUpdatedAt());
    return dto;
  }

  /**
   * 转换为质证DTO.
   *
   * @param crossExam 质证实体
   * @return 质证DTO
   */
  private EvidenceCrossExamDTO toCrossExamDTO(final EvidenceCrossExam crossExam) {
    EvidenceCrossExamDTO dto = new EvidenceCrossExamDTO();
    dto.setId(crossExam.getId());
    dto.setEvidenceId(crossExam.getEvidenceId());
    dto.setExamParty(crossExam.getExamParty());
    dto.setExamPartyName(getExamPartyName(crossExam.getExamParty()));
    dto.setAuthenticityOpinion(crossExam.getAuthenticityOpinion());
    dto.setAuthenticityOpinionName(getOpinionName(crossExam.getAuthenticityOpinion()));
    dto.setAuthenticityReason(crossExam.getAuthenticityReason());
    dto.setLegalityOpinion(crossExam.getLegalityOpinion());
    dto.setLegalityOpinionName(getOpinionName(crossExam.getLegalityOpinion()));
    dto.setLegalityReason(crossExam.getLegalityReason());
    dto.setRelevanceOpinion(crossExam.getRelevanceOpinion());
    dto.setRelevanceOpinionName(getOpinionName(crossExam.getRelevanceOpinion()));
    dto.setRelevanceReason(crossExam.getRelevanceReason());
    dto.setOverallOpinion(crossExam.getOverallOpinion());
    dto.setCourtOpinion(crossExam.getCourtOpinion());
    dto.setCourtAccepted(crossExam.getCourtAccepted());
    dto.setCreatedBy(crossExam.getCreatedBy());
    dto.setCreatedAt(crossExam.getCreatedAt());
    return dto;
  }
}
