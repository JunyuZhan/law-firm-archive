package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateContractCommand;
import com.lawfirm.application.hr.command.UpdateContractCommand;
import com.lawfirm.application.hr.dto.ContractDTO;
import com.lawfirm.application.hr.dto.ContractQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.hr.entity.Contract;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.repository.ContractRepository;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 劳动合同应用服务
 */
@Slf4j
@Service("laborContractAppService")
@RequiredArgsConstructor
public class ContractAppService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    /**
     * 分页查询劳动合同
     */
    public PageResult<ContractDTO> listContracts(ContractQueryDTO query) {
        IPage<Contract> page = contractMapper.selectContractPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getEmployeeId(),
                query.getContractNo(),
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
     * 根据ID查询劳动合同
     */
    public ContractDTO getContractById(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");
        return toDTO(contract);
    }

    /**
     * 根据员工ID查询所有合同
     */
    public List<ContractDTO> getContractsByEmployeeId(Long employeeId) {
        List<Contract> contracts = contractRepository.findByEmployeeId(employeeId);
        return contracts.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建劳动合同
     */
    @Transactional
    public ContractDTO createContract(CreateContractCommand command) {
        // 验证员工存在
        Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

        // 生成合同编号
        String contractNo = command.getContractNo();
        if (contractNo == null || contractNo.isEmpty()) {
            contractNo = generateContractNo();
        } else {
            // 检查合同编号是否已存在
            if (contractRepository.findByContractNo(contractNo).isPresent()) {
                throw new BusinessException("合同编号已存在");
            }
        }

        // 计算试用期结束日期
        LocalDate probationEndDate = command.getProbationEndDate();
        if (probationEndDate == null && command.getProbationMonths() != null && command.getProbationMonths() > 0) {
            probationEndDate = command.getStartDate().plusMonths(command.getProbationMonths());
        }

        // 计算到期日期（如果是固定期限合同）
        LocalDate expireDate = command.getEndDate();
        if (expireDate == null && "FIXED".equals(command.getContractType()) && command.getStartDate() != null) {
            // 默认3年
            expireDate = command.getStartDate().plusYears(3);
        }

        // 创建合同
        Contract contract = Contract.builder()
                .employeeId(command.getEmployeeId())
                .contractNo(contractNo)
                .contractType(command.getContractType())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .probationMonths(command.getProbationMonths() != null ? command.getProbationMonths() : 0)
                .probationEndDate(probationEndDate)
                .baseSalary(command.getBaseSalary())
                .performanceBonus(command.getPerformanceBonus())
                .otherAllowance(command.getOtherAllowance())
                .status("ACTIVE")
                .signDate(command.getSignDate() != null ? command.getSignDate() : LocalDate.now())
                .expireDate(expireDate)
                .renewCount(0)
                .contractFileUrl(command.getContractFileUrl())
                .remark(command.getRemark())
                .build();

        contractRepository.save(contract);
        log.info("创建劳动合同成功: {} ({})", contractNo, employee.getEmployeeNo());
        return toDTO(contract);
    }

    /**
     * 更新劳动合同
     */
    @Transactional
    public ContractDTO updateContract(Long id, UpdateContractCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");

        // 如果更新合同编号，检查是否重复
        if (command.getContractNo() != null && !command.getContractNo().equals(contract.getContractNo())) {
            if (contractRepository.findByContractNo(command.getContractNo()).isPresent()) {
                throw new BusinessException("合同编号已存在");
            }
            contract.setContractNo(command.getContractNo());
        }

        // 更新字段
        if (command.getContractType() != null) contract.setContractType(command.getContractType());
        if (command.getStartDate() != null) contract.setStartDate(command.getStartDate());
        if (command.getEndDate() != null) contract.setEndDate(command.getEndDate());
        if (command.getProbationMonths() != null) contract.setProbationMonths(command.getProbationMonths());
        if (command.getProbationEndDate() != null) contract.setProbationEndDate(command.getProbationEndDate());
        if (command.getBaseSalary() != null) contract.setBaseSalary(command.getBaseSalary());
        if (command.getPerformanceBonus() != null) contract.setPerformanceBonus(command.getPerformanceBonus());
        if (command.getOtherAllowance() != null) contract.setOtherAllowance(command.getOtherAllowance());
        if (command.getStatus() != null) contract.setStatus(command.getStatus());
        if (command.getSignDate() != null) contract.setSignDate(command.getSignDate());
        if (command.getExpireDate() != null) contract.setExpireDate(command.getExpireDate());
        if (command.getContractFileUrl() != null) contract.setContractFileUrl(command.getContractFileUrl());
        if (command.getRemark() != null) contract.setRemark(command.getRemark());

        contractRepository.updateById(contract);
        log.info("更新劳动合同成功: {}", id);
        return toDTO(contract);
    }

    /**
     * 删除劳动合同（软删除）
     */
    @Transactional
    public void deleteContract(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");
        contractRepository.softDelete(id);
        log.info("删除劳动合同: {}", id);
    }

    /**
     * 续签合同
     */
    @Transactional
    public ContractDTO renewContract(Long id, LocalDate newStartDate, LocalDate newEndDate) {
        Contract oldContract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");
        
        if (!"ACTIVE".equals(oldContract.getStatus())) {
            throw new BusinessException("只有生效中的合同可以续签");
        }

        // 将旧合同状态改为已到期
        oldContract.setStatus("EXPIRED");
        oldContract.setExpireDate(newStartDate.minusDays(1));
        contractRepository.updateById(oldContract);

        // 创建新合同
        Contract newContract = Contract.builder()
                .employeeId(oldContract.getEmployeeId())
                .contractNo(generateContractNo())
                .contractType(oldContract.getContractType())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .probationMonths(0) // 续签不再有试用期
                .baseSalary(oldContract.getBaseSalary())
                .performanceBonus(oldContract.getPerformanceBonus())
                .otherAllowance(oldContract.getOtherAllowance())
                .status("ACTIVE")
                .signDate(LocalDate.now())
                .expireDate(newEndDate)
                .renewCount(oldContract.getRenewCount() + 1)
                .remark("续签自合同：" + oldContract.getContractNo())
                .build();

        contractRepository.save(newContract);
        log.info("续签劳动合同成功: {} -> {}", oldContract.getContractNo(), newContract.getContractNo());
        return toDTO(newContract);
    }

    /**
     * 转换为DTO
     */
    private ContractDTO toDTO(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setEmployeeId(contract.getEmployeeId());
        dto.setContractNo(contract.getContractNo());
        dto.setContractType(contract.getContractType());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setProbationMonths(contract.getProbationMonths());
        dto.setProbationEndDate(contract.getProbationEndDate());
        dto.setBaseSalary(contract.getBaseSalary());
        dto.setPerformanceBonus(contract.getPerformanceBonus());
        dto.setOtherAllowance(contract.getOtherAllowance());
        dto.setStatus(contract.getStatus());
        dto.setSignDate(contract.getSignDate());
        dto.setExpireDate(contract.getExpireDate());
        dto.setRenewCount(contract.getRenewCount());
        dto.setContractFileUrl(contract.getContractFileUrl());
        dto.setRemark(contract.getRemark());
        dto.setCreatedAt(contract.getCreatedAt());
        dto.setUpdatedAt(contract.getUpdatedAt());

        // 加载员工和用户信息
        if (contract.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(contract.getEmployeeId());
            if (employee != null && employee.getUserId() != null) {
                dto.setUserId(employee.getUserId());
                User user = userRepository.findById(employee.getUserId());
                if (user != null) {
                    dto.setEmployeeName(user.getRealName());
                }
            }
        }

        // 设置类型名称
        if (dto.getContractType() != null) {
            switch (dto.getContractType()) {
                case "FIXED" -> dto.setContractTypeName("固定期限");
                case "UNFIXED" -> dto.setContractTypeName("无固定期限");
                case "PROJECT" -> dto.setContractTypeName("项目合同");
                case "INTERN" -> dto.setContractTypeName("实习");
                default -> dto.setContractTypeName(dto.getContractType());
            }
        }

        // 设置状态名称
        if (dto.getStatus() != null) {
            switch (dto.getStatus()) {
                case "ACTIVE" -> dto.setStatusName("生效中");
                case "EXPIRED" -> dto.setStatusName("已到期");
                case "TERMINATED" -> dto.setStatusName("已终止");
                default -> dto.setStatusName(dto.getStatus());
            }
        }

        return dto;
    }

    /**
     * 生成合同编号
     */
    private String generateContractNo() {
        String prefix = "CONTRACT";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + timestamp;
    }
}

