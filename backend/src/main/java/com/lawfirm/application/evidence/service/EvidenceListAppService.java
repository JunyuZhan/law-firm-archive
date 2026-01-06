package com.lawfirm.application.evidence.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.evidence.command.CreateEvidenceListCommand;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 证据清单应用服务
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

    /**
     * 分页查询证据清单
     */
    public PageResult<EvidenceListDTO> listEvidenceLists(Long matterId, String listType, int pageNum, int pageSize) {
        IPage<EvidenceList> page = listMapper.selectListPage(
                new Page<>(pageNum, pageSize), matterId, listType);
        List<EvidenceListDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取清单详情
     */
    public EvidenceListDTO getListById(Long id) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        EvidenceListDTO dto = toDTO(list);
        // 加载证据详情
        List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
        if (!evidenceIds.isEmpty()) {
            List<EvidenceDTO> evidences = evidenceIds.stream()
                    .map(evidenceRepository::findById)
                    .filter(Objects::nonNull)
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList());
            dto.setEvidences(evidences);
        }
        return dto;
    }

    /**
     * 创建证据清单
     */
    @Transactional
    public EvidenceListDTO createList(CreateEvidenceListCommand command) {
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
        log.info("证据清单创建成功: {}", list.getName());
        return toDTO(list);
    }

    /**
     * 更新证据清单
     */
    @Transactional
    public EvidenceListDTO updateList(Long id, String name, String listType, List<Long> evidenceIds) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        if (name != null) list.setName(name);
        if (listType != null) list.setListType(listType);
        if (evidenceIds != null) list.setEvidenceIds(toJson(evidenceIds));
        listRepository.updateById(list);
        log.info("证据清单更新成功: {}", list.getName());
        return toDTO(list);
    }

    /**
     * 删除证据清单
     */
    @Transactional
    public void deleteList(Long id) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        listRepository.removeById(id);
        log.info("证据清单删除成功: {}", list.getName());
    }

    /**
     * 生成证据清单文件（返回下载URL）
     */
    @Transactional
    public String generateListFile(Long id, String format) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
        
        if (evidenceIds.isEmpty()) {
            throw new BusinessException("清单中没有证据");
        }

        // 获取证据详情
        List<EvidenceDTO> evidences = evidenceIds.stream()
                .map(evidenceRepository::findById)
                .filter(Objects::nonNull)
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList());

        // 获取案件信息
        Matter matter = null;
        if (list.getMatterId() != null) {
            matter = matterRepository.findById(list.getMatterId());
        }

        // 生成文档
        byte[] documentBytes;
        String contentType;
        String fileExtension;
        
        if ("pdf".equalsIgnoreCase(format)) {
            // PDF格式暂时使用Word转换（需要额外库支持）
            // 这里先生成Word，后续可以添加PDF转换
            documentBytes = documentGenerator.generateWordDocument(toDTO(list), matter, evidences);
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            fileExtension = "docx";
            log.warn("PDF格式暂不支持，已生成Word格式");
        } else {
            // 默认生成Word
            documentBytes = documentGenerator.generateWordDocument(toDTO(list), matter, evidences);
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            fileExtension = "docx";
        }

        // 上传到MinIO
        String fileName = list.getName() + "_" + System.currentTimeMillis() + "." + fileExtension;
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
     */
    public byte[] exportToWord(Long id) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
        
        if (evidenceIds.isEmpty()) {
            throw new BusinessException("清单中没有证据");
        }

        // 获取证据详情
        List<EvidenceDTO> evidences = evidenceIds.stream()
                .map(evidenceRepository::findById)
                .filter(Objects::nonNull)
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
     */
    public byte[] exportToPdf(Long id) {
        EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
        List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
        
        if (evidenceIds.isEmpty()) {
            throw new BusinessException("清单中没有证据");
        }

        // 获取证据详情
        List<EvidenceDTO> evidences = evidenceIds.stream()
                .map(evidenceRepository::findById)
                .filter(Objects::nonNull)
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
     */
    public List<EvidenceListDTO> getListsByMatter(Long matterId) {
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
     */
    public Map<String, Object> compareLists(Long listId1, Long listId2) {
        EvidenceList list1 = listRepository.getByIdOrThrow(listId1, "清单1不存在");
        EvidenceList list2 = listRepository.getByIdOrThrow(listId2, "清单2不存在");

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

        Map<String, Object> result = new HashMap<>();
        result.put("list1", toDTO(list1));
        result.put("list2", toDTO(list2));
        result.put("addedEvidenceIds", added);
        result.put("removedEvidenceIds", removed);
        result.put("commonEvidenceIds", common);
        result.put("addedCount", added.size());
        result.put("removedCount", removed.size());
        result.put("commonCount", common.size());

        // 加载证据详情
        if (!added.isEmpty()) {
            List<EvidenceDTO> addedEvidences = added.stream()
                    .map(evidenceRepository::findById)
                    .filter(Objects::nonNull)
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList());
            result.put("addedEvidences", addedEvidences);
        }

        if (!removed.isEmpty()) {
            List<EvidenceDTO> removedEvidences = removed.stream()
                    .map(evidenceRepository::findById)
                    .filter(Objects::nonNull)
                    .map(this::toEvidenceDTO)
                    .collect(Collectors.toList());
            result.put("removedEvidences", removedEvidences);
        }

        log.info("清单对比完成: list1={}, list2={}, 新增={}, 删除={}, 共同={}",
                list1.getListNo(), list2.getListNo(), added.size(), removed.size(), common.size());
        return result;
    }

    private String generateListNo() {
        String prefix = "EL" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<Long> parseEvidenceIds(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
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
