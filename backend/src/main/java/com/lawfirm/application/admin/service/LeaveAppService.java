package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.ApplyLeaveCommand;
import com.lawfirm.application.admin.command.ApproveLeaveCommand;
import com.lawfirm.application.admin.dto.*;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.LeaveApplication;
import com.lawfirm.domain.admin.entity.LeaveBalance;
import com.lawfirm.domain.admin.entity.LeaveType;
import com.lawfirm.domain.admin.repository.LeaveApplicationRepository;
import com.lawfirm.domain.admin.repository.LeaveBalanceRepository;
import com.lawfirm.domain.admin.repository.LeaveTypeRepository;
import com.lawfirm.infrastructure.persistence.mapper.LeaveApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveBalanceMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 请假应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveAppService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceMapper leaveBalanceMapper;

    /**
     * 获取所有请假类型
     */
    public List<LeaveTypeDTO> listLeaveTypes() {
        return leaveTypeMapper.selectEnabledTypes().stream()
                .map(this::toLeaveTypeDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询请假申请
     */
    public PageResult<LeaveApplicationDTO> listApplications(LeaveApplicationQueryDTO query) {
        IPage<LeaveApplication> page = leaveApplicationMapper.selectApplicationPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getUserId(),
                query.getLeaveTypeId(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime()
        );

        List<LeaveApplicationDTO> records = page.getRecords().stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 提交请假申请
     */
    @Transactional
    public LeaveApplicationDTO applyLeave(ApplyLeaveCommand command) {
        Long userId = SecurityUtils.getUserId();

        // 验证请假类型
        LeaveType leaveType = leaveTypeRepository.getByIdOrThrow(command.getLeaveTypeId(), "请假类型不存在");
        if (!leaveType.getEnabled()) {
            throw new BusinessException("该请假类型已禁用");
        }

        // 验证时间
        if (command.getStartTime().isAfter(command.getEndTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if (command.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("开始时间不能早于当前时间");
        }

        // 检查时间段是否有重叠
        int overlapping = leaveApplicationMapper.countOverlapping(userId, command.getStartTime(), command.getEndTime());
        if (overlapping > 0) {
            throw new BusinessException("该时间段已有请假申请");
        }

        // 检查假期余额（如果有限额）
        if (leaveType.getAnnualLimit() != null) {
            int year = command.getStartTime().getYear();
            LeaveBalance balance = leaveBalanceMapper.selectByUserTypeYear(userId, leaveType.getId(), year);
            if (balance == null) {
                throw new BusinessException("您没有该类型的假期余额，请联系管理员");
            }
            if (balance.getRemainingDays().compareTo(command.getDuration()) < 0) {
                throw new BusinessException("假期余额不足，剩余" + balance.getRemainingDays() + "天");
            }
        }

        // 生成申请编号
        String applicationNo = generateApplicationNo();

        // 创建申请
        LeaveApplication application = LeaveApplication.builder()
                .applicationNo(applicationNo)
                .userId(userId)
                .leaveTypeId(command.getLeaveTypeId())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .duration(command.getDuration())
                .reason(command.getReason())
                .attachmentUrl(command.getAttachmentUrl())
                .status(LeaveApplication.STATUS_PENDING)
                .build();

        leaveApplicationRepository.save(application);
        log.info("请假申请提交成功: {} ({})", applicationNo, leaveType.getName());
        return toApplicationDTO(application);
    }

    /**
     * 审批请假申请
     */
    @Transactional
    public LeaveApplicationDTO approveLeave(ApproveLeaveCommand command) {
        Long approverId = SecurityUtils.getUserId();

        LeaveApplication application = leaveApplicationRepository.getByIdOrThrow(
                command.getApplicationId(), "请假申请不存在");

        if (!LeaveApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        application.setApproverId(approverId);
        application.setApprovedAt(LocalDateTime.now());
        application.setApprovalComment(command.getComment());

        if (command.getApproved()) {
            application.setStatus(LeaveApplication.STATUS_APPROVED);

            // 扣减假期余额
            LeaveType leaveType = leaveTypeRepository.getById(application.getLeaveTypeId());
            if (leaveType != null && leaveType.getAnnualLimit() != null) {
                int year = application.getStartTime().getYear();
                int updated = leaveBalanceMapper.deductBalance(
                        application.getUserId(),
                        application.getLeaveTypeId(),
                        year,
                        application.getDuration()
                );
                if (updated == 0) {
                    throw new BusinessException("扣减假期余额失败，余额不足");
                }
            }

            log.info("请假申请已批准: {}", application.getApplicationNo());
        } else {
            application.setStatus(LeaveApplication.STATUS_REJECTED);
            log.info("请假申请已拒绝: {}", application.getApplicationNo());
        }

        leaveApplicationRepository.updateById(application);
        return toApplicationDTO(application);
    }

    /**
     * 取消请假申请
     */
    @Transactional
    public void cancelApplication(Long applicationId) {
        Long userId = SecurityUtils.getUserId();

        LeaveApplication application = leaveApplicationRepository.getByIdOrThrow(applicationId, "请假申请不存在");

        if (!application.getUserId().equals(userId)) {
            throw new BusinessException("只能取消自己的申请");
        }
        if (!LeaveApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("只能取消待审批的申请");
        }

        application.setStatus(LeaveApplication.STATUS_CANCELLED);
        leaveApplicationRepository.updateById(application);
        log.info("请假申请已取消: {}", application.getApplicationNo());
    }

    /**
     * 获取用户假期余额
     */
    public List<LeaveBalanceDTO> getUserBalance(Long userId, Integer year) {
        if (userId == null) {
            userId = SecurityUtils.getUserId();
        }
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        return leaveBalanceMapper.selectByUserAndYear(userId, year).stream()
                .map(this::toBalanceDTO)
                .collect(Collectors.toList());
    }

    /**
     * 初始化用户年度假期余额
     */
    @Transactional
    public void initUserBalance(Long userId, Integer year) {
        List<LeaveType> types = leaveTypeMapper.selectEnabledTypes();
        for (LeaveType type : types) {
            if (type.getAnnualLimit() != null) {
                LeaveBalance existing = leaveBalanceMapper.selectByUserTypeYear(userId, type.getId(), year);
                if (existing == null) {
                    LeaveBalance balance = LeaveBalance.builder()
                            .userId(userId)
                            .leaveTypeId(type.getId())
                            .year(year)
                            .totalDays(type.getAnnualLimit())
                            .usedDays(BigDecimal.ZERO)
                            .remainingDays(type.getAnnualLimit())
                            .build();
                    leaveBalanceRepository.save(balance);
                }
            }
        }
        log.info("用户假期余额初始化完成: userId={}, year={}", userId, year);
    }

    /**
     * 获取待审批列表
     */
    public List<LeaveApplicationDTO> getPendingApplications() {
        return leaveApplicationMapper.selectPendingApplications().stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 生成申请编号
     */
    private String generateApplicationNo() {
        String prefix = "LV" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case LeaveApplication.STATUS_PENDING -> "待审批";
            case LeaveApplication.STATUS_APPROVED -> "已批准";
            case LeaveApplication.STATUS_REJECTED -> "已拒绝";
            case LeaveApplication.STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }

    private LeaveTypeDTO toLeaveTypeDTO(LeaveType type) {
        LeaveTypeDTO dto = new LeaveTypeDTO();
        dto.setId(type.getId());
        dto.setName(type.getName());
        dto.setCode(type.getCode());
        dto.setPaid(type.getPaid());
        dto.setAnnualLimit(type.getAnnualLimit());
        dto.setNeedApproval(type.getNeedApproval());
        dto.setDescription(type.getDescription());
        dto.setSortOrder(type.getSortOrder());
        dto.setEnabled(type.getEnabled());
        return dto;
    }

    private LeaveApplicationDTO toApplicationDTO(LeaveApplication app) {
        LeaveApplicationDTO dto = new LeaveApplicationDTO();
        dto.setId(app.getId());
        dto.setApplicationNo(app.getApplicationNo());
        dto.setUserId(app.getUserId());
        dto.setLeaveTypeId(app.getLeaveTypeId());
        dto.setStartTime(app.getStartTime());
        dto.setEndTime(app.getEndTime());
        dto.setDuration(app.getDuration());
        dto.setReason(app.getReason());
        dto.setAttachmentUrl(app.getAttachmentUrl());
        dto.setStatus(app.getStatus());
        dto.setStatusName(getStatusName(app.getStatus()));
        dto.setApproverId(app.getApproverId());
        dto.setApprovedAt(app.getApprovedAt());
        dto.setApprovalComment(app.getApprovalComment());
        dto.setCreatedAt(app.getCreatedAt());

        // 获取请假类型名称
        LeaveType type = leaveTypeRepository.getById(app.getLeaveTypeId());
        if (type != null) {
            dto.setLeaveTypeName(type.getName());
        }

        return dto;
    }

    private LeaveBalanceDTO toBalanceDTO(LeaveBalance balance) {
        LeaveBalanceDTO dto = new LeaveBalanceDTO();
        dto.setId(balance.getId());
        dto.setUserId(balance.getUserId());
        dto.setLeaveTypeId(balance.getLeaveTypeId());
        dto.setYear(balance.getYear());
        dto.setTotalDays(balance.getTotalDays());
        dto.setUsedDays(balance.getUsedDays());
        dto.setRemainingDays(balance.getRemainingDays());

        // 获取请假类型名称
        LeaveType type = leaveTypeRepository.getById(balance.getLeaveTypeId());
        if (type != null) {
            dto.setLeaveTypeName(type.getName());
        }

        return dto;
    }
}
