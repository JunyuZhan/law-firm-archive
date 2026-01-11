package com.lawfirm.application.document.service;

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
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 卷宗自动归档服务
 * 
 * 负责在业务流程中自动将相关材料归档到项目卷宗：
 * - 收案审批表：基于模板自动生成（支持自定义格式）
 * - 委托合同：关联系统中已生成的合同文档或基于模板生成
 * - 授权委托书：基于模板自动生成（支持自定义代理权限）
 * - 收费发票：开票完成后自动关联
 * 
 * 模板类型：
 * - APPROVAL_FORM: 收案审批表
 * - POWER_OF_ATTORNEY: 授权委托书
 * - CONTRACT: 委托合同
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DossierAutoArchiveService {

    // 模板类型常量
    public static final String TEMPLATE_TYPE_APPROVAL_FORM = "APPROVAL_FORM";
    public static final String TEMPLATE_TYPE_POWER_OF_ATTORNEY = "POWER_OF_ATTORNEY";
    public static final String TEMPLATE_TYPE_CONTRACT = "CONTRACT";

    private final MatterRepository matterRepository;
    private final MatterDossierItemRepository dossierItemRepository;
    private final MatterDossierService dossierService;
    private final DocumentRepository documentRepository;
    private final DocumentTemplateRepository documentTemplateRepository;
    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final ApprovalMapper approvalMapper;
    private final MinioService minioService;
    private final PdfGeneratorService pdfGeneratorService;
    private final TemplateVariableService templateVariableService;

    /** 文档来源类型常量 */
    public static final String SOURCE_TYPE_SYSTEM_GENERATED = "SYSTEM_GENERATED";
    public static final String SOURCE_TYPE_SYSTEM_LINKED = "SYSTEM_LINKED";
    public static final String SOURCE_TYPE_USER_UPLOADED = "USER_UPLOADED";
    public static final String SOURCE_TYPE_SIGNED_VERSION = "SIGNED_VERSION";

    /** 来源模块常量 */
    public static final String SOURCE_MODULE_CONTRACT = "CONTRACT";
    public static final String SOURCE_MODULE_APPROVAL = "APPROVAL";
    public static final String SOURCE_MODULE_INVOICE = "INVOICE";
    public static final String SOURCE_MODULE_MATTER = "MATTER";

    /**
     * 项目创建后，自动归档相关材料（异步执行）
     * 注意：此方法在新事务中执行，确保主事务已提交后数据可见
     * 
     * @param matterId 项目ID
     * @param contractId 关联合同ID
     * @param operatorId 操作人ID（用于异步线程中设置创建人）
     */
    @Async
    public void archiveMatterDocumentsAsync(Long matterId, Long contractId, Long operatorId) {
        // 等待一小段时间确保主事务已提交
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 在新事务中执行归档
        doArchiveMatterDocuments(matterId, contractId, operatorId);
    }

    /**
     * 项目创建后，自动归档相关材料（同步执行）
     * 用于测试或手动触发
     * 
     * @param matterId 项目ID
     * @param contractId 关联合同ID
     */
    @Transactional
    public void archiveMatterDocuments(Long matterId, Long contractId) {
        Long operatorId = SecurityUtils.getUserIdOrDefault(1L);
        doArchiveMatterDocuments(matterId, contractId, operatorId);
    }

    /**
     * 执行自动归档的核心逻辑
     */
    @Transactional
    public void doArchiveMatterDocuments(Long matterId, Long contractId, Long operatorId) {
        log.info("开始自动归档项目材料: matterId={}, contractId={}, operatorId={}", matterId, contractId, operatorId);
        
        try {
            // 确保项目存在
            Matter matter = matterRepository.getById(matterId);
            if (matter == null) {
                log.error("项目不存在，无法归档: matterId={}", matterId);
                return;
            }
            
            // 直接初始化卷宗目录（不做权限检查，因为是系统操作）
            initializeDossierInternal(matterId, matter);
            
            // 1. 归档收案审批表
            archiveApprovalFormInternal(matterId, contractId, operatorId);
            
            // 2. 归档委托合同
            archiveContractInternal(matterId, contractId, operatorId);
            
            // 3. 归档授权委托书
            archivePowerOfAttorneyInternal(matterId, operatorId);
            
            log.info("项目材料自动归档完成: matterId={}", matterId);
        } catch (Exception e) {
            log.error("项目材料自动归档失败: matterId={}, error={}", matterId, e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 内部初始化卷宗目录（不做权限检查）
     */
    private void initializeDossierInternal(Long matterId, Matter matter) {
        if (dossierItemRepository.hasDossierItems(matterId)) {
            log.debug("项目卷宗目录已存在: matterId={}", matterId);
            return;
        }
        
        // 调用 dossierService 初始化，但如果失败则使用备用方案
        try {
            dossierService.initializeDossier(matterId);
        } catch (Exception e) {
            log.warn("通过 DossierService 初始化失败，尝试备用方案: {}", e.getMessage());
            // 备用方案：直接创建基本目录项
            createDefaultDossierItems(matterId);
        }
    }

    /**
     * 创建默认的卷宗目录项（备用方案）
     */
    private void createDefaultDossierItems(Long matterId) {
        String[] defaultItems = {"收案审批表", "委托合同", "授权委托书", "起诉材料", "证据材料", "裁判文书", "结案材料"};
        int sortOrder = 1;
        
        for (String name : defaultItems) {
            MatterDossierItem item = MatterDossierItem.builder()
                    .matterId(matterId)
                    .parentId(null)
                    .name(name)
                    .itemType("FOLDER")
                    .sortOrder(sortOrder++)
                    .documentCount(0)
                    .build();
            dossierItemRepository.save(item);
        }
        log.info("使用备用方案创建默认卷宗目录: matterId={}", matterId);
    }

    /**
     * 归档收案审批表（公开方法，使用当前用户）
     * 
     * @param matterId 项目ID
     * @param contractId 合同ID
     */
    @Transactional
    public void archiveApprovalForm(Long matterId, Long contractId) {
        archiveApprovalFormInternal(matterId, contractId, SecurityUtils.getUserIdOrDefault(1L));
    }

    /**
     * 归档收案审批表（内部方法，指定操作人）
     * 直接使用审批记录数据生成 PDF（审批表在合同审批时已产生）
     */
    private void archiveApprovalFormInternal(Long matterId, Long contractId, Long operatorId) {
        if (contractId == null) {
            log.debug("未关联合同，跳过收案审批表归档: matterId={}", matterId);
            return;
        }

        // 查找"收案审批表"目录项
        MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "收案审批");
        if (dossierItem == null) {
            log.debug("未找到收案审批表目录项: matterId={}", matterId);
            return;
        }

        // 检查是否已归档
        if (hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_APPROVAL, contractId)) {
            log.debug("收案审批表已归档: matterId={}, contractId={}", matterId, contractId);
            return;
        }

        try {
            // 获取审批记录
            List<Approval> approvals = approvalMapper.selectByBusiness("CONTRACT", contractId);
            if (approvals == null || approvals.isEmpty()) {
                log.debug("未找到合同审批记录: contractId={}", contractId);
                return;
            }
            // 取最新的已通过审批记录
            Approval approval = approvals.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .findFirst()
                .orElse(approvals.get(0));

            // 获取合同和项目信息
            Contract contract = contractRepository.getById(contractId);
            Matter matter = matterRepository.getById(matterId);
            Client client = contract != null && contract.getClientId() != null 
                ? clientRepository.getById(contract.getClientId()) : null;

            // 直接使用审批记录数据生成 PDF（不使用模板，因为审批表格式固定）
            byte[] pdfContent = pdfGeneratorService.generateApprovalFormPdf(approval, contract, matter, client);

            // 上传到MinIO
            String fileName = "收案审批表_" + (contract != null ? contract.getContractNo() : matterId) + ".pdf";
            String storagePath = "dossier/" + matterId + "/" + fileName;
            String fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");

            // 创建文档记录
            createArchivedDocument(
                matterId,
                dossierItem.getId(),
                "收案审批表",
                fileName,
                fileUrl,
                (long) pdfContent.length,
                "pdf",
                SOURCE_TYPE_SYSTEM_GENERATED,
                SOURCE_MODULE_APPROVAL,
                approval.getId(),
                operatorId
            );

            log.info("收案审批表归档成功: matterId={}, contractId={}", matterId, contractId);
        } catch (Exception e) {
            log.error("收案审批表归档失败: matterId={}, contractId={}, error={}", 
                matterId, contractId, e.getMessage(), e);
        }
    }

    /**
     * 归档委托合同（公开方法，使用当前用户）
     * 
     * @param matterId 项目ID
     * @param contractId 合同ID
     */
    @Transactional
    public void archiveContract(Long matterId, Long contractId) {
        archiveContractInternal(matterId, contractId, SecurityUtils.getUserIdOrDefault(1L));
    }

    /**
     * 归档委托合同（内部方法，指定操作人）
     * 优先使用已有文件，其次使用合同内容生成 PDF
     * 注意：合同在创建时已从模板加载内容，这里只是生成快照
     */
    private void archiveContractInternal(Long matterId, Long contractId, Long operatorId) {
        if (contractId == null) {
            log.debug("未关联合同，跳过委托合同归档: matterId={}", matterId);
            return;
        }

        // 查找"委托合同"目录项
        MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "委托合同");
        if (dossierItem == null) {
            log.debug("未找到委托合同目录项: matterId={}", matterId);
            return;
        }

        // 检查是否已归档
        if (hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_CONTRACT, contractId)) {
            log.debug("委托合同已归档: matterId={}, contractId={}", matterId, contractId);
            return;
        }

        try {
            Contract contract = contractRepository.getById(contractId);
            if (contract == null) {
                log.warn("合同不存在: contractId={}", contractId);
                return;
            }

            String fileUrl;
            String fileName;
            long fileSize;
            String fileType = "pdf";
            String sourceType;

            // 方案1: 如果合同已有文件，直接关联
            if (contract.getFileUrl() != null && !contract.getFileUrl().isEmpty()) {
                fileUrl = contract.getFileUrl();
                fileName = "委托代理合同_" + contract.getContractNo() + ".pdf";
                fileSize = 0; // 无法获取实际大小
                sourceType = SOURCE_TYPE_SYSTEM_LINKED;
            } 
            // 方案2: 使用合同内容生成 PDF（合同内容已在创建时从模板填充）
            else if (contract.getContent() != null && !contract.getContent().isEmpty()) {
                Matter matter = matterRepository.getById(matterId);
                Client client = contract.getClientId() != null 
                    ? clientRepository.getById(contract.getClientId()) : null;

                byte[] pdfContent = pdfGeneratorService.generateContractPdf(contract, matter, client);
                
                fileName = "委托代理合同_" + contract.getContractNo() + ".pdf";
                String storagePath = "dossier/" + matterId + "/" + fileName;
                fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");
                fileSize = pdfContent.length;
                sourceType = SOURCE_TYPE_SYSTEM_GENERATED;
            } 
            // 方案3: 合同无内容，使用默认格式生成
            else {
                Matter matter = matterRepository.getById(matterId);
                Client client = contract.getClientId() != null 
                    ? clientRepository.getById(contract.getClientId()) : null;

                byte[] pdfContent = pdfGeneratorService.generateContractPdf(contract, matter, client);
                
                fileName = "委托代理合同_" + contract.getContractNo() + ".pdf";
                String storagePath = "dossier/" + matterId + "/" + fileName;
                fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");
                fileSize = pdfContent.length;
                sourceType = SOURCE_TYPE_SYSTEM_GENERATED;
            }

            createArchivedDocument(
                matterId,
                dossierItem.getId(),
                contract.getName() != null ? contract.getName() : "委托代理合同",
                fileName,
                fileUrl,
                fileSize,
                fileType,
                sourceType,
                SOURCE_MODULE_CONTRACT,
                contractId,
                operatorId
            );

            log.info("委托合同归档成功: matterId={}, contractId={}", matterId, contractId);
        } catch (Exception e) {
            log.error("委托合同归档失败: matterId={}, contractId={}, error={}", 
                matterId, contractId, e.getMessage(), e);
        }
    }

    /**
     * 归档授权委托书（公开方法，使用当前用户）
     * 
     * @param matterId 项目ID
     */
    @Transactional
    public void archivePowerOfAttorney(Long matterId) {
        archivePowerOfAttorneyInternal(matterId, SecurityUtils.getUserIdOrDefault(1L));
    }

    /**
     * 归档授权委托书（内部方法，指定操作人）
     * 优先使用模板系统生成，支持自定义代理权限
     */
    private void archivePowerOfAttorneyInternal(Long matterId, Long operatorId) {
        // 查找"授权委托书"目录项
        MatterDossierItem dossierItem = findDossierItemByNamePattern(matterId, "授权委托书");
        if (dossierItem == null) {
            log.debug("未找到授权委托书目录项: matterId={}", matterId);
            return;
        }

        // 检查是否已归档
        if (hasArchivedDocument(dossierItem.getId(), SOURCE_MODULE_MATTER, matterId)) {
            log.debug("授权委托书已归档: matterId={}", matterId);
            return;
        }

        try {
            Matter matter = matterRepository.getById(matterId);
            if (matter == null) {
                log.warn("项目不存在: matterId={}", matterId);
                return;
            }

            Client client = matter.getClientId() != null 
                ? clientRepository.getById(matter.getClientId()) : null;

            byte[] pdfContent;

            // 尝试使用模板生成
            DocumentTemplate template = documentTemplateRepository.findFirstByTemplateType(TEMPLATE_TYPE_POWER_OF_ATTORNEY);
            if (template != null && template.getContent() != null && !template.getContent().isEmpty()) {
                log.debug("使用模板生成授权委托书: templateNo={}", template.getTemplateNo());
                
                // 收集变量并替换
                Map<String, Object> variables = templateVariableService.collectVariables(matterId);
                String content = templateVariableService.replaceVariables(template.getContent(), variables);
                
                // 生成 PDF
                pdfContent = pdfGeneratorService.generatePdfFromTemplateContent("授权委托书", content);
                
                // 增加模板使用次数
                documentTemplateRepository.incrementUseCount(template.getId());
            } else {
                // 回退到默认格式
                log.debug("未找到授权委托书模板，使用默认格式");
                pdfContent = pdfGeneratorService.generatePowerOfAttorneyPdf(matter, client);
            }

            String fileName = "授权委托书_" + matter.getMatterNo() + ".pdf";
            String storagePath = "dossier/" + matterId + "/" + fileName;
            String fileUrl = minioService.uploadBytes(pdfContent, storagePath, "application/pdf");

            createArchivedDocument(
                matterId,
                dossierItem.getId(),
                "授权委托书",
                fileName,
                fileUrl,
                (long) pdfContent.length,
                "pdf",
                SOURCE_TYPE_SYSTEM_GENERATED,
                SOURCE_MODULE_MATTER,
                matterId,
                operatorId
            );

            log.info("授权委托书归档成功: matterId={}", matterId);
        } catch (Exception e) {
            log.error("授权委托书归档失败: matterId={}, error={}", matterId, e.getMessage(), e);
        }
    }

    /**
     * 根据名称模式查找卷宗目录项
     * 
     * @param matterId 项目ID
     * @param namePattern 名称模式（支持模糊匹配）
     * @return 匹配的目录项，未找到返回null
     */
    public MatterDossierItem findDossierItemByNamePattern(Long matterId, String namePattern) {
        List<MatterDossierItem> items = dossierItemRepository.findByMatterId(matterId);
        
        return items.stream()
            .filter(item -> item.getName() != null && item.getName().contains(namePattern))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查是否已有归档文档
     * 
     * @param dossierItemId 卷宗目录项ID
     * @param sourceModule 来源模块
     * @param sourceId 来源ID
     * @return 是否已归档
     */
    private boolean hasArchivedDocument(Long dossierItemId, String sourceModule, Long sourceId) {
        // 使用 DocumentRepository 查询
        List<Document> existingDocs = documentRepository.findByDossierItemId(dossierItemId);
        return existingDocs.stream()
            .anyMatch(doc -> sourceModule.equals(doc.getSourceModule()) 
                && sourceId.equals(doc.getSourceId()));
    }

    /**
     * 创建归档文档记录
     */
    private void createArchivedDocument(
            Long matterId,
            Long dossierItemId,
            String title,
            String fileName,
            String fileUrl,
            Long fileSize,
            String fileType,
            String sourceType,
            String sourceModule,
            Long sourceId,
            Long operatorId
    ) {
        // 生成文档编号
        String docNo = generateDocNo();

        Document document = Document.builder()
            .docNo(docNo)
            .title(title)
            .matterId(matterId)
            .dossierItemId(dossierItemId)
            .fileName(fileName)
            .filePath(fileUrl)
            .fileSize(fileSize)
            .fileType(fileType)
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
        updateDossierItemCount(dossierItemId);

        log.debug("创建归档文档: docNo={}, title={}, sourceType={}", docNo, title, sourceType);
    }

    /**
     * 更新卷宗目录项的文档计数
     */
    private void updateDossierItemCount(Long dossierItemId) {
        List<Document> docs = documentRepository.findByDossierItemId(dossierItemId);
        int count = (int) docs.stream()
            .filter(doc -> !"DELETED".equals(doc.getStatus()))
            .count();
        dossierItemRepository.updateDocumentCount(dossierItemId, count);
    }

    /**
     * 生成文档编号
     */
    private String generateDocNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "DOC-" + timestamp + "-" + uuid;
    }
}

