package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateEmployeeCommand;
import com.lawfirm.application.hr.command.UpdateEmployeeCommand;
import com.lawfirm.application.hr.dto.EmployeeDTO;
import com.lawfirm.application.hr.dto.EmployeeQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 员工档案应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeAppService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final UserRepository userRepository;

    /**
     * 分页查询员工档案
     */
    public PageResult<EmployeeDTO> listEmployees(EmployeeQueryDTO query) {
        IPage<Employee> page = employeeMapper.selectEmployeePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getEmployeeNo(),
                query.getRealName(),
                query.getDepartmentId(),
                query.getWorkStatus(),
                query.getPosition()
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
                page.getTotal(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    /**
     * 根据ID查询员工档案
     */
    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.getByIdOrThrow(id, "员工档案不存在");
        return toDTO(employee);
    }

    /**
     * 根据用户ID查询员工档案
     */
    public EmployeeDTO getEmployeeByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("员工档案不存在"));
        return toDTO(employee);
    }

    /**
     * 创建员工档案
     */
    @Transactional
    public EmployeeDTO createEmployee(CreateEmployeeCommand command) {
        // 验证用户存在
        User user = userRepository.getByIdOrThrow(command.getUserId(), "用户不存在");

        // 检查是否已有档案
        if (employeeRepository.findByUserId(command.getUserId()).isPresent()) {
            throw new BusinessException("该用户已有员工档案");
        }

        // 生成工号
        String employeeNo = command.getEmployeeNo();
        if (employeeNo == null || employeeNo.isEmpty()) {
            employeeNo = generateEmployeeNo();
        } else {
            // 检查工号是否已存在
            if (employeeRepository.findByEmployeeNo(employeeNo).isPresent()) {
                throw new BusinessException("工号已存在");
            }
        }

        // 创建员工档案
        Employee employee = Employee.builder()
                .userId(command.getUserId())
                .employeeNo(employeeNo)
                .gender(command.getGender())
                .birthDate(command.getBirthDate())
                .idCard(command.getIdCard())
                .nationality(command.getNationality() != null ? command.getNationality() : "中国")
                .nativePlace(command.getNativePlace())
                .politicalStatus(command.getPoliticalStatus())
                .education(command.getEducation())
                .major(command.getMajor())
                .graduationSchool(command.getGraduationSchool())
                .graduationDate(command.getGraduationDate())
                .emergencyContact(command.getEmergencyContact())
                .emergencyPhone(command.getEmergencyPhone())
                .address(command.getAddress())
                .lawyerLicenseNo(command.getLawyerLicenseNo())
                .licenseIssueDate(command.getLicenseIssueDate())
                .licenseExpireDate(command.getLicenseExpireDate())
                .licenseStatus(command.getLicenseStatus())
                .practiceArea(command.getPracticeArea())
                .practiceYears(command.getPracticeYears())
                .position(command.getPosition())
                .level(command.getLevel())
                .entryDate(command.getEntryDate() != null ? command.getEntryDate() : LocalDate.now())
                .probationEndDate(command.getProbationEndDate())
                .workStatus(command.getWorkStatus() != null ? command.getWorkStatus() : "ACTIVE")
                .remark(command.getRemark())
                .build();

        employeeRepository.save(employee);
        log.info("创建员工档案成功: {} ({})", user.getRealName(), employeeNo);
        return toDTO(employee);
    }

    /**
     * 更新员工档案
     */
    @Transactional
    public EmployeeDTO updateEmployee(Long id, UpdateEmployeeCommand command) {
        Employee employee = employeeRepository.getByIdOrThrow(id, "员工档案不存在");

        // 如果更新工号，检查是否重复
        if (command.getEmployeeNo() != null && !command.getEmployeeNo().equals(employee.getEmployeeNo())) {
            if (employeeRepository.findByEmployeeNo(command.getEmployeeNo()).isPresent()) {
                throw new BusinessException("工号已存在");
            }
            employee.setEmployeeNo(command.getEmployeeNo());
        }

        // 更新字段
        if (command.getGender() != null) employee.setGender(command.getGender());
        if (command.getBirthDate() != null) employee.setBirthDate(command.getBirthDate());
        if (command.getIdCard() != null) employee.setIdCard(command.getIdCard());
        if (command.getNationality() != null) employee.setNationality(command.getNationality());
        if (command.getNativePlace() != null) employee.setNativePlace(command.getNativePlace());
        if (command.getPoliticalStatus() != null) employee.setPoliticalStatus(command.getPoliticalStatus());
        if (command.getEducation() != null) employee.setEducation(command.getEducation());
        if (command.getMajor() != null) employee.setMajor(command.getMajor());
        if (command.getGraduationSchool() != null) employee.setGraduationSchool(command.getGraduationSchool());
        if (command.getGraduationDate() != null) employee.setGraduationDate(command.getGraduationDate());
        if (command.getEmergencyContact() != null) employee.setEmergencyContact(command.getEmergencyContact());
        if (command.getEmergencyPhone() != null) employee.setEmergencyPhone(command.getEmergencyPhone());
        if (command.getAddress() != null) employee.setAddress(command.getAddress());
        if (command.getLawyerLicenseNo() != null) employee.setLawyerLicenseNo(command.getLawyerLicenseNo());
        if (command.getLicenseIssueDate() != null) employee.setLicenseIssueDate(command.getLicenseIssueDate());
        if (command.getLicenseExpireDate() != null) employee.setLicenseExpireDate(command.getLicenseExpireDate());
        if (command.getLicenseStatus() != null) employee.setLicenseStatus(command.getLicenseStatus());
        if (command.getPracticeArea() != null) employee.setPracticeArea(command.getPracticeArea());
        if (command.getPracticeYears() != null) employee.setPracticeYears(command.getPracticeYears());
        if (command.getPosition() != null) employee.setPosition(command.getPosition());
        if (command.getLevel() != null) employee.setLevel(command.getLevel());
        if (command.getEntryDate() != null) employee.setEntryDate(command.getEntryDate());
        if (command.getProbationEndDate() != null) employee.setProbationEndDate(command.getProbationEndDate());
        if (command.getRegularDate() != null) employee.setRegularDate(command.getRegularDate());
        if (command.getWorkStatus() != null) employee.setWorkStatus(command.getWorkStatus());
        if (command.getRemark() != null) employee.setRemark(command.getRemark());

        employeeRepository.updateById(employee);
        log.info("更新员工档案成功: {}", id);
        return toDTO(employee);
    }

    /**
     * 删除员工档案（软删除）
     */
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.getByIdOrThrow(id, "员工档案不存在");
        employeeRepository.softDelete(id);
        log.info("删除员工档案: {}", id);
    }

    /**
     * 转换为DTO
     */
    private EmployeeDTO toDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setUserId(employee.getUserId());
        dto.setEmployeeNo(employee.getEmployeeNo());
        dto.setGender(employee.getGender());
        dto.setBirthDate(employee.getBirthDate());
        dto.setIdCard(employee.getIdCard());
        dto.setNationality(employee.getNationality());
        dto.setNativePlace(employee.getNativePlace());
        dto.setPoliticalStatus(employee.getPoliticalStatus());
        dto.setEducation(employee.getEducation());
        dto.setMajor(employee.getMajor());
        dto.setGraduationSchool(employee.getGraduationSchool());
        dto.setGraduationDate(employee.getGraduationDate());
        dto.setEmergencyContact(employee.getEmergencyContact());
        dto.setEmergencyPhone(employee.getEmergencyPhone());
        dto.setAddress(employee.getAddress());
        dto.setLawyerLicenseNo(employee.getLawyerLicenseNo());
        dto.setLicenseIssueDate(employee.getLicenseIssueDate());
        dto.setLicenseExpireDate(employee.getLicenseExpireDate());
        dto.setLicenseStatus(employee.getLicenseStatus());
        dto.setPracticeArea(employee.getPracticeArea());
        dto.setPracticeYears(employee.getPracticeYears());
        dto.setPosition(employee.getPosition());
        dto.setLevel(employee.getLevel());
        dto.setEntryDate(employee.getEntryDate());
        dto.setProbationEndDate(employee.getProbationEndDate());
        dto.setRegularDate(employee.getRegularDate());
        dto.setResignationDate(employee.getResignationDate());
        dto.setResignationReason(employee.getResignationReason());
        dto.setWorkStatus(employee.getWorkStatus());
        dto.setRemark(employee.getRemark());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());

        // 加载用户信息
        if (employee.getUserId() != null) {
            User user = userRepository.findById(employee.getUserId());
            if (user != null) {
                dto.setRealName(user.getRealName());
                dto.setEmail(user.getEmail());
                dto.setPhone(user.getPhone());
                dto.setDepartmentId(user.getDepartmentId());
            }
        }

        // 设置状态名称
        if (dto.getWorkStatus() != null) {
            switch (dto.getWorkStatus()) {
                case "ACTIVE" -> dto.setWorkStatusName("在职");
                case "PROBATION" -> dto.setWorkStatusName("试用");
                case "RESIGNED" -> dto.setWorkStatusName("离职");
                case "RETIRED" -> dto.setWorkStatusName("退休");
                default -> dto.setWorkStatusName(dto.getWorkStatus());
            }
        }

        return dto;
    }

    /**
     * 生成工号
     */
    private String generateEmployeeNo() {
        String prefix = "EMP";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + timestamp;
    }
}

