package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.command.CreateResignationCommand;
import com.lawfirm.application.hr.dto.ResignationDTO;
import com.lawfirm.application.hr.dto.ResignationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.Resignation;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.ResignationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ResignationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 离职申请应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResignationAppService {

    private final ResignationRepository resignationRepository;
    private final ResignationMapper resignationMapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 分页查询离职申请
     */
    public PageResult<ResignationDTO> listResignations(ResignationQueryDTO query) {
        IPage<Resignation> page = resignationMapper.selectResignationPage(
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
     * 根据ID查询离职申请
     */
    public ResignationDTO getResignationById(Long id) {
        Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");
        return toDTO(resignation);
    }

    /**
     * 根据员工ID查询离职申请
     */
    public List<ResignationDTO> getResignationsByEmployeeId(Long employeeId) {
        List<Resignation> resignations = resignationRepository.findByEmployeeId(employeeId);
        return resignations.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建离职申请
     */
    @Transactional
    public ResignationDTO createResignation(CreateResignationCommand command) {
        // 验证员工存在
        Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

        // 检查是否已有待审批的离职申请
        List<Resignation> pendingApplications = resignationRepository.findByEmployeeId(command.getEmployeeId())
                .stream()
                .filter(r -> "PENDING".equals(r.getStatus()) || "APPROVED".equals(r.getStatus()))
                .collect(Collectors.toList());
        if (!pendingApplications.isEmpty()) {
            throw new BusinessException("该员工已有待处理或已审批的离职申请");
        }

        // 生成申请编号
        String applicationNo = generateApplicationNo();

        // 创建离职申请
        Resignation resignation = Resignation.builder()
                .employeeId(command.getEmployeeId())
                .applicationNo(applicationNo)
                .resignationType(command.getResignationType())
                .resignationDate(command.getResignationDate())
                .lastWorkDate(command.getLastWorkDate())
                .reason(command.getReason())
                .handoverPersonId(command.getHandoverPersonId())
                .handoverStatus("PENDING")
                .handoverNote(command.getHandoverNote())
                .status("PENDING")
                .build();

        resignationRepository.save(resignation);

        // 创建审批记录
        Long approverId = approverService.findDefaultApprover();
        approvalService.createApproval(
                "RESIGNATION",
                resignation.getId(),
                applicationNo,
                "离职申请：" + (employee.getEmployeeNo() != null ? employee.getEmployeeNo() : ""),
                approverId,
                "MEDIUM",
                "NORMAL",
                null
        );

        log.info("创建离职申请成功: {} ({})", applicationNo, employee.getEmployeeNo());
        return toDTO(resignation);
    }

    /**
     * 审批离职申请
     */
    @Transactional
    public ResignationDTO approveResignation(Long id, ApproveResignationCommand command) {
        Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");

        if (!"PENDING".equals(resignation.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        Long approverId = SecurityUtils.getUserId();
        resignation.setApproverId(approverId);
        resignation.setApprovedDate(LocalDate.now());
        resignation.setComment(command.getComment());
        resignation.setStatus(command.getApproved() ? "APPROVED" : "REJECTED");

        resignationRepository.updateById(resignation);

        // 如果审批通过，更新员工档案
        if (command.getApproved()) {
            Employee employee = employeeRepository.getByIdOrThrow(resignation.getEmployeeId(), "员工不存在");
            employee.setResignationDate(resignation.getLastWorkDate());
            employee.setResignationReason(resignation.getReason());
            employee.setWorkStatus("RESIGNED");
            employeeRepository.updateById(employee);
            log.info("离职申请审批通过，已更新员工档案: {}", employee.getEmployeeNo());
        }

        log.info("审批离职申请: {} ({})", id, command.getApproved() ? "通过" : "拒绝");
        return toDTO(resignation);
    }

    /**
     * 完成交接
     */
    @Transactional
    public ResignationDTO completeHandover(Long id, String handoverNote) {
        Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");

        if (!"APPROVED".equals(resignation.getStatus())) {
            throw new BusinessException("只有已审批通过的申请才能完成交接");
        }

        resignation.setHandoverStatus("COMPLETED");
        if (handoverNote != null) {
            resignation.setHandoverNote(handoverNote);
        }
        resignation.setStatus("COMPLETED");

        resignationRepository.updateById(resignation);
        log.info("完成离职交接: {}", id);
        return toDTO(resignation);
    }

    /**
     * 删除离职申请（软删除）
     */
    @Transactional
    public void deleteResignation(Long id) {
        Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");
        if (!"PENDING".equals(resignation.getStatus())) {
            throw new BusinessException("只有待审批状态的申请可以删除");
        }
        resignationRepository.softDelete(id);
        log.info("删除离职申请: {}", id);
    }

    /**
     * 转换为DTO
     */
    private ResignationDTO toDTO(Resignation resignation) {
        ResignationDTO dto = new ResignationDTO();
        dto.setId(resignation.getId());
        dto.setEmployeeId(resignation.getEmployeeId());
        dto.setApplicationNo(resignation.getApplicationNo());
        dto.setResignationType(resignation.getResignationType());
        dto.setResignationDate(resignation.getResignationDate());
        dto.setLastWorkDate(resignation.getLastWorkDate());
        dto.setReason(resignation.getReason());
        dto.setHandoverPersonId(resignation.getHandoverPersonId());
        dto.setHandoverStatus(resignation.getHandoverStatus());
        dto.setHandoverNote(resignation.getHandoverNote());
        dto.setStatus(resignation.getStatus());
        dto.setApproverId(resignation.getApproverId());
        dto.setApprovedDate(resignation.getApprovedDate());
        dto.setComment(resignation.getComment());
        dto.setCreatedAt(resignation.getCreatedAt());
        dto.setUpdatedAt(resignation.getUpdatedAt());

        // 加载员工和用户信息
        if (resignation.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(resignation.getEmployeeId());
            if (employee != null && employee.getUserId() != null) {
                dto.setUserId(employee.getUserId());
                User user = userRepository.findById(employee.getUserId());
                if (user != null) {
                    dto.setEmployeeName(user.getRealName());
                }
            }
        }

        // 加载交接人信息
        if (resignation.getHandoverPersonId() != null) {
            User handoverPerson = userRepository.findById(resignation.getHandoverPersonId());
            if (handoverPerson != null) {
                dto.setHandoverPersonName(handoverPerson.getRealName());
            }
        }

        // 加载审批人信息
        if (resignation.getApproverId() != null) {
            User approver = userRepository.findById(resignation.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }

        // 设置离职类型名称
        if (dto.getResignationType() != null) {
            switch (dto.getResignationType()) {
                case "VOLUNTARY" -> dto.setResignationTypeName("主动离职");
                case "DISMISSED" -> dto.setResignationTypeName("辞退");
                case "RETIREMENT" -> dto.setResignationTypeName("退休");
                case "CONTRACT_EXPIRED" -> dto.setResignationTypeName("合同到期");
                default -> dto.setResignationTypeName(dto.getResignationType());
            }
        }

        // 设置交接状态名称
        if (dto.getHandoverStatus() != null) {
            switch (dto.getHandoverStatus()) {
                case "PENDING" -> dto.setHandoverStatusName("待交接");
                case "IN_PROGRESS" -> dto.setHandoverStatusName("交接中");
                case "COMPLETED" -> dto.setHandoverStatusName("已完成");
                default -> dto.setHandoverStatusName(dto.getHandoverStatus());
            }
        }

        // 设置状态名称
        if (dto.getStatus() != null) {
            switch (dto.getStatus()) {
                case "PENDING" -> dto.setStatusName("待审批");
                case "APPROVED" -> dto.setStatusName("已通过");
                case "REJECTED" -> dto.setStatusName("已拒绝");
                case "COMPLETED" -> dto.setStatusName("已完成");
                default -> dto.setStatusName(dto.getStatus());
            }
        }

        return dto;
    }

    /**
     * 生成申请编号
     */
    private String generateApplicationNo() {
        String prefix = "RES";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + timestamp;
    }
}

