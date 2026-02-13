package com.lawfirm.application.evidence.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.evidence.command.CreateEvidenceListCommand;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceListCompareResult;
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import com.lawfirm.domain.evidence.repository.EvidenceListRepository;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.document.EvidenceListDocumentGenerator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceListMapper;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 证据清单应用服务 ✅ 修复问题579-600: 添加权限验证和N+1查询优化 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceListAppService {

  /** 证据清单仓储 */
  private final EvidenceListRepository listRepository;

  /** 证据清单Mapper */
  private final EvidenceListMapper listMapper;

  /** 证据仓储 */
  private final EvidenceRepository evidenceRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** 证据清单文档生成器 */
  private final EvidenceListDocumentGenerator documentGenerator;

  /** MinIO服务 */
  private final MinioService minioService;

  /** 文档仓储 */
  private final com.lawfirm.domain.document.repository.DocumentRepository documentRepository;

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

  /**
   * 分页查询证据清单 ✅ 修复问题579: 添加权限验证.
   *
   * @param matterId 项目ID
   * @param listType 清单类型
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  public PageResult<EvidenceListDTO> listEvidenceLists(
      final Long matterId, final String listType, final int pageNum, final int pageSize) {
    // ✅ 验证项目访问权限
    if (matterId != null) {
      validateMatterAccess(matterId);
    }

    IPage<EvidenceList> page =
        listMapper.selectListPage(new Page<>(pageNum, pageSize), matterId, listType);
    List<EvidenceListDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
    return PageResult.of(records, page.getTotal(), pageNum, pageSize);
  }

  /**
   * 获取清单详情 ✅ 修复问题580: 添加权限验证 + 优化N+1查询.
   *
   * @param id 清单ID
   * @return 清单DTO
   */
  public EvidenceListDTO getListById(final Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目访问权限
    if (list.getMatterId() != null) {
      validateMatterAccess(list.getMatterId());
    }

    EvidenceListDTO dto = toDTO(list);
    // ✅ 批量加载证据详情（优化N+1查询）
    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
    if (!evidenceIds.isEmpty()) {
      List<Evidence> evidences = evidenceRepository.listByIds(evidenceIds);
      dto.setEvidences(evidences.stream().map(this::toEvidenceDTO).collect(Collectors.toList()));
    }
    return dto;
  }

  /**
   * 创建证据清单 ✅ 修复问题589: 添加权限验证.
   *
   * @param command 创建命令
   * @return 清单DTO
   */
  @Transactional
  public EvidenceListDTO createList(final CreateEvidenceListCommand command) {
    // ✅ 验证项目编辑权限
    if (command.getMatterId() != null) {
      validateMatterEditPermission(command.getMatterId());
    }

    String listNo = generateListNo();
    String evidenceIdsJson = toJson(command.getEvidenceIds());

    EvidenceList list =
        EvidenceList.builder()
            .listNo(listNo)
            .matterId(command.getMatterId())
            .name(command.getName())
            .listType(command.getListType())
            .evidenceIds(evidenceIdsJson)
            .status(EvidenceList.STATUS_DRAFT)
            .build();

    listRepository.save(list);
    log.info("证据清单创建成功: {}, 创建人: {}", list.getName(), SecurityUtils.getUserId());
    return toDTO(list);
  }

  /**
   * 更新证据清单 ✅ 修复问题590: 添加权限验证.
   *
   * @param id 清单ID
   * @param name 名称
   * @param listType 清单类型
   * @param evidenceIds 证据ID列表
   * @return 清单DTO
   */
  @Transactional
  public EvidenceListDTO updateList(
      final Long id, final String name, final String listType, final List<Long> evidenceIds) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目编辑权限
    if (list.getMatterId() != null) {
      validateMatterEditPermission(list.getMatterId());
    }

    if (name != null) {
      list.setName(name);
    }
    if (listType != null) {
      list.setListType(listType);
    }
    if (evidenceIds != null) {
      list.setEvidenceIds(toJson(evidenceIds));
    }
    listRepository.updateById(list);
    log.info("证据清单更新成功: {}", list.getName());
    return toDTO(list);
  }

  /**
   * 删除证据清单 ✅ 修复问题591: 添加权限验证.
   *
   * @param id 清单ID
   */
  @Transactional
  public void deleteList(final Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目编辑权限
    if (list.getMatterId() != null) {
      validateMatterEditPermission(list.getMatterId());
    }

    listRepository.removeById(id);
    log.info("证据清单删除成功: {}, 操作人: {}", list.getName(), SecurityUtils.getUserId());
  }

  /**
   * 生成证据清单文件（返回下载URL） ✅ 修复问题592/597: 添加权限验证 + 优化N+1查询.
   *
   * @param id 清单ID
   * @param format 文件格式
   * @return 文件URL
   */
  @Transactional
  public String generateListFile(final Long id, final String format) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目访问权限
    if (list.getMatterId() != null) {
      validateMatterAccess(list.getMatterId());
    }

    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());

    if (evidenceIds.isEmpty()) {
      throw new BusinessException("清单中没有证据");
    }

    // ✅ 批量加载证据详情（优化N+1查询）
    List<Evidence> evidenceList = evidenceRepository.listByIds(evidenceIds);
    List<EvidenceDTO> evidences =
        evidenceList.stream().map(this::toEvidenceDTO).collect(Collectors.toList());

    // 获取案件信息
    Matter matter = null;
    if (list.getMatterId() != null) {
      matter = matterRepository.findById(list.getMatterId());
    }

    // 生成文档
    // ✅ 修复问题612: 支持PDF格式生成
    byte[] documentBytes;
    String contentType;
    String fileExtension;

    if ("pdf".equalsIgnoreCase(format)) {
      // PDF格式：调用PDF生成器
      documentBytes = documentGenerator.generatePdfDocument(toDTO(list), matter, evidences);
      contentType = "application/pdf";
      fileExtension = "pdf";
    } else {
      // 默认生成Word
      documentBytes = documentGenerator.generateWordDocument(toDTO(list), matter, evidences);
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      fileExtension = "docx";
    }

    // ✅ 修复问题613: 使用更友好的文件名格式（日期而非时间戳）
    String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String fileName = list.getName() + "_" + dateStr + "." + fileExtension;
    String fileUrl;
    try {
      fileUrl =
          minioService.uploadFile(
              new ByteArrayInputStream(documentBytes), fileName, "evidence-list/", contentType);
    } catch (Exception e) {
      log.error("上传证据清单文件失败", e);
      throw new BusinessException("上传文件失败: " + e.getMessage());
    }

    // 更新清单状态
    list.setFileName(fileName);
    list.setFileUrl(fileUrl);
    list.setStatus(EvidenceList.STATUS_GENERATED);
    listRepository.updateById(list);

    log.info("证据清单文件生成成功: {}", fileName);
    return fileUrl;
  }

  /**
   * 导出证据清单为Word格式 ✅ 修复问题593/598: 添加权限验证 + 优化N+1查询.
   *
   * @param id 清单ID
   * @return 文件字节数组
   */
  public byte[] exportToWord(final Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目访问权限
    if (list.getMatterId() != null) {
      validateMatterAccess(list.getMatterId());
    }

    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());

    if (evidenceIds.isEmpty()) {
      throw new BusinessException("清单中没有证据");
    }

    // ✅ 批量加载证据详情（优化N+1查询）
    List<Evidence> evidenceList = evidenceRepository.listByIds(evidenceIds);
    List<EvidenceDTO> evidences =
        evidenceList.stream().map(this::toEvidenceDTO).collect(Collectors.toList());

    // 获取案件信息
    Matter matter = null;
    if (list.getMatterId() != null) {
      matter = matterRepository.findById(list.getMatterId());
    }

    return documentGenerator.generateWordDocument(toDTO(list), matter, evidences);
  }

  /**
   * 导出证据清单为PDF格式 ✅ 修复问题594/599: 添加权限验证 + 优化N+1查询.
   *
   * @param id 清单ID
   * @return 文件字节数组
   */
  public byte[] exportToPdf(final Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目访问权限
    if (list.getMatterId() != null) {
      validateMatterAccess(list.getMatterId());
    }

    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());

    if (evidenceIds.isEmpty()) {
      throw new BusinessException("清单中没有证据");
    }

    // ✅ 批量加载证据详情（优化N+1查询）
    List<Evidence> evidenceList = evidenceRepository.listByIds(evidenceIds);
    List<EvidenceDTO> evidences =
        evidenceList.stream().map(this::toEvidenceDTO).collect(Collectors.toList());

    // 获取案件信息
    Matter matter = null;
    if (list.getMatterId() != null) {
      matter = matterRepository.findById(list.getMatterId());
    }

    return documentGenerator.generatePdfDocument(toDTO(list), matter, evidences);
  }

  /**
   * 将证据清单导出为PDF并保存到卷宗指定目录。
   *
   * @param id 清单ID
   * @param dossierItemId 卷宗目录项ID
   * @return 保存后的文档ID
   */
  @Transactional
  public Long saveToDossier(final Long id, final Long dossierItemId) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // 验证项目访问权限
    if (list.getMatterId() == null) {
      throw new BusinessException("证据清单未关联项目");
    }
    validateMatterAccess(list.getMatterId());

    // 生成PDF
    byte[] pdfBytes = exportToPdf(id);

    // 构建文件名（对用户输入的名称进行清理，防止路径遍历攻击）
    String baseName =
        MinioPathGenerator.sanitizeFilename(list.getName() != null ? list.getName() : "证据清单");
    String fileName =
        baseName + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

    // 构建存储路径
    String storagePath =
        "matter/M_"
            + list.getMatterId()
            + "/"
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            + "/证据清单/";

    // 生成物理文件名
    String physicalName =
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "_"
            + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            + "_"
            + fileName;

    try {
      // 上传到MinIO
      minioService.uploadFile(
          new ByteArrayInputStream(pdfBytes), physicalName, storagePath, "application/pdf");

      // 创建文档记录
      com.lawfirm.domain.document.entity.Document document =
          com.lawfirm.domain.document.entity.Document.builder()
              .matterId(list.getMatterId())
              .title(list.getName() != null ? list.getName() : "证据清单")
              .fileName(fileName)
              .fileSize((long) pdfBytes.length)
              .fileType("pdf")
              .bucketName("law-firm")
              .storagePath(storagePath)
              .physicalName(physicalName)
              .securityLevel("INTERNAL")
              .status("ACTIVE")
              .description("从证据清单自动生成的PDF文件")
              .dossierItemId(dossierItemId)
              .sourceType("EVIDENCE_LIST")
              .createdBy(SecurityUtils.getUserId())
              .build();

      // 使用文档仓储保存（需要注入）
      documentRepository.save(document);

      log.info(
          "证据清单PDF已保存到卷宗: listId={}, docId={}, dossierItemId={}",
          id,
          document.getId(),
          dossierItemId);

      return document.getId();

    } catch (Exception e) {
      log.error("保存证据清单PDF到卷宗失败: listId={}, error={}", id, e.getMessage(), e);
      throw new BusinessException("保存失败: " + e.getMessage());
    }
  }

  /**
   * 按案件获取清单列表 ✅ 修复问题595: 添加权限验证.
   *
   * @param matterId 项目ID
   * @return 清单列表
   */
  public List<EvidenceListDTO> getListsByMatter(final Long matterId) {
    // ✅ 验证项目访问权限
    validateMatterAccess(matterId);

    return listMapper.selectByMatterId(matterId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取案件的所有历史清单（按时间倒序，M6-044）.
   *
   * @param matterId 项目ID
   * @return 清单列表
   */
  public List<EvidenceListDTO> getListHistory(final Long matterId) {
    List<EvidenceList> lists = listMapper.selectByMatterId(matterId);
    // 按创建时间倒序排序
    return lists.stream()
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 对比两个清单的差异（M6-044） ✅ 修复问题596/600: 添加权限验证 + 优化N+1查询 ✅ 修复问题614: 返回类型安全的DTO替代Map<String, Object>.
   *
   * @param listId1 清单1 ID
   * @param listId2 清单2 ID
   * @return 对比结果
   */
  public EvidenceListCompareResult compareLists(final Long listId1, final Long listId2) {
    EvidenceList list1 = listRepository.getByIdOrThrow(listId1, "清单1不存在");
    EvidenceList list2 = listRepository.getByIdOrThrow(listId2, "清单2不存在");

    // ✅ 验证项目访问权限
    if (list1.getMatterId() != null) {
      validateMatterAccess(list1.getMatterId());
    }
    if (list2.getMatterId() != null) {
      validateMatterAccess(list2.getMatterId());
    }

    List<Long> evidenceIds1 = parseEvidenceIds(list1.getEvidenceIds());
    List<Long> evidenceIds2 = parseEvidenceIds(list2.getEvidenceIds());

    // 找出新增、删除、保留的证据
    Set<Long> set1 = new HashSet<>(evidenceIds1);
    Set<Long> set2 = new HashSet<>(evidenceIds2);

    List<Long> added = new ArrayList<>(set2);
    added.removeAll(set1);

    List<Long> removed = new ArrayList<>(set1);
    removed.removeAll(set2);

    List<Long> common = new ArrayList<>(set1);
    common.retainAll(set2);

    // ✅ 使用类型安全的DTO
    EvidenceListCompareResult result =
        EvidenceListCompareResult.create(toDTO(list1), toDTO(list2), added, removed, common);

    // ✅ 批量加载证据详情（优化N+1查询）
    if (!added.isEmpty()) {
      List<Evidence> addedList = evidenceRepository.listByIds(added);
      result.setAddedEvidences(
          addedList.stream().map(this::toEvidenceDTO).collect(Collectors.toList()));
    }

    if (!removed.isEmpty()) {
      List<Evidence> removedList = evidenceRepository.listByIds(removed);
      result.setRemovedEvidences(
          removedList.stream().map(this::toEvidenceDTO).collect(Collectors.toList()));
    }

    log.info(
        "清单对比完成: list1={}, list2={}, 新增={}, 删除={}, 共同={}",
        list1.getListNo(),
        list2.getListNo(),
        added.size(),
        removed.size(),
        common.size());
    return result;
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
    if ("ALL".equals(dataScope)) {
      return;
    }

    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    List<Long> accessibleMatterIds =
        matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    if (accessibleMatterIds == null) {
      return;
    }

    if (!accessibleMatterIds.contains(matterId)) {
      throw new BusinessException("权限不足：无法访问该项目的证据清单");
    }
  }

  /**
   * 验证项目编辑权限.
   *
   * @param matterId 项目ID
   */
  private void validateMatterEditPermission(final Long matterId) {
    if (matterId == null) {
      return;
    }

    // 验证访问权限
    validateMatterAccess(matterId);

    // 验证是否是项目成员
    matterAppService.validateMatterOwnership(matterId);

    // 检查项目状态
    Matter matter = matterRepository.findById(matterId);
    if (matter != null
        && ("ARCHIVED".equals(matter.getStatus()) || "CLOSED".equals(matter.getStatus()))) {
      String statusName = "ARCHIVED".equals(matter.getStatus()) ? "已归档" : "已结案";
      throw new BusinessException("该项目" + statusName + "，无法编辑证据清单");
    }
  }

  /**
   * 生成清单编号.
   *
   * @return 清单编号
   */
  private String generateListNo() {
    return com.lawfirm.common.util.NumberGenerator.generateEvidenceListNo();
  }

  /**
   * 转换为JSON.
   *
   * @param ids ID列表
   * @return JSON字符串
   */
  private String toJson(final List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(ids);
    } catch (JsonProcessingException e) {
      log.error("证据ID列表序列化失败: {}", e.getMessage());
      return "[]";
    }
  }

  /**
   * 解析证据ID列表.
   *
   * @param json JSON字符串
   * @return ID列表
   */
  private List<Long> parseEvidenceIds(final String json) {
    if (json == null || json.isEmpty() || "[]".equals(json.trim())) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
    } catch (JsonProcessingException e) {
      log.error("证据ID列表解析失败, json={}, error={}", json, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * 获取清单类型名称.
   *
   * @param type 类型
   * @return 类型名称
   */
  private String getListTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case EvidenceList.TYPE_SUBMISSION -> "提交清单";
      case EvidenceList.TYPE_EXCHANGE -> "交换清单";
      case EvidenceList.TYPE_COURT -> "庭审清单";
      default -> type;
    };
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case EvidenceList.STATUS_DRAFT -> "草稿";
      case EvidenceList.STATUS_GENERATED -> "已生成";
      default -> status;
    };
  }

  /**
   * 转换为DTO.
   *
   * @param list 证据清单实体
   * @return 清单DTO
   */
  private EvidenceListDTO toDTO(final EvidenceList list) {
    EvidenceListDTO dto = new EvidenceListDTO();
    dto.setId(list.getId());
    dto.setListNo(list.getListNo());
    dto.setMatterId(list.getMatterId());
    dto.setName(list.getName());
    dto.setListType(list.getListType());
    dto.setListTypeName(getListTypeName(list.getListType()));
    dto.setEvidenceIds(list.getEvidenceIds());
    dto.setEvidenceIdList(parseEvidenceIds(list.getEvidenceIds()));
    dto.setFileUrl(list.getFileUrl());
    dto.setFileName(list.getFileName());
    dto.setStatus(list.getStatus());
    dto.setStatusName(getStatusName(list.getStatus()));
    dto.setCreatedAt(list.getCreatedAt());
    dto.setUpdatedAt(list.getUpdatedAt());
    return dto;
  }

  /**
   * 转换为证据DTO.
   *
   * @param evidence 证据实体
   * @return 证据DTO
   */
  private EvidenceDTO toEvidenceDTO(final Evidence evidence) {
    EvidenceDTO dto = new EvidenceDTO();
    dto.setId(evidence.getId());
    dto.setEvidenceNo(evidence.getEvidenceNo());
    dto.setName(evidence.getName());
    dto.setEvidenceType(evidence.getEvidenceType());
    dto.setGroupName(evidence.getGroupName());
    dto.setSortOrder(evidence.getSortOrder());
    dto.setProvePurpose(evidence.getProvePurpose());
    dto.setIsOriginal(evidence.getIsOriginal());
    dto.setPageStart(evidence.getPageStart());
    dto.setPageEnd(evidence.getPageEnd());
    return dto;
  }
}
