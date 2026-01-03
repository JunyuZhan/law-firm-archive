package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.ApproveRegularizationCommand;
import com.lawfirm.application.hr.command.CreateRegularizationCommand;
import com.lawfirm.application.hr.dto.RegularizationDTO;
import com.lawfirm.application.hr.dto.RegularizationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.Regularization;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.RegularizationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.RegularizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 转正申请应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegularizationAppService {

    private final RegularizationRepository regularizationRepository;
    private final RegularizationMapper regularizationMapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 分页查询转正申请
     */
    public PageResult<RegularizationDTO> listRegularizations(RegularizationQueryDTO query) {
        IPage<Regularization> page = regularizationMapper.selectRegularizationPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getEmployeeId(),
                query.getStatus()
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
                page.getTotal(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    /**
     * 根据ID查询转正申请
     */
    public RegularizationDTO getRegularizationById(Long id) {
        Regularization regularization = regularizationRepository.getByIdOrThrow(id, "转正申请不存在");
        return toDTO(regularization);
    }

    /**
     * 根据员工ID查询转正申请
     */
    public List<RegularizationDTO> getRegularizationsByEmployeeId(Long employeeId) {
        List<Regularization> regularizations = regularizationRepository.findByEmployeeId(employeeId);
        return regularizations.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建转正申请
     */
    @Transactional
    public RegularizationDTO createRegularization(CreateRegularizationCommand command) {
        // 验证员工存在
        Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

        // 检查是否已有待审批的转正申请
        List<Regularization> pendingApplications = regularizationRepository.findByEmployeeId(command.getEmployeeId())
                .stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
        if (!pendingApplications.isEmpty()) {
            throw new BusinessException("该员工已有待审批的转正申请");
        }

        // 生成申请编号
        String applicationNo = generateApplicationNo();

        // 获取试用期信息
        LocalDate probationStartDate = command.getProbationStartDate();
        LocalDate probationEndDate = command.getProbationEndDate();
        if (probationStartDate == null && employee.getEntryDate() != null) {
            probationStartDate = employee.getEntryDate();
        }
        if (probationEndDate == null && employee.getProbationEndDate() != null) {
            probationEndDate = employee.getProbationEndDate();
        }

        // 预计转正日期
        LocalDate expectedRegularDate = command.getExpectedRegularDate();
        if (expectedRegularDate == null && probationEndDate != null) {
            expectedRegularDate = probationEndDate;
        }

        // 创建转正申请
        Regularization regularization = Regularization.builder()
                .employeeId(command.getEmployeeId())
                .applicationNo(applicationNo)
                .probationStartDate(probationStartDate)
                .probationEndDate(probationEndDate)
                .applicationDate(LocalDate.now())
                .expectedRegularDate(expectedRegularDate)
                .selfEvaluation(command.getSelfEvaluation())
                .status("PENDING")
                .build();

        regularizationRepository.save(regularization);

        // 创建审批记录
        Long approverId = approverService.findDefaultApprover();
        approvalService.createApproval(
                "REGULARIZATION",
                regularization.getId(),
                applicationNo,
                "转正申请：" + (employee.getEmployeeNo() != null ? employee.getEmployeeNo() : ""),
                approverId,
                "MEDIUM",
                "NORMAL",
                null
        );

        log.info("创建转正申请成功: {} ({})", applicationNo, employee.getEmployeeNo());
        return toDTO(regularization);
    }

    /**
     * 审批转正申请
     */
    @Transactional
    public RegularizationDTO approveRegularization(Long id, ApproveRegularizationCommand command) {
        Regularization regularization = regularizationRepository.getByIdOrThrow(id, "转正申请不存在");

        if (!"PENDING".equals(regularization.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        Long approverId = SecurityUtils.getUserId();
        regularization.setApproverId(approverId);
        regularization.setApprovedDate(LocalDate.now());
        regularization.setComment(command.getComment());
        regularization.setStatus(command.getApproved() ? "APPROVED" : "REJECTED");

        regularizationRepository.updateById(regularization);

        // 如果审批通过，更新员工档案
        if (command.getApproved()) {
            Employee employee = employeeRepository.getByIdOrThrow(regularization.getEmployeeId(), "员工不存在");
            employee.setRegularDate(regularization.getExpectedRegularDate() != null ? 
                    regularization.getExpectedRegularDate() : LocalDate.now());
            employee.setWorkStatus("ACTIVE");
            employeeRepository.updateById(employee);
            log.info("转正申请审批通过，已更新员工档案: {}", employee.getEmployeeNo());
        }

        log.info("审批转正申请: {} ({})", id, command.getApproved() ? "通过" : "拒绝");
        return toDTO(regularization);
    }

    /**
     * 删除转正申请（软删除）
     */
    @Transactional
    public void deleteRegularization(Long id) {
        Regularization regularization = regularizationRepository.getByIdOrThrow(id, "转正申请不存在");
        if (!"PENDING".equals(regularization.getStatus())) {
            throw new BusinessException("只有待审批状态的申请可以删除");
        }
        regularizationRepository.softDelete(id);
        log.info("删除转正申请: {}", id);
    }

    /**
     * 转换为DTO
     */
    private RegularizationDTO toDTO(Regularization regularization) {
        RegularizationDTO dto = new RegularizationDTO();
        dto.setId(regularization.getId());
        dto.setEmployeeId(regularization.getEmployeeId());
        dto.setApplicationNo(regularization.getApplicationNo());
        dto.setProbationStartDate(regularization.getProbationStartDate());
        dto.setProbationEndDate(regularization.getProbationEndDate());
        dto.setApplicationDate(regularization.getApplicationDate());
        dto.setExpectedRegularDate(regularization.getExpectedRegularDate());
        dto.setSelfEvaluation(regularization.getSelfEvaluation());
        dto.setSupervisorEvaluation(regularization.getSupervisorEvaluation());
        dto.setHrEvaluation(regularization.getHrEvaluation());
        dto.setStatus(regularization.getStatus());
        dto.setApproverId(regularization.getApproverId());
        dto.setApprovedDate(regularization.getApprovedDate());
        dto.setComment(regularization.getComment());
        dto.setCreatedAt(regularization.getCreatedAt());
        dto.setUpdatedAt(regularization.getUpdatedAt());

        // 加载员工和用户信息
        if (regularization.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(regularization.getEmployeeId());
            if (employee != null && employee.getUserId() != null) {
                dto.setUserId(employee.getUserId());
                User user = userRepository.findById(employee.getUserId());
                if (user != null) {
                    dto.setEmployeeName(user.getRealName());
                }
            }
        }

        // 加载审批人信息
        if (regularization.getApproverId() != null) {
            User approver = userRepository.findById(regularization.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }

        // 设置状态名称
        if (dto.getStatus() != null) {
            switch (dto.getStatus()) {
                case "PENDING" -> dto.setStatusName("待审批");
                case "APPROVED" -> dto.setStatusName("已通过");
                case "REJECTED" -> dto.setStatusName("已拒绝");
                default -> dto.setStatusName(dto.getStatus());
            }
        }

        return dto;
    }

    /**
     * 生成申请编号
     */
    private String generateApplicationNo() {
        String prefix = "REG";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + timestamp;
    }
}

