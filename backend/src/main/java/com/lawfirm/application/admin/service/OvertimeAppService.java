package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.command.ApplyOvertimeCommand;
import com.lawfirm.application.admin.dto.OvertimeApplicationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.OvertimeApplication;
import com.lawfirm.domain.admin.repository.OvertimeApplicationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.OvertimeApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 加班申请服务（M8-004）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeAppService {

    private final OvertimeApplicationRepository overtimeRepository;
    private final OvertimeApplicationMapper overtimeMapper;
    private final UserRepository userRepository;

    /**
     * 申请加班
     */
    @Transactional
    public OvertimeApplicationDTO applyOvertime(ApplyOvertimeCommand command) {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();

        // 验证时间
        if (command.getStartTime().isAfter(command.getEndTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        // 计算加班时长
        Duration duration = Duration.between(command.getStartTime(), command.getEndTime());
        BigDecimal overtimeHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // 生成申请编号
        String applicationNo = generateApplicationNo();

        OvertimeApplication application = OvertimeApplication.builder()
                .applicationNo(applicationNo)
                .userId(userId)
                .overtimeDate(command.getOvertimeDate())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .overtimeHours(overtimeHours)
                .reason(command.getReason())
                .workContent(command.getWorkContent())
                .status(OvertimeApplication.STATUS_PENDING)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        overtimeRepository.save(application);
        log.info("加班申请已提交: applicationNo={}, userId={}, hours={}", applicationNo, userId, overtimeHours);
        return toDTO(application);
    }

    /**
     * 审批加班申请
     */
    @Transactional
    public OvertimeApplicationDTO approveOvertime(Long id, boolean approved, String comment) {
        OvertimeApplication application = overtimeRepository.getByIdOrThrow(id, "加班申请不存在");
        
        if (!OvertimeApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("只有待审批的申请可以审批");
        }

        application.setStatus(approved ? OvertimeApplication.STATUS_APPROVED : OvertimeApplication.STATUS_REJECTED);
        application.setApproverId(SecurityUtils.getUserId());
        application.setApprovedAt(LocalDateTime.now());
        application.setApprovalComment(comment);
        application.setUpdatedBy(SecurityUtils.getUserId());
        application.setUpdatedAt(LocalDateTime.now());
        overtimeRepository.updateById(application);

        log.info("加班申请审批完成: applicationNo={}, approved={}", application.getApplicationNo(), approved);
        return toDTO(application);
    }

    /**
     * 查询我的加班申请
     */
    public List<OvertimeApplicationDTO> getMyApplications() {
        Long userId = SecurityUtils.getUserId();
        List<OvertimeApplication> applications = overtimeMapper.selectByUserId(userId);
        return applications.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询指定日期范围的加班申请
     */
    public List<OvertimeApplicationDTO> getApplicationsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getUserId();
        List<OvertimeApplication> applications = overtimeMapper.selectByDateRange(userId, startDate, endDate);
        return applications.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private String generateApplicationNo() {
        String prefix = "OT" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case OvertimeApplication.STATUS_PENDING -> "待审批";
            case OvertimeApplication.STATUS_APPROVED -> "已批准";
            case OvertimeApplication.STATUS_REJECTED -> "已拒绝";
            default -> status;
        };
    }

    private OvertimeApplicationDTO toDTO(OvertimeApplication application) {
        OvertimeApplicationDTO dto = new OvertimeApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationNo(application.getApplicationNo());
        dto.setUserId(application.getUserId());
        dto.setOvertimeDate(application.getOvertimeDate());
        dto.setStartTime(application.getStartTime());
        dto.setEndTime(application.getEndTime());
        dto.setOvertimeHours(application.getOvertimeHours());
        dto.setReason(application.getReason());
        dto.setWorkContent(application.getWorkContent());
        dto.setStatus(application.getStatus());
        dto.setStatusName(getStatusName(application.getStatus()));
        dto.setApproverId(application.getApproverId());
        dto.setApprovedAt(application.getApprovedAt());
        dto.setApprovalComment(application.getApprovalComment());
        dto.setCreatedAt(application.getCreatedAt());
        dto.setUpdatedAt(application.getUpdatedAt());

        // 查询用户名称
        if (application.getUserId() != null) {
            User user = userRepository.findById(application.getUserId());
            if (user != null) {
                dto.setUserName(user.getRealName());
            }
        }
        if (application.getApproverId() != null) {
            User approver = userRepository.findById(application.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }

        return dto;
    }
}

