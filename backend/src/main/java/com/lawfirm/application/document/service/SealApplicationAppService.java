package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateSealApplicationCommand;
import com.lawfirm.application.document.dto.SealApplicationDTO;
import com.lawfirm.application.document.dto.SealApplicationQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.domain.document.entity.SealApplication;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.domain.document.repository.SealApplicationRepository;
import com.lawfirm.domain.document.repository.SealRepository;
import com.lawfirm.infrastructure.persistence.mapper.SealApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用印申请应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealApplicationAppService {

    private final SealApplicationRepository applicationRepository;
    private final SealRepository sealRepository;
    private final SealApplicationMapper applicationMapper;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 分页查询用印申请
     */
    public PageResult<SealApplicationDTO> listApplications(SealApplicationQueryDTO query) {
        IPage<SealApplication> page = applicationMapper.selectApplicationPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getApplicantId(),
                query.getSealId(),
                query.getMatterId(),
                query.getStatus()
        );

        List<SealApplicationDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建用印申请
     */
    @Transactional
    public SealApplicationDTO createApplication(CreateSealApplicationCommand command) {
        // 验证印章
        Seal seal = sealRepository.getByIdOrThrow(command.getSealId(), "印章不存在");
        if (!"ACTIVE".equals(seal.getStatus())) {
            throw new BusinessException("印章不可用");
        }

        String applicationNo = generateApplicationNo();
        Long userId = SecurityUtils.getUserId();

        SealApplication application = SealApplication.builder()
                .applicationNo(applicationNo)
                .applicantId(userId)
                .applicantName(SecurityUtils.getUsername())
                .sealId(command.getSealId())
                .sealName(seal.getName())
                .matterId(command.getMatterId())
                .documentName(command.getDocumentName())
                .documentType(command.getDocumentType())
                .copies(command.getCopies() != null ? command.getCopies() : 1)
                .usePurpose(command.getUsePurpose())
                .expectedUseDate(command.getExpectedUseDate())
                .status("PENDING")
                .build();

        applicationRepository.save(application);
        
        // 创建审批记录
        Long approverId = approverService.findSealApplicationApprover();
        if (approverId == null) {
            approverId = approverService.findDefaultApprover();
        }
        
        approvalService.createApproval(
                "SEAL_APPLICATION",
                application.getId(),
                application.getApplicationNo(),
                application.getDocumentName(),
                approverId,
                "MEDIUM",
                "NORMAL",
                null  // businessSnapshot
        );
        
        log.info("用印申请创建成功: {} ({}) (审批人: {})", application.getDocumentName(), application.getApplicationNo(), approverId);
        return toDTO(application);
    }

    /**
     * 获取申请详情
     */
    public SealApplicationDTO getApplicationById(Long id) {
        SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
        return toDTO(application);
    }

    /**
     * 审批通过
     */
    @Transactional
    public SealApplicationDTO approve(Long id, String comment) {
        SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("只能审批待审批的申请");
        }

        application.setStatus("APPROVED");
        application.setApprovedBy(SecurityUtils.getUserId());
        application.setApprovedAt(LocalDateTime.now());
        application.setApprovalComment(comment);

        applicationRepository.updateById(application);
        log.info("用印申请审批通过: {}", application.getApplicationNo());
        return toDTO(application);
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public SealApplicationDTO reject(Long id, String comment) {
        SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("只能审批待审批的申请");
        }

        application.setStatus("REJECTED");
        application.setApprovedBy(SecurityUtils.getUserId());
        application.setApprovedAt(LocalDateTime.now());
        application.setApprovalComment(comment);

        applicationRepository.updateById(application);
        log.info("用印申请审批拒绝: {}", application.getApplicationNo());
        return toDTO(application);
    }

    /**
     * 登记用印
     */
    @Transactional
    public SealApplicationDTO registerUsage(Long id, String remark) {
        SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        if (!"APPROVED".equals(application.getStatus())) {
            throw new BusinessException("只能对已批准的申请登记用印");
        }

        application.setStatus("USED");
        application.setUsedBy(SecurityUtils.getUserId());
        application.setUsedAt(LocalDateTime.now());
        application.setActualUseDate(LocalDate.now());
        application.setUseRemark(remark);

        applicationRepository.updateById(application);
        log.info("用印登记成功: {}", application.getApplicationNo());
        return toDTO(application);
    }

    /**
     * 取消申请
     */
    @Transactional
    public void cancelApplication(Long id) {
        SealApplication application = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("只能取消待审批的申请");
        }

        // 验证是否是申请人本人
        if (!application.getApplicantId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能取消自己的申请");
        }

        application.setStatus("CANCELLED");
        applicationRepository.updateById(application);
        log.info("用印申请已取消: {}", application.getApplicationNo());
    }

    /**
     * 获取待审批列表
     */
    public List<SealApplicationDTO> getPendingApplications() {
        List<SealApplication> applications = applicationRepository.findPendingApplications();
        return applications.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 生成申请编号
     */
    private String generateApplicationNo() {
        String prefix = "SA" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            case "USED" -> "已用印";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private SealApplicationDTO toDTO(SealApplication app) {
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
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }
}
