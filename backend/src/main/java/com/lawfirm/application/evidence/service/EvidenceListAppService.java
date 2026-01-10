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
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import com.lawfirm.domain.evidence.repository.EvidenceListRepository;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.document.EvidenceListDocumentGenerator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceListMapper;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.application.matter.service.MatterAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 证据清单应用服务
 * ✅ 修复问题579-600: 添加权限验证和N+1查询优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceListAppService {

    private final EvidenceListRepository listRepository;
    private final EvidenceListMapper listMapper;
    private final EvidenceRepository evidenceRepository;
    private final ObjectMapper objectMapper;
    private final MatterRepository matterRepository;
    private final EvidenceListDocumentGenerator documentGenerator;
    private final MinioService minioService;
    
    private MatterAppService matterAppService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    public void setMatterAppService(MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 分页查询证据清单
     * ✅ 修复问题579: 添加权限验证
     */
    public PageResult<EvidenceListDTO> listEvidenceLists(Long matterId, String listType, int pageNum, int pageSize) {
        // ✅ 验证项目访问权限
        if (matterId != null) {
            validateMatterAccess(matterId);
        }
        
        IPage<EvidenceList> page = listMapper.selectListPage(
                new Page<>(pageNum, pageSize), matterId, listType);
        List<EvidenceListDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取清单详情
     * ✅ 修复问题580: 添加权限验证 + 优化N+1查询
     */
    public EvidenceListDTO getListById(Long id) {
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
            dto.setEvidences(evidences.stream()
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    /**
     * 创建证据清单
     * ✅ 修复问题589: 添加权限验证
     */
    @Transactional
    public EvidenceListDTO createList(CreateEvidenceListCommand command) {
        // ✅ 验证项目编辑权限
        if (command.getMatterId() != null) {
            validateMatterEditPermission(command.getMatterId());
        }
        
        String listNo = generateListNo();
        String evidenceIdsJson = toJson(command.getEvidenceIds());

        EvidenceList list = EvidenceList.builder()
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
     * 更新证据清单
     * ✅ 修复问题590: 添加权限验证
     */
    @Transactional
    public EvidenceListDTO updateList(Long id, String name, String listType, List<Long> evidenceIds) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        
        // ✅ 验证项目编辑权限
        if (list.getMatterId() != null) {
            validateMatterEditPermission(list.getMatterId());
        }
        
        if (name != null) list.setName(name);
        if (listType != null) list.setListType(listType);
        if (evidenceIds != null) list.setEvidenceIds(toJson(evidenceIds));
        listRepository.updateById(list);
        log.info("证据清单更新成功: {}", list.getName());
        return toDTO(list);
    }

    /**
     * 删除证据清单
     * ✅ 修复问题591: 添加权限验证
     */
    @Transactional
    public void deleteList(Long id) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        
        // ✅ 验证项目编辑权限
        if (list.getMatterId() != null) {
            validateMatterEditPermission(list.getMatterId());
        }
        
        listRepository.removeById(id);
        log.info("证据清单删除成功: {}, 操作人: {}", list.getName(), SecurityUtils.getUserId());
    }

    /**
     * 生成证据清单文件（返回下载URL）
     * ✅ 修复问题592/597: 添加权限验证 + 优化N+1查询
     */
    @Transactional
    public String generateListFile(Long id, String format) {
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
        List<EvidenceDTO> evidences = evidenceList.stream()
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList());

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
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = list.getName() + "_" + dateStr + "." + fileExtension;
        String fileUrl;
        try {
            fileUrl = minioService.uploadFile(
                    new ByteArrayInputStream(documentBytes),
                    fileName,
                    "evidence-list/",
                    contentType
            );
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
     * 导出证据清单为Word格式
     * ✅ 修复问题593/598: 添加权限验证 + 优化N+1查询
     */
    public byte[] exportToWord(Long id) {
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
        List<EvidenceDTO> evidences = evidenceList.stream()
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList());

        // 获取案件信息
        Matter matter = null;
        if (list.getMatterId() != null) {
            matter = matterRepository.findById(list.getMatterId());
        }

        return documentGenerator.generateWordDocument(toDTO(list), matter, evidences);
    }

    /**
     * 导出证据清单为PDF格式
     * ✅ 修复问题594/599: 添加权限验证 + 优化N+1查询
     */
    public byte[] exportToPdf(Long id) {
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
        List<EvidenceDTO> evidences = evidenceList.stream()
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList());

        // 获取案件信息
        Matter matter = null;
        if (list.getMatterId() != null) {
            matter = matterRepository.findById(list.getMatterId());
        }

        return documentGenerator.generatePdfDocument(toDTO(list), matter, evidences);
    }

    /**
     * 按案件获取清单列表
     * ✅ 修复问题595: 添加权限验证
     */
    public List<EvidenceListDTO> getListsByMatter(Long matterId) {
        // ✅ 验证项目访问权限
        validateMatterAccess(matterId);
        
        return listMapper.selectByMatterId(matterId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取案件的所有历史清单（按时间倒序，M6-044）
     */
    public List<EvidenceListDTO> getListHistory(Long matterId) {
        List<EvidenceList> lists = listMapper.selectByMatterId(matterId);
        // 按创建时间倒序排序
        return lists.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 对比两个清单的差异（M6-044）
     * ✅ 修复问题596/600: 添加权限验证 + 优化N+1查询
     * ✅ 修复问题614: 返回类型安全的DTO替代Map<String, Object>
     */
    public EvidenceListCompareResult compareLists(Long listId1, Long listId2) {
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
        EvidenceListCompareResult result = EvidenceListCompareResult.create(
                toDTO(list1), toDTO(list2), added, removed, common);

        // ✅ 批量加载证据详情（优化N+1查询）
        if (!added.isEmpty()) {
            List<Evidence> addedList = evidenceRepository.listByIds(added);
            result.setAddedEvidences(addedList.stream()
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList()));
        }

        if (!removed.isEmpty()) {
            List<Evidence> removedList = evidenceRepository.listByIds(removed);
            result.setRemovedEvidences(removedList.stream()
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList()));
        }

        log.info("清单对比完成: list1={}, list2={}, 新增={}, 删除={}, 共同={}",
                list1.getListNo(), list2.getListNo(), added.size(), removed.size(), common.size());
        return result;
    }

    /**
     * 验证项目访问权限
     */
    private void validateMatterAccess(Long matterId) {
        if (matterId == null) {
            return;
        }
        
        String dataScope = SecurityUtils.getDataScope();
        if ("ALL".equals(dataScope)) {
            return;
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        if (accessibleMatterIds == null) {
            return;
        }
        
        if (!accessibleMatterIds.contains(matterId)) {
            throw new BusinessException("权限不足：无法访问该项目的证据清单");
        }
    }

    /**
     * 验证项目编辑权限
     */
    private void validateMatterEditPermission(Long matterId) {
        if (matterId == null) {
            return;
        }
        
        // 验证访问权限
        validateMatterAccess(matterId);
        
        // 验证是否是项目成员
        matterAppService.validateMatterOwnership(matterId);
        
        // 检查项目状态
        Matter matter = matterRepository.findById(matterId);
        if (matter != null && ("ARCHIVED".equals(matter.getStatus()) || "CLOSED".equals(matter.getStatus()))) {
            String statusName = "ARCHIVED".equals(matter.getStatus()) ? "已归档" : "已结案";
            throw new BusinessException("该项目" + statusName + "，无法编辑证据清单");
        }
    }

    /**
     * 生成证据清单编号
     * ✅ 修复问题609: 使用更可靠的编号生成器替代UUID截断
     */
    private String generateListNo() {
        return com.lawfirm.common.util.NumberGenerator.generateEvidenceListNo();
    }

    /**
     * 将ID列表转换为JSON字符串
     * ✅ 修复问题610: 记录转换失败的异常
     */
    private String toJson(List<Long> ids) {
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
     * 解析证据ID列表JSON字符串
     * ✅ 修复问题611: 记录解析失败的异常
     */
    private List<Long> parseEvidenceIds(String json) {
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

    private String getListTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case EvidenceList.TYPE_SUBMISSION -> "提交清单";
            case EvidenceList.TYPE_EXCHANGE -> "交换清单";
            case EvidenceList.TYPE_COURT -> "庭审清单";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case EvidenceList.STATUS_DRAFT -> "草稿";
            case EvidenceList.STATUS_GENERATED -> "已生成";
            default -> status;
        };
    }

    private EvidenceListDTO toDTO(EvidenceList list) {
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

    private EvidenceDTO toEvidenceDTO(Evidence e) {
        EvidenceDTO dto = new EvidenceDTO();
        dto.setId(e.getId());
        dto.setEvidenceNo(e.getEvidenceNo());
        dto.setName(e.getName());
        dto.setEvidenceType(e.getEvidenceType());
        dto.setGroupName(e.getGroupName());
        dto.setSortOrder(e.getSortOrder());
        dto.setProvePurpose(e.getProvePurpose());
        dto.setIsOriginal(e.getIsOriginal());
        dto.setPageStart(e.getPageStart());
        dto.setPageEnd(e.getPageEnd());
        return dto;
    }
}
