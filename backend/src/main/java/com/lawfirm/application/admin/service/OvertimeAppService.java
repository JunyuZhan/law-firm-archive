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
import java.util.*;
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
     * ✅ 修复：添加完善的业务验证
     */
    @Transactional
    public OvertimeApplicationDTO applyOvertime(ApplyOvertimeCommand command) {
        Long userId = SecurityUtils.getUserId();

        // ✅ 验证时间顺序（同一天内的时间比较）
        // 如果结束时间小于开始时间，说明是跨天加班（如 22:00 到 02:00）
        boolean isCrossDay = command.getEndTime().isBefore(command.getStartTime());
        if (isCrossDay) {
            log.info("跨天加班申请: userId={}, date={}, startTime={}, endTime={}",
                    userId, command.getOvertimeDate(), command.getStartTime(), command.getEndTime());
        }

        // 计算加班时长
        Duration duration;
        if (isCrossDay) {
            // 跨天：从开始时间到24:00 + 从00:00到结束时间
            duration = Duration.between(command.getStartTime(), java.time.LocalTime.MAX)
                    .plus(Duration.between(java.time.LocalTime.MIN, command.getEndTime()));
        } else {
            duration = Duration.between(command.getStartTime(), command.getEndTime());
        }
        BigDecimal overtimeHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // ✅ 验证加班时长合理性（不超过12小时）
        if (overtimeHours.compareTo(BigDecimal.valueOf(12)) > 0) {
            throw new BusinessException("单次加班时长不能超过12小时");
        }
        if (overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("加班时长必须大于0");
        }

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
     * ✅ 修复：添加权限验证
     */
    @Transactional
    public OvertimeApplicationDTO approveOvertime(Long id, boolean approved, String comment) {
        OvertimeApplication application = overtimeRepository.getByIdOrThrow(id, "加班申请不存在");
        
        if (!OvertimeApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("只有待审批的申请可以审批");
        }

        Long approverId = SecurityUtils.getUserId();

        // ✅ 验证审批权限：只有管理员或部门主管才能审批
        if (!SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER", "DEPT_MANAGER")) {
            throw new BusinessException("权限不足：只有管理员或部门主管才能审批加班申请");
        }

        // ✅ 防止自己审批自己
        if (application.getUserId().equals(approverId)) {
            throw new BusinessException("不能审批自己的加班申请");
        }

        application.setStatus(approved ? OvertimeApplication.STATUS_APPROVED : OvertimeApplication.STATUS_REJECTED);
        application.setApproverId(approverId);
        application.setApprovedAt(LocalDateTime.now());
        application.setApprovalComment(comment);
        application.setUpdatedBy(approverId);
        application.setUpdatedAt(LocalDateTime.now());
        overtimeRepository.updateById(application);

        log.info("加班申请审批完成: applicationNo={}, approved={}, approver={}", 
                application.getApplicationNo(), approved, approverId);
        return toDTO(application);
    }

    /**
     * 查询我的加班申请
     * ✅ 优化：使用批量加载避免N+1查询
     */
    public List<OvertimeApplicationDTO> getMyApplications() {
        Long userId = SecurityUtils.getUserId();
        List<OvertimeApplication> applications = overtimeMapper.selectByUserId(userId);
        return convertToDTOs(applications);
    }

    /**
     * 查询指定日期范围的加班申请
     * ✅ 优化：使用批量加载避免N+1查询
     */
    public List<OvertimeApplicationDTO> getApplicationsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getUserId();
        List<OvertimeApplication> applications = overtimeMapper.selectByDateRange(userId, startDate, endDate);
        return convertToDTOs(applications);
    }

    /**
     * 批量转换DTO
     */
    private List<OvertimeApplicationDTO> convertToDTOs(List<OvertimeApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量加载用户信息（申请人和审批人）
        Set<Long> userIds = new HashSet<>();
        applications.forEach(app -> {
            if (app.getUserId() != null) userIds.add(app.getUserId());
            if (app.getApproverId() != null) userIds.add(app.getApproverId());
        });

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.listByIds(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 转换DTO(从Map获取)
        return applications.stream()
                .map(app -> toDTO(app, userMap))
                .collect(Collectors.toList());
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

    /**
     * 转换为DTO（单条查询使用，会触发数据库查询）
     */
    private OvertimeApplicationDTO toDTO(OvertimeApplication application) {
        Map<Long, User> userMap = new HashMap<>();
        if (application.getUserId() != null) {
            User user = userRepository.findById(application.getUserId());
            if (user != null) userMap.put(user.getId(), user);
        }
        if (application.getApproverId() != null) {
            User approver = userRepository.findById(application.getApproverId());
            if (approver != null) userMap.put(approver.getId(), approver);
        }
        return toDTO(application, userMap);
    }

    /**
     * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）
     */
    private OvertimeApplicationDTO toDTO(OvertimeApplication application, Map<Long, User> userMap) {
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

        // 从Map获取用户名称（避免N+1）
        if (application.getUserId() != null) {
            User user = userMap.get(application.getUserId());
            if (user != null) {
                dto.setUserName(user.getRealName());
            }
        }
        if (application.getApproverId() != null) {
            User approver = userMap.get(application.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }

        return dto;
    }
}

