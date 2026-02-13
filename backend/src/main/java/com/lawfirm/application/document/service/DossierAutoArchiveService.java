package com.lawfirm.application.document.service;

import com.lawfirm.application.finance.dto.ContractPrintDTO;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.application.finance.service.ContractPrintService;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.domain.document.repository.DocumentTemplateRepository;
import com.lawfirm.domain.document.repository.MatterDossierItemRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 卷宗自动归档服务
 *
 * <p>业务目的： 将项目材料按照卷宗管理的目录结构自动归档到对应的顺序目录中。 自动生成的文档可用于打印给客户签字盖章。
 *
 * <p>归档内容： - 收案审批表：合同审批单的快照PDF（审批通过后生成） - 委托合同：合同表单内容的快照PDF - 授权委托书：通过文书模板 + 项目信息自动生成（用于打印签字）
 *
 * <p>触发时机： - 项目创建后异步触发（archiveMatterDocumentsAsync） - 也可手动触发重新归档（archiveMatterDocuments）
 *
 * <p>模板类型常量： - APPROVAL_FORM: 收案审批表模板 - POWER_OF_ATTORNEY: 授权委托书模板 - CONTRACT: 委托合同模板
 */
@Service
@RequiredArgsConstructor
public class DossierAutoArchiveService {

  /** 日志记录器. */
  private static final Logger LOG = LoggerFactory.getLogger(DossierAutoArchiveService.class);

  /** 异步归档等待时间（毫秒），确保主事务已提交 */
  private static final long ASYNC_ARCHIVE_DELAY_MS = 500L;

  /** 收案审批表模板类型. */
  public static final String TEMPLATE_TYPE_APPROVAL_FORM = "APPROVAL_FORM";

  /** 授权委托书模板类型. */
  public static final String TEMPLATE_TYPE_POWER_OF_ATTORNEY = "POWER_OF_ATTORNEY";

  /** 委托合同模板类型. */
  public static final String TEMPLATE_TYPE_CONTRACT = "CONTRACT";

  /** 项目仓储. */
  private final MatterRepository matterRepository;

  /** 卷宗目录项仓储. */
  private final MatterDossierItemRepository dossierItemRepository;

  /** 卷宗服务. */
  private final MatterDossierService dossierService;

  /** 文档仓储. */
  private final DocumentRepository documentRepository;

  /** 文档模板仓储. */
  private final DocumentTemplateRepository documentTemplateRepository;

  /** 合同仓储. */
  private final ContractRepository contractRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** 审批Mapper. */
  private final ApprovalMapper approvalMapper;

  /** 系统配置Mapper. */
  private final SysConfigMapper sysConfigMapper;

  /** MinIO服务. */
  private final MinioService minioService;

  /** PDF生成服务. */
  private final PdfGeneratorService pdfGeneratorService;

  /** 模板变量服务. */
  private final TemplateVariableService templateVariableService;

  /** 合同应用服务（延迟加载以打破循环依赖）. */
  // 使用字段注入 + @Lazy 打破循环依赖
  // 注意：ContractAppService 形成循环依赖链，需要延迟加载
  @Lazy @org.springframework.beans.factory.annotation.Autowired
  private ContractAppService contractAppService;

  /** 合同打印服务（延迟加载以打破循环依赖） */
  @Lazy @org.springframework.beans.factory.annotation.Autowired
  private ContractPrintService contractPrintService;

  /** 文档来源类型常量. */
  public static final String SOURCE_TYPE_SYSTEM_GENERATED = "SYSTEM_GENERATED";

  /** 系统关联来源类型. */
  public static final String SOURCE_TYPE_SYSTEM_LINKED = "SYSTEM_LINKED";

  /** 用户上传来源类型. */
  public static final String SOURCE_TYPE_USER_UPLOADED = "USER_UPLOADED";

  /** 签字版本来源类型. */
  public static final String SOURCE_TYPE_SIGNED_VERSION = "SIGNED_VERSION";

  /** 来源模块常量. */
  public static final String SOURCE_MODULE_CONTRACT = "CONTRACT";

  /** 审批来源模块. */
  public static final String SOURCE_MODULE_APPROVAL = "APPROVAL";

  /** 发票来源模块. */
  public static final String SOURCE_MODULE_INVOICE = "INVOICE";

  /** 项目来源模块. */
  public static final String SOURCE_MODULE_MATTER = "MATTER";

  /**
   * 文档创建上下文参数对象，用于封装创建归档文档所需的文件和来源信息。
   *
   * <p>此类用于减少 createArchivedDocument 方法的参数数量，将相关的文档信息组合在一起。
   */
  private static class DocumentCreationContext {
    /** 项目ID */
    private final Long matterId;

    /** 卷宗目录项ID */
    private final Long dossierItemId;

    /** 文档标题 */
    private final String title;

    /** 文件名 */
    private final String fileName;

    /** 文件URL */
    private final String fileUrl;

    /** 文件大小 */
    private final Long fileSize;

    /** 文件类型 */
    private final String fileType;

    /**
     * 构造文档创建上下文。
     *
     * @param matterId 项目ID
     * @param dossierItemId 卷宗目录项ID
     * @param title 文档标题
     * @param fileName 文件名
     * @param fileUrl 文件URL
     * @param fileSize 文件大小
     * @param fileType 文件类型
     */
    DocumentCreationContext(
        final Long matterId,
        final Long dossierItemId,
        final String title,
        final String fileName,
        final String fileUrl,
        final Long fileSize,
        final String fileType) {
      this.matterId = matterId;
      this.dossierItemId = dossierItemId;
      this.title = title;
      this.fileName = fileName;
      this.fileUrl = fileUrl;
      this.fileSize = fileSize;
      this.fileType = fileType;
    }

    public Long getMatterId() {
      return matterId;
    }

    public Long getDossierItemId() {
      return dossierItemId;
    }

    public String getTitle() {
      return title;
    }

    public String getFileName() {
      return fileName;
    }

    public String getFileUrl() {
      return fileUrl;
    }

    public Long getFileSize() {
      return fileSize;
    }

    public String getFileType() {
      return fileType;
    }
  }

  /**
   * 项目创建后，自动归档相关材料（异步执行）。 注意：此方法在新事务中执行，确保主事务已提交后数据可见
   *
   * @param matterId 项目ID
   * @param contractId 关联合同ID
   * @param operatorId 操作人ID（用于异步线程中设置创建人）
   */
  @Async
  public void archiveMatterDocumentsAsync(
      final Long matterId, final Long contractId, final Long operatorId) {
    try {
      // 等待一小段时间确保主事务已提交
      try {
        Thread.sleep(ASYNC_ARCHIVE_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      // 在新事务中执行归档
      doArchiveMatterDocuments(matterId, contractId, operatorId);
    } catch (Exception e) {
      LOG.error("异步归档项目文档失败: matterId={}, contractId={}", matterId, contractId, e);
    }
  }

  /**
   * 项目创建后，自动归档相关材料（同步执行）。 用于测试或手动触发
   *
   * @param matterId 项目ID
   * @param contractId 关联合同ID
   */
  @Transactional
  public void archiveMatterDocuments(final Long matterId, final Long contractId) {
    Long operatorId = SecurityUtils.getUserIdOrDefault(1L);
    doArchiveMatterDocuments(matterId, contractId, operatorId);
  }

  /**
   * 执行自动归档的核心逻辑。
   *
   * @param matterId 项目ID
   * @param contractId 关联合同ID
   * @param operatorId 操作人ID
   */
  @Transactional
  public void doArchiveMatterDocuments(
      final Long matterId, final Long contractId, final Long operatorId) {
    LOG.info(
        "开始自动归档项目材料: matterId={}, contractId={}, operatorId={}", matterId, contractId, operatorId);

    try {
      // 确保项目存在
      Matter matter = matterRepository.getById(matterId);
      if (matter == null) {
        LOG.error("项目不存在，无法归档: matterId={}", matterId);
        return;
      }

      // 直接初始化卷宗目录（不做权限检查，因为是系统操作）
      initializeDossierInternal(matterId, matter);

      // 1. 归档收案审批表
      archiveApprovalFormInternal(matterId, contractId, operatorId);

      // 2. 归档委托合同
      archiveContractInternal(matterId, contractId, operatorId);

      // 3. 归档授权委托书（首次归档，不强制覆盖）
      archivePowerOfAttorneyInternal(matterId, operatorId, false);

      LOG.info("项目材料自动归档完成: matterId={}", matterId);
    } catch (Exception e) {
      LOG.error("项目材料自动归档失败: matterId={}, error={}", matterId, e.getMessage(), e);
      // 不抛出异常，避免影响主业务流程
    }
  }

  /**
   * 内部初始化卷宗目录（不做权限检查）。
   *
   * @param matterId 项目ID
   * @param matter 项目实体
   */
  private void initializeDossierInternal(final Long matterId, final Matter matter) {
    if (dossierItemRepository.hasDossierItems(matterId)) {
      LOG.debug("项目卷宗目录已存在: matterId={}", matterId);
      return;
    }

    // 调用 dossierService 初始化，但如果失败则使用备用方案
    try {
      dossierService.initializeDossier(matterId);
    } catch (Exception e) {
      LOG.warn("通过 DossierService 初始化失败，尝试备用方案: {}", e.getMessage());
      // 备用方案：直接创建基本目录项
      createDefaultDossierItems(matterId);
    }
  }

  /**
   * 创建默认的卷宗目录项（备用方案）。
   *
   * @param matterId 项目ID
   */
  private void createDefaultDossierItems(final Long matterId) {
    String[] defaultItems = {"收案审批表", "委托合同", "授权委托书", "起诉材料", "证据材料", "裁判文书", "结案材料"};
    int sortOrder = 1;

    for (String name : defaultItems) {
      MatterDossierItem item = new MatterDossierItem();
      item.setMatterId(matterId);
      item.setParentId(0L);
      item.setName(name);
      item.setItemType("FOLDER");
      item.setSortOrder(sortOrder++);
      item.setDocumentCount(0);
      dossierItemRepository.save(item);
    }
    LOG.info("使用备用方案创建默认卷宗目录: matterId={}", matterId);
  }

  /**
   * 归档收案审批表（公开方法，使用当前用户）。
   *
   * @param matterId 项目ID
   * @param contractId 合同ID
   */
  @Transactional
  public void archiveApprovalForm(final Long matterId, final Long contractId) {
    archiveApprovalFormInternal(matterId, contractId, SecurityUtils.getUserIdOrDefault(1L));
  }

  /**
   * 归档收案审批表（内部方法，指定操作人）。
   *
   * <p>业务说明：收案审批表是合同审批单的快照PDF 只有审批通过后才能生成有效的审批表归档
   *
   * @param matterId 项目ID
   * @param contractId 合同ID
   * @param operatorId 操作人ID
   */
  private void archiveApprovalFormInternal(
      final Long matterId, final Long contractId, final Long operatorId) {
    if (contractId == null) {
      LOG.debug("未关联合同，跳过收案审批表归档: matterId={}", matterId);
      return;
    }

    // 查找"收案审批表"目录项
    MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "收案审批");
    if (dossierItem == null) {
      LOG.debug("未找到收案审批表目录项: matterId={}", matterId);
      return;
    }

    // 检查是否已归档
    if (hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_APPROVAL, contractId)) {
      LOG.debug("收案审批表已归档: matterId={}, contractId={}", matterId, contractId);
      return;
    }

    try {
      // 使用 ContractAppService 获取完整的打印数据（包含所有字段）
      // 这样可以生成与合同管理模块预览一致的完整审批表
      ContractPrintDTO printDTO;
      try {
        printDTO = contractPrintService.getContractPrintData(contractId);
      } catch (Exception e) {
        LOG.warn("获取合同打印数据失败，使用简化方式生成审批表: contractId={}, error={}", contractId, e.getMessage());
        // 降级方案：使用简化方式生成
        List<Approval> approvals = approvalMapper.selectByBusiness("CONTRACT", contractId);
        if (approvals == null || approvals.isEmpty()) {
          LOG.debug("未找到合同审批记录: contractId={}", contractId);
          return;
        }

        Approval approval =
            approvals.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .max(
                    (a1, a2) -> {
                      if (a1.getApprovedAt() == null) {
                        return -1;
                      }
                      if (a2.getApprovedAt() == null) {
                        return 1;
                      }
                      return a1.getApprovedAt().compareTo(a2.getApprovedAt());
                    })
                .orElse(null);

        if (approval == null) {
          LOG.debug("未找到已通过的审批记录，跳过收案审批表归档: contractId={}", contractId);
          return;
        }

        Contract contract = contractRepository.getById(contractId);
        Matter matter = matterRepository.getById(matterId);
        Client client =
            contract != null && contract.getClientId() != null
                ? clientRepository.getById(contract.getClientId())
                : null;

        byte[] pdfContent =
            pdfGeneratorService.generateApprovalFormPdf(approval, contract, matter, client);

        String fileName =
            "收案审批表_" + (contract != null ? contract.getContractNo() : matterId) + ".pdf";
        String storagePath = "dossier/" + matterId + "/" + fileName;
        String fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");

        DocumentCreationContext context =
            new DocumentCreationContext(
                matterId,
                dossierItem.getId(),
                "收案审批表",
                fileName,
                fileUrl,
                (long) pdfContent.length,
                "pdf");
        createArchivedDocument(
            context,
            SOURCE_TYPE_SYSTEM_GENERATED,
            SOURCE_MODULE_APPROVAL,
            approval.getId(),
            operatorId);

        LOG.info("收案审批表归档成功（简化版）: matterId={}, contractId={}", matterId, contractId);
        return;
      }

      // 使用完整数据生成审批表PDF（与合同管理模块预览一致）
      byte[] pdfContent = pdfGeneratorService.generateApprovalFormPdfFromPrintDTO(printDTO);

      // 上传到MinIO
      String contractNo =
          printDTO.getContractNo() != null ? printDTO.getContractNo() : String.valueOf(contractId);
      String fileName = "收案审批表_" + contractNo + ".pdf";
      String storagePath = "dossier/" + matterId + "/" + fileName;
      String fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");

      // 获取审批记录ID（用于文档来源追踪）
      Long approvalId = null;
      if (printDTO.getApprovals() != null && !printDTO.getApprovals().isEmpty()) {
        // 查找第一个已通过的审批记录的ID
        List<Approval> approvals = approvalMapper.selectByBusiness("CONTRACT", contractId);
        if (approvals != null && !approvals.isEmpty()) {
          Approval approval =
              approvals.stream()
                  .filter(a -> "APPROVED".equals(a.getStatus()))
                  .findFirst()
                  .orElse(null);
          if (approval != null) {
            approvalId = approval.getId();
          }
        }
      }

      // 创建文档记录
      DocumentCreationContext context =
          new DocumentCreationContext(
              matterId,
              dossierItem.getId(),
              "收案审批表",
              fileName,
              fileUrl,
              (long) pdfContent.length,
              "pdf");
      createArchivedDocument(
          context,
          SOURCE_TYPE_SYSTEM_GENERATED,
          SOURCE_MODULE_APPROVAL,
          approvalId != null ? approvalId : contractId,
          operatorId);

      LOG.info("收案审批表归档成功（完整版）: matterId={}, contractId={}", matterId, contractId);
    } catch (Exception e) {
      LOG.error(
          "收案审批表归档失败: matterId={}, contractId={}, error={}",
          matterId,
          contractId,
          e.getMessage(),
          e);
    }
  }

  /**
   * 归档委托合同（公开方法，使用当前用户）。
   *
   * @param matterId 项目ID
   * @param contractId 合同ID
   */
  @Transactional
  public void archiveContract(final Long matterId, final Long contractId) {
    archiveContractInternal(matterId, contractId, SecurityUtils.getUserIdOrDefault(1L));
  }

  /**
   * 归档委托合同（内部方法，指定操作人）。
   *
   * <p>业务说明：委托合同是合同表单内容的快照PDF - 如果合同已有上传的文件，直接关联 - 否则根据合同表单内容生成PDF快照
   *
   * @param matterId 项目ID
   * @param contractId 合同ID
   * @param operatorId 操作人ID
   */
  private void archiveContractInternal(
      final Long matterId, final Long contractId, final Long operatorId) {
    if (contractId == null) {
      LOG.debug("未关联合同，跳过委托合同归档: matterId={}", matterId);
      return;
    }

    // 查找"委托合同"目录项
    MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "委托合同");
    if (dossierItem == null) {
      LOG.debug("未找到委托合同目录项: matterId={}", matterId);
      return;
    }

    // 检查是否已归档
    if (hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_CONTRACT, contractId)) {
      LOG.debug("委托合同已归档: matterId={}, contractId={}", matterId, contractId);
      return;
    }

    try {
      Contract contract = contractRepository.getById(contractId);
      if (contract == null) {
        LOG.warn("合同不存在: contractId={}", contractId);
        return;
      }

      String fileUrl;
      String fileName = "委托代理合同_" + contract.getContractNo() + ".pdf";
      long fileSize;
      String fileType = "pdf";
      String sourceType;

      // 方案1: 如果合同已有上传的文件，直接关联
      if (contract.getFileUrl() != null && !contract.getFileUrl().isEmpty()) {
        fileUrl = contract.getFileUrl();
        fileSize = 0; // 已有文件无法获取实际大小
        sourceType = SOURCE_TYPE_SYSTEM_LINKED;
        LOG.debug("委托合同使用已有文件: contractId={}", contractId);
      } else {
        // 方案2: 根据合同表单内容生成PDF快照
        Matter matter = matterRepository.getById(matterId);
        Client client =
            contract.getClientId() != null
                ? clientRepository.getById(contract.getClientId())
                : null;

        byte[] pdfContent = pdfGeneratorService.generateContractPdf(contract, matter, client);

        String storagePath = "dossier/" + matterId + "/" + fileName;
        fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");
        fileSize = pdfContent.length;
        sourceType = SOURCE_TYPE_SYSTEM_GENERATED;
        LOG.debug("委托合同生成PDF快照: contractId={}", contractId);
      }

      DocumentCreationContext context =
          new DocumentCreationContext(
              matterId,
              dossierItem.getId(),
              contract.getName() != null ? contract.getName() : "委托代理合同",
              fileName,
              fileUrl,
              fileSize,
              fileType);
      createArchivedDocument(context, sourceType, SOURCE_MODULE_CONTRACT, contractId, operatorId);

      LOG.info("委托合同归档成功: matterId={}, contractId={}", matterId, contractId);
    } catch (Exception e) {
      LOG.error(
          "委托合同归档失败: matterId={}, contractId={}, error={}",
          matterId,
          contractId,
          e.getMessage(),
          e);
    }
  }

  /**
   * 归档授权委托书（公开方法，使用当前用户）。
   *
   * @param matterId 项目ID
   */
  @Transactional
  public void archivePowerOfAttorney(final Long matterId) {
    archivePowerOfAttorneyInternal(matterId, SecurityUtils.getUserIdOrDefault(1L), false);
  }

  /**
   * 重新生成授权委托书（强制覆盖已有的）。
   *
   * <p>使用场景： - 模板管理员更新了模板后，需要重新生成 - 项目信息变更后，需要更新委托书 - 之前无模板时使用了默认格式，现在模板准备好了
   *
   * @param matterId 项目ID
   * @return 是否成功重新生成
   */
  @Transactional
  public boolean regeneratePowerOfAttorney(final Long matterId) {
    LOG.info("开始重新生成授权委托书: matterId={}", matterId);

    // 查找并删除已有的授权委托书
    MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "授权委托书");
    if (dossierItem != null) {
      deleteExistingDocument(dossierItem.getId(), SOURCE_MODULE_MATTER, matterId);
    }

    // 重新生成
    archivePowerOfAttorneyInternal(matterId, SecurityUtils.getUserIdOrDefault(1L), true);
    return true;
  }

  /**
   * 归档授权委托书（内部方法，指定操作人）。
   *
   * <p>业务说明：授权委托书通过文书模板 + 项目信息自动生成 - 用于打印给客户签字盖章 - 优先使用系统配置的授权委托书模板 - 如无模板则使用默认格式生成
   *
   * @param matterId 项目ID
   * @param operatorId 操作人ID
   * @param forceRegenerate 是否强制重新生成（跳过已归档检查）
   */
  private void archivePowerOfAttorneyInternal(
      final Long matterId, final Long operatorId, final boolean forceRegenerate) {
    // 查找"授权委托书"目录项
    MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "授权委托书");
    if (dossierItem == null) {
      LOG.debug("未找到授权委托书目录项: matterId={}", matterId);
      return;
    }

    // 检查是否已归档（非强制模式下）
    if (!forceRegenerate
        && hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_MATTER, matterId)) {
      LOG.debug("授权委托书已归档: matterId={}", matterId);
      return;
    }

    try {
      Matter matter = matterRepository.getById(matterId);
      if (matter == null) {
        LOG.warn("项目不存在: matterId={}", matterId);
        return;
      }

      Client client =
          matter.getClientId() != null ? clientRepository.getById(matter.getClientId()) : null;

      // 获取承办律师信息
      User lawyer =
          matter.getLeadLawyerId() != null
              ? userRepository.getById(matter.getLeadLawyerId())
              : null;
      String lawyerName = lawyer != null ? lawyer.getRealName() : null;
      String lawyerLicenseNo = lawyer != null ? lawyer.getLawyerLicenseNo() : null;

      // 获取律所信息
      String firmName = getConfigValue("firm.name");

      byte[] pdfContent;
      Long usedTemplateId = null; // 记录使用的模板ID，用于成功后增加计数

      // 尝试使用模板生成（根据案件类型匹配模板）
      String caseType = matter.getCaseType();
      DocumentTemplate template =
          documentTemplateRepository.findByTemplateTypeAndCaseType(
              TEMPLATE_TYPE_POWER_OF_ATTORNEY, caseType);
      if (template != null && template.getContent() != null && !template.getContent().isEmpty()) {
        LOG.debug("使用模板生成授权委托书: templateNo={}, caseType={}", template.getTemplateNo(), caseType);

        // 收集变量并替换（自动抓取项目信息）
        Map<String, Object> variables = templateVariableService.collectVariables(matterId);
        String content = templateVariableService.replaceVariables(template.getContent(), variables);

        // 检测模板格式：JSON分块格式 或 纯文本/HTML格式
        if (pdfGeneratorService.isBlockTemplateFormat(content)) {
          // 使用分块模板格式生成（固定排版格式：标题二号宋体，正文三号仿宋，备注五号宋体）
          LOG.debug("使用分块模板格式生成授权委托书");
          pdfContent = pdfGeneratorService.generatePowerOfAttorneyFromBlocks(content);
        } else {
          // 使用纯文本/HTML模板格式生成
          pdfContent = pdfGeneratorService.generatePdfFromTemplateContent("授权委托书", content);
        }
        usedTemplateId = template.getId();
      } else {
        // 回退到默认格式（传入律师和律所信息）
        LOG.debug("未找到授权委托书模板，使用默认格式");
        pdfContent =
            pdfGeneratorService.generatePowerOfAttorneyPdf(
                matter, client, lawyerName, lawyerLicenseNo, firmName);
      }

      String fileName = "授权委托书_" + matter.getMatterNo() + ".pdf";
      String storagePath = "dossier/" + matterId + "/" + fileName;
      String fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");

      DocumentCreationContext context =
          new DocumentCreationContext(
              matterId,
              dossierItem.getId(),
              "授权委托书",
              fileName,
              fileUrl,
              (long) pdfContent.length,
              "pdf");
      createArchivedDocument(
          context, SOURCE_TYPE_SYSTEM_GENERATED, SOURCE_MODULE_MATTER, matterId, operatorId);

      // 归档成功后再增加模板使用次数
      if (usedTemplateId != null) {
        documentTemplateRepository.incrementUseCount(usedTemplateId);
      }

      LOG.info("授权委托书归档成功: matterId={}", matterId);
    } catch (Exception e) {
      LOG.error("授权委托书归档失败: matterId={}, error={}", matterId, e.getMessage(), e);
    }
  }

  /**
   * 根据名称模式查找卷宗目录项。
   *
   * @param matterId 项目ID
   * @param namePattern 名称模式（支持模糊匹配）
   * @return 匹配的目录项，未找到返回null
   */
  public MatterDossierItem findDossierItemByNamePattern(
      final Long matterId, final String namePattern) {
    List<MatterDossierItem> items = dossierItemRepository.findByMatterId(matterId);

    return items.stream()
        .filter(item -> item.getName() != null && item.getName().contains(namePattern))
        .findFirst()
        .orElse(null);
  }

  /**
   * 检查是否已有归档文档。
   *
   * @param dossierItemId 卷宗目录项ID
   * @param sourceModule 来源模块
   * @param sourceId 来源ID
   * @return 是否已归档
   */
  private boolean hasArchivedDocument(
      final Long dossierItemId, final String sourceModule, final Long sourceId) {
    // 使用 DocumentRepository 查询
    List<Document> existingDocs = documentRepository.findByDossierItemId(dossierItemId);
    return existingDocs.stream()
        .anyMatch(
            doc ->
                sourceModule.equals(doc.getSourceModule()) && sourceId.equals(doc.getSourceId()));
  }

  /**
   * 删除已有的归档文档（用于重新生成）。
   *
   * @param dossierItemId 卷宗目录项ID
   * @param sourceModule 来源模块
   * @param sourceId 来源ID
   */
  private void deleteExistingDocument(
      final Long dossierItemId, final String sourceModule, final Long sourceId) {
    List<Document> existingDocs = documentRepository.findByDossierItemId(dossierItemId);
    existingDocs.stream()
        .filter(
            doc -> sourceModule.equals(doc.getSourceModule()) && sourceId.equals(doc.getSourceId()))
        .forEach(
            doc -> {
              // 软删除文档
              doc.setStatus("DELETED");
              documentRepository.updateById(doc);
              LOG.info("删除已有归档文档: docNo={}, title={}", doc.getDocNo(), doc.getTitle());
            });

    // 更新文档计数
    updateDossierItemCount(dossierItemId);
  }

  /**
   * 创建归档文档记录。
   *
   * @param context 文档创建上下文（包含项目、卷宗、文件等信息）
   * @param sourceType 来源类型
   * @param sourceModule 来源模块
   * @param sourceId 来源ID
   * @param operatorId 操作人ID
   */
  private void createArchivedDocument(
      final DocumentCreationContext context,
      final String sourceType,
      final String sourceModule,
      final Long sourceId,
      final Long operatorId) {
    // 生成文档编号
    String docNo = generateDocNo();

    Document document =
        Document.builder()
            .docNo(docNo)
            .title(context.getTitle())
            .matterId(context.getMatterId())
            .dossierItemId(context.getDossierItemId())
            .fileName(context.getFileName())
            .filePath(context.getFileUrl())
            .fileSize(context.getFileSize())
            .fileType(context.getFileType())
            .mimeType("application/pdf")
            .version(1)
            .isLatest(true)
            .securityLevel("INTERNAL")
            .status("ACTIVE")
            .fileCategory("OTHER")
            .sourceType(sourceType)
            .sourceModule(sourceModule)
            .sourceId(sourceId)
            .createdBy(operatorId != null ? operatorId : 1L)
            .build();

    documentRepository.save(document);

    // 更新卷宗目录项文档计数
    updateDossierItemCount(context.getDossierItemId());

    LOG.debug("创建归档文档: docNo={}, title={}, sourceType={}", docNo, context.getTitle(), sourceType);
  }

  /**
   * 更新卷宗目录项的文档计数。
   *
   * @param dossierItemId 卷宗目录项ID
   */
  private void updateDossierItemCount(final Long dossierItemId) {
    List<Document> docs = documentRepository.findByDossierItemId(dossierItemId);
    int count = (int) docs.stream().filter(doc -> !"DELETED".equals(doc.getStatus())).count();
    dossierItemRepository.updateDocumentCount(dossierItemId, count);
  }

  /**
   * 生成文档编号。
   *
   * @return 文档编号
   */
  private String generateDocNo() {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    return "DOC-" + timestamp + "-" + uuid;
  }

  /**
   * 获取系统配置值。
   *
   * @param configKey 配置键
   * @return 配置值，不存在返回null
   */
  private String getConfigValue(final String configKey) {
    try {
      return sysConfigMapper.selectValueByKey(configKey);
    } catch (Exception e) {
      LOG.warn("获取系统配置失败: key={}, error={}", configKey, e.getMessage());
      return null;
    }
  }
}
