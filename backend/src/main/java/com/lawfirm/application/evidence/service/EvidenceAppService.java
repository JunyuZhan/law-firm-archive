package com.lawfirm.application.evidence.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceCrossExam;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceCrossExamMapper;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 证据应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceAppService {

    private final EvidenceRepository evidenceRepository;
    private final EvidenceMapper evidenceMapper;
    private final EvidenceCrossExamMapper crossExamMapper;

    /**
     * 分页查询证据
     */
    public PageResult<EvidenceDTO> listEvidence(EvidenceQueryDTO query) {
        IPage<Evidence> page = evidenceMapper.selectEvidencePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getMatterId(),
                query.getName(),
                query.getEvidenceType(),
                query.getGroupName(),
                query.getCrossExamStatus()
        );

        List<EvidenceDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建证据
     */
    @Transactional
    public EvidenceDTO createEvidence(CreateEvidenceCommand command) {
        String evidenceNo = generateEvidenceNo();

        // 获取排序号
        Integer maxSort = evidenceRepository.getMaxSortOrder(command.getMatterId(), command.getGroupName());
        int sortOrder = (maxSort != null ? maxSort : 0) + 1;

        Evidence evidence = Evidence.builder()
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
                .fileUrl(command.getFileUrl())
                .fileName(command.getFileName())
                .fileSize(command.getFileSize())
                .crossExamStatus("PENDING")
                .status("ACTIVE")
                .build();

        evidenceRepository.save(evidence);
        log.info("证据创建成功: {} ({})", evidence.getName(), evidence.getEvidenceNo());
        return toDTO(evidence);
    }

    /**
     * 获取证据详情
     */
    public EvidenceDTO getEvidenceById(Long id) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
        EvidenceDTO dto = toDTO(evidence);
        
        // 加载质证记录
        List<EvidenceCrossExam> crossExams = crossExamMapper.selectByEvidenceId(id);
        dto.setCrossExams(crossExams.stream().map(this::toCrossExamDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 更新证据
     */
    @Transactional
    public EvidenceDTO updateEvidence(Long id, UpdateEvidenceCommand command) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");

        if (StringUtils.hasText(command.getName())) evidence.setName(command.getName());
        if (StringUtils.hasText(command.getEvidenceType())) evidence.setEvidenceType(command.getEvidenceType());
        if (command.getSource() != null) evidence.setSource(command.getSource());
        if (command.getGroupName() != null) evidence.setGroupName(command.getGroupName());
        if (command.getProvePurpose() != null) evidence.setProvePurpose(command.getProvePurpose());
        if (command.getDescription() != null) evidence.setDescription(command.getDescription());
        if (command.getIsOriginal() != null) evidence.setIsOriginal(command.getIsOriginal());
        if (command.getOriginalCount() != null) evidence.setOriginalCount(command.getOriginalCount());
        if (command.getCopyCount() != null) evidence.setCopyCount(command.getCopyCount());
        if (command.getPageStart() != null) evidence.setPageStart(command.getPageStart());
        if (command.getPageEnd() != null) evidence.setPageEnd(command.getPageEnd());

        evidenceRepository.updateById(evidence);
        log.info("证据更新成功: {}", evidence.getName());
        return toDTO(evidence);
    }

    /**
     * 删除证据
     */
    @Transactional
    public void deleteEvidence(Long id) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
        evidenceRepository.removeById(id);
        log.info("证据删除成功: {}", evidence.getName());
    }

    /**
     * 调整证据排序
     */
    @Transactional
    public void updateSortOrder(Long id, Integer sortOrder) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
        evidence.setSortOrder(sortOrder);
        evidenceRepository.updateById(evidence);
    }

    /**
     * 批量调整分组
     */
    @Transactional
    public void batchUpdateGroup(List<Long> ids, String groupName) {
        for (Long id : ids) {
            Evidence evidence = evidenceRepository.findById(id);
            if (evidence != null) {
                evidence.setGroupName(groupName);
                evidenceRepository.updateById(evidence);
            }
        }
        log.info("批量调整证据分组成功，共{}条", ids.size());
    }

    /**
     * 添加质证记录
     */
    @Transactional
    public EvidenceCrossExamDTO addCrossExam(CreateCrossExamCommand command) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(command.getEvidenceId(), "证据不存在");

        // 检查是否已有该方的质证记录
        EvidenceCrossExam existing = crossExamMapper.selectByEvidenceIdAndParty(
                command.getEvidenceId(), command.getExamParty());
        if (existing != null) {
            throw new BusinessException("该方已有质证记录，请编辑现有记录");
        }

        EvidenceCrossExam crossExam = EvidenceCrossExam.builder()
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
     * 完成质证
     */
    @Transactional
    public void completeCrossExam(Long evidenceId) {
        Evidence evidence = evidenceRepository.getByIdOrThrow(evidenceId, "证据不存在");
        evidence.setCrossExamStatus("COMPLETED");
        evidenceRepository.updateById(evidence);
        log.info("证据质证完成: {}", evidence.getName());
    }

    /**
     * 按案件获取证据列表
     */
    public List<EvidenceDTO> getEvidenceByMatter(Long matterId) {
        List<Evidence> evidences = evidenceRepository.findByMatterId(matterId);
        return evidences.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取案件的证据分组
     */
    public List<String> getEvidenceGroups(Long matterId) {
        return evidenceRepository.findGroupsByMatterId(matterId);
    }

    /**
     * 生成证据编号
     */
    private String generateEvidenceNo() {
        String prefix = "EV" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null) return null;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") 
            || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp")) {
            return "image";
        } else if (lowerName.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "word";
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "excel";
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") || lowerName.endsWith(".mov")) {
            return "video";
        } else if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a")) {
            return "audio";
        } else {
            return "other";
        }
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") 
            || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
    }

    /**
     * 获取证据类型名称
     */
    private String getEvidenceTypeName(String type) {
        if (type == null) return null;
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
     */
    private String getCrossExamStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待质证";
            case "IN_PROGRESS" -> "质证中";
            case "COMPLETED" -> "已质证";
            default -> status;
        };
    }

    /**
     * 获取质证方名称
     */
    private String getExamPartyName(String party) {
        if (party == null) return null;
        return switch (party) {
            case "OUR_SIDE" -> "我方";
            case "OPPOSITE" -> "对方";
            case "COURT" -> "法院";
            default -> party;
        };
    }

    /**
     * 获取意见名称
     */
    private String getOpinionName(String opinion) {
        if (opinion == null) return null;
        return switch (opinion) {
            case "ACCEPT" -> "认可";
            case "PARTIAL" -> "部分认可";
            case "REJECT" -> "不认可";
            default -> opinion;
        };
    }

    /**
     * Entity 转 DTO
     */
    private EvidenceDTO toDTO(Evidence evidence) {
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
        // 设置文件类型（前端直接使用fileUrl显示预览图，不需要后端生成缩略图）
        if (evidence.getFileName() != null) {
            dto.setFileType(getFileType(evidence.getFileName()));
            // thumbnailUrl 保留用于兼容，但前端已改为直接使用 fileUrl
            if (isImageFile(evidence.getFileName()) && evidence.getFileUrl() != null) {
                dto.setThumbnailUrl(evidence.getFileUrl());
            }
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
     * CrossExam Entity 转 DTO
     */
    private EvidenceCrossExamDTO toCrossExamDTO(EvidenceCrossExam exam) {
        EvidenceCrossExamDTO dto = new EvidenceCrossExamDTO();
        dto.setId(exam.getId());
        dto.setEvidenceId(exam.getEvidenceId());
        dto.setExamParty(exam.getExamParty());
        dto.setExamPartyName(getExamPartyName(exam.getExamParty()));
        dto.setAuthenticityOpinion(exam.getAuthenticityOpinion());
        dto.setAuthenticityOpinionName(getOpinionName(exam.getAuthenticityOpinion()));
        dto.setAuthenticityReason(exam.getAuthenticityReason());
        dto.setLegalityOpinion(exam.getLegalityOpinion());
        dto.setLegalityOpinionName(getOpinionName(exam.getLegalityOpinion()));
        dto.setLegalityReason(exam.getLegalityReason());
        dto.setRelevanceOpinion(exam.getRelevanceOpinion());
        dto.setRelevanceOpinionName(getOpinionName(exam.getRelevanceOpinion()));
        dto.setRelevanceReason(exam.getRelevanceReason());
        dto.setOverallOpinion(exam.getOverallOpinion());
        dto.setCourtOpinion(exam.getCourtOpinion());
        dto.setCourtAccepted(exam.getCourtAccepted());
        dto.setCreatedBy(exam.getCreatedBy());
        dto.setCreatedAt(exam.getCreatedAt());
        return dto;
    }
}
