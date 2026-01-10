package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.AllocateCostCommand;
import com.lawfirm.application.finance.command.ApproveExpenseCommand;
import com.lawfirm.application.finance.command.CreateExpenseCommand;
import com.lawfirm.application.finance.dto.CostAllocationDTO;
import com.lawfirm.application.finance.dto.ExpenseDTO;
import com.lawfirm.application.finance.dto.ExpenseQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.constant.ExpenseStatus;
import com.lawfirm.domain.finance.entity.CostAllocation;
import com.lawfirm.domain.finance.entity.Expense;
import com.lawfirm.domain.finance.entity.CostSplit;
import com.lawfirm.domain.finance.repository.CostAllocationRepository;
import com.lawfirm.domain.finance.repository.CostSplitRepository;
import com.lawfirm.domain.finance.repository.ExpenseRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.finance.command.SplitCostCommand;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.CostAllocationMapper;
import com.lawfirm.infrastructure.persistence.mapper.ExpenseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 费用报销应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAppService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final CostAllocationRepository costAllocationRepository;
    private final CostAllocationMapper costAllocationMapper;
    private final CostSplitRepository costSplitRepository;
    private final MatterRepository matterRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;
    private final ObjectMapper objectMapper;
    private MatterAppService matterAppService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    public void setMatterAppService(MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 分页查询费用报销列表
     */
    public PageResult<ExpenseDTO> listExpenses(ExpenseQueryDTO query) {
        // 根据用户权限过滤数据
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        
        // 获取可访问的项目ID列表
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // 如果返回空列表，表示没有权限，返回空结果
        if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
        }
        
        // 如果query中指定了matterId，需要验证是否有权限访问该项目
        if (query.getMatterId() != null && accessibleMatterIds != null) {
            if (!accessibleMatterIds.contains(query.getMatterId())) {
                // 没有权限访问指定的项目，返回空结果
                return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
            }
        }
        
        // SELF权限时，只能查看自己申请的费用
        if ("SELF".equals(dataScope) && query.getApplicantId() != null && !query.getApplicantId().equals(currentUserId)) {
            // 查询指定了其他申请人，但当前用户只有SELF权限，返回空结果
            return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
        }
        
        // 如果SELF权限且未指定申请人，自动过滤为当前用户
        Long applicantId = query.getApplicantId();
        if ("SELF".equals(dataScope) && applicantId == null) {
            applicantId = currentUserId;
        }
        
        List<Expense> expenses = expenseMapper.selectExpensePage(
                query.getExpenseNo(),
                query.getMatterId(),
                applicantId,
                query.getStatus(),
                query.getExpenseType(),
                query.getExpenseCategory(),
                accessibleMatterIds  // null表示可以访问所有项目的费用（ALL权限）
        );

        // 手动分页
        int offset = query.getOffset();
        int limit = query.getPageSize();
        int total = expenses.size();
        List<Expense> pagedExpenses = expenses.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        List<ExpenseDTO> records = pagedExpenses.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, total, query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取费用报销详情
     */
    public ExpenseDTO getExpense(Long id) {
        Expense expense = expenseRepository.findById(id);
        if (expense == null) {
            throw new BusinessException("费用报销记录不存在");
        }
        return toDTO(expense);
    }

    /**
     * 创建费用报销申请
     */
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDTO createExpense(CreateExpenseCommand command) {
        // 验证项目是否存在（如果提供了项目ID）
        if (command.getMatterId() != null) {
            if (matterRepository.findById(command.getMatterId()) == null) {
                throw new BusinessException("项目不存在");
            }
        }

        // 生成报销单号
        String expenseNo = generateExpenseNo();

        Expense expense = Expense.builder()
                .expenseNo(expenseNo)
                .matterId(command.getMatterId())
                .applicantId(SecurityUtils.getUserId())
                .expenseType(command.getExpenseType())
                .expenseCategory(command.getExpenseCategory())
                .expenseDate(command.getExpenseDate())
                .amount(command.getAmount())
                .currency(command.getCurrency() != null ? command.getCurrency() : "CNY")
                .description(command.getDescription())
                .vendorName(command.getVendorName())
                .invoiceNo(command.getInvoiceNo())
                .invoiceUrl(command.getInvoiceUrl())
                .status(ExpenseStatus.PENDING)
                .isCostAllocation(false)
                .remark(command.getRemark())
                .createdBy(SecurityUtils.getUserId())
                .createdAt(LocalDateTime.now())
                .build();

        expenseRepository.getBaseMapper().insert(expense);

        // 创建审批记录
        try {
            Long approverId = approverService.findDefaultApprover();
            String businessSnapshot = objectMapper.writeValueAsString(expense);
            approvalService.createApproval(
                    "EXPENSE",
                    expense.getId(),
                    expenseNo,
                    "费用报销申请：" + command.getDescription(),
                    approverId,
                    "NORMAL",
                    "NORMAL",
                    businessSnapshot
            );
        } catch (Exception e) {
            log.error("创建审批记录失败", e);
            // 不阻断主流程，仅记录日志
        }

        log.info("创建费用报销申请: expenseNo={}, amount={}", expenseNo, command.getAmount());

        return toDTO(expense);
    }

    /**
     * 审批费用报销
     */
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDTO approveExpense(ApproveExpenseCommand command) {
        // 权限检查：只有管理层和财务可以审批
        checkApprovalPermission();
        
        Expense expense = expenseRepository.findById(command.getExpenseId());
        if (expense == null) {
            throw new BusinessException("费用报销记录不存在");
        }

        if (!ExpenseStatus.canApprove(expense.getStatus())) {
            throw new BusinessException("只能审批待审批状态的报销单");
        }

        if ("APPROVE".equals(command.getAction())) {
            expense.setStatus(ExpenseStatus.APPROVED);
        } else if ("REJECT".equals(command.getAction())) {
            expense.setStatus(ExpenseStatus.REJECTED);
        } else {
            throw new BusinessException("无效的审批操作");
        }

        expense.setApproverId(SecurityUtils.getUserId());
        expense.setApprovedAt(LocalDateTime.now());
        expense.setApprovalComment(command.getComment());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setUpdatedBy(SecurityUtils.getUserId());

        expenseRepository.getBaseMapper().updateById(expense);

        log.info("审批费用报销: expenseId={}, action={}", command.getExpenseId(), command.getAction());

        return toDTO(expense);
    }

    /**
     * 确认支付
     * 使用乐观锁防止并发重复支付
     */
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDTO confirmPayment(Long id, String paymentMethod) {
        // 权限检查：只有财务可以确认支付
        checkFinancePermission();
        
        Expense expense = expenseRepository.findById(id);
        if (expense == null) {
            throw new BusinessException("费用报销记录不存在");
        }

        // 检查是否已支付，防止重复支付
        if (ExpenseStatus.PAID.equals(expense.getStatus())) {
            throw new BusinessException("该报销单已支付，请勿重复操作");
        }

        if (!ExpenseStatus.canPay(expense.getStatus())) {
            throw new BusinessException("只能支付已审批的报销单");
        }

        // 记录支付前状态（审计）
        String previousStatus = expense.getStatus();

        expense.setStatus(ExpenseStatus.PAID);
        expense.setPaidAt(LocalDateTime.now());
        expense.setPaidBy(SecurityUtils.getUserId());
        expense.setPaymentMethod(paymentMethod);
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setUpdatedBy(SecurityUtils.getUserId());

        // 使用乐观锁更新，如果版本不匹配会抛出异常
        int updatedRows = expenseRepository.getBaseMapper().updateById(expense);
        if (updatedRows == 0) {
            throw new BusinessException("支付失败：数据已被其他用户修改，请刷新后重试");
        }

        log.info("确认费用支付: expenseId={}, paymentMethod={}, previousStatus={}", 
                id, paymentMethod, previousStatus);

        return toDTO(expense);
    }

    /**
     * 成本归集
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocateCost(AllocateCostCommand command) {
        // 验证项目是否存在
        if (matterRepository.findById(command.getMatterId()) == null) {
            throw new BusinessException("项目不存在");
        }

        for (Long expenseId : command.getExpenseIds()) {
            Expense expense = expenseRepository.findById(expenseId);
            if (expense == null) {
                throw new BusinessException("费用报销记录不存在: " + expenseId);
            }

            if (!ExpenseStatus.PAID.equals(expense.getStatus())) {
                throw new BusinessException("只能归集已支付的费用: " + expense.getExpenseNo());
            }

            // 创建成本归集记录
            CostAllocation allocation = CostAllocation.builder()
                    .matterId(command.getMatterId())
                    .expenseId(expenseId)
                    .allocatedAmount(expense.getAmount())
                    .allocationDate(LocalDate.now())
                    .allocatedBy(SecurityUtils.getUserId())
                    .createdAt(LocalDateTime.now())
                    .createdBy(SecurityUtils.getUserId())
                    .build();

            costAllocationRepository.getBaseMapper().insert(allocation);

            // 更新费用记录
            expense.setIsCostAllocation(true);
            expense.setAllocatedToMatterId(command.getMatterId());
            expense.setUpdatedAt(LocalDateTime.now());
            expense.setUpdatedBy(SecurityUtils.getUserId());
            expenseRepository.getBaseMapper().updateById(expense);
        }

        log.info("成本归集完成: matterId={}, expenseCount={}", 
                command.getMatterId(), command.getExpenseIds().size());
    }

    /**
     * 查询项目的成本归集记录
     */
    public List<CostAllocationDTO> listCostAllocations(Long matterId) {
        List<CostAllocation> allocations = costAllocationMapper.selectByMatterId(matterId);
        return allocations.stream()
                .map(this::toCostAllocationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取项目总成本（包括归集成本和分摊成本）
     */
    public BigDecimal getTotalCost(Long matterId) {
        BigDecimal allocatedCost = expenseMapper.selectTotalCostByMatterId(matterId);
        BigDecimal splitCost = costSplitRepository.getBaseMapper()
                .selectTotalSplitCostByMatterId(matterId);
        if (splitCost == null) {
            splitCost = BigDecimal.ZERO;
        }
        return allocatedCost.add(splitCost);
    }

    /**
     * 成本分摊（M4-043）：将公共成本分摊到多个项目
     */
    @Transactional(rollbackFor = Exception.class)
    public void splitCost(SplitCostCommand command) {
        // 验证费用存在且是公共费用（matterId为空）
        Expense expense = expenseRepository.findById(command.getExpenseId());
        if (expense == null) {
            throw new BusinessException("费用报销记录不存在");
        }

        if (expense.getMatterId() != null) {
            throw new BusinessException("该费用已关联项目，不能进行分摊");
        }

        if (!ExpenseStatus.PAID.equals(expense.getStatus())) {
            throw new BusinessException("只能分摊已支付的费用");
        }

        // 验证所有项目存在
        for (Long matterId : command.getMatterIds()) {
            if (matterRepository.findById(matterId) == null) {
                throw new BusinessException("项目不存在: " + matterId);
            }
        }

        BigDecimal totalAmount = expense.getAmount();
        List<CostSplit> splits = new java.util.ArrayList<>();

        // 根据分摊方式计算分摊金额
        if ("EQUAL".equals(command.getSplitMethod())) {
            // 平均分摊 - 修复精度问题：最后一个项目承担差额
            int count = command.getMatterIds().size();
            BigDecimal splitAmount = totalAmount.divide(
                    BigDecimal.valueOf(count),
                    2,
                    java.math.RoundingMode.HALF_UP);
            BigDecimal splitRatio = BigDecimal.ONE
                    .divide(BigDecimal.valueOf(count),
                            4, java.math.RoundingMode.HALF_UP);

            BigDecimal allocatedTotal = BigDecimal.ZERO;

            for (int i = 0; i < count; i++) {
                Long matterId = command.getMatterIds().get(i);
                BigDecimal amount;

                // 最后一个项目承担差额，确保分摊总额精确等于原始总额
                if (i == count - 1) {
                    amount = totalAmount.subtract(allocatedTotal);
                } else {
                    amount = splitAmount;
                    allocatedTotal = allocatedTotal.add(amount);
                }

                CostSplit split = CostSplit.builder()
                        .expenseId(command.getExpenseId())
                        .matterId(matterId)
                        .splitAmount(amount)
                        .splitRatio(splitRatio)
                        .splitMethod("EQUAL")
                        .splitDate(LocalDate.now())
                        .splitBy(SecurityUtils.getUserId())
                        .remark(command.getRemark())
                        .createdAt(LocalDateTime.now())
                        .createdBy(SecurityUtils.getUserId())
                        .build();
                splits.add(split);
            }
        } else if ("RATIO".equals(command.getSplitMethod())) {
            // 按比例分摊 - 修复精度问题：最后一个项目承担差额
            if (command.getRatios() == null || command.getRatios().isEmpty()) {
                throw new BusinessException("按比例分摊需要提供分摊比例");
            }

            BigDecimal totalRatio = command.getRatios().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalRatio.compareTo(BigDecimal.ONE) != 0) {
                throw new BusinessException("分摊比例总和必须等于1（100%）");
            }

            BigDecimal allocatedTotal = BigDecimal.ZERO;
            int count = command.getMatterIds().size();

            for (int i = 0; i < count; i++) {
                Long matterId = command.getMatterIds().get(i);
                BigDecimal ratio = command.getRatios().get(matterId);
                if (ratio == null) {
                    throw new BusinessException("项目 " + matterId + " 缺少分摊比例");
                }

                BigDecimal splitAmount;
                // 最后一个项目承担差额，确保分摊总额精确等于原始总额
                if (i == count - 1) {
                    splitAmount = totalAmount.subtract(allocatedTotal);
                } else {
                    splitAmount = totalAmount.multiply(ratio)
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    allocatedTotal = allocatedTotal.add(splitAmount);
                }

                CostSplit split = CostSplit.builder()
                        .expenseId(command.getExpenseId())
                        .matterId(matterId)
                        .splitAmount(splitAmount)
                        .splitRatio(ratio)
                        .splitMethod("RATIO")
                        .splitDate(LocalDate.now())
                        .splitBy(SecurityUtils.getUserId())
                        .remark(command.getRemark())
                        .createdAt(LocalDateTime.now())
                        .createdBy(SecurityUtils.getUserId())
                        .build();
                splits.add(split);
            }
        } else if ("MANUAL".equals(command.getSplitMethod())) {
            // 手动指定分摊金额
            if (command.getManualAmounts() == null || command.getManualAmounts().isEmpty()) {
                throw new BusinessException("手动分摊需要提供分摊金额");
            }

            BigDecimal totalSplitAmount = command.getManualAmounts().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalSplitAmount.compareTo(totalAmount) != 0) {
                throw new BusinessException("分摊金额总和必须等于费用总额");
            }

            for (Long matterId : command.getMatterIds()) {
                BigDecimal splitAmount = command.getManualAmounts().get(matterId);
                if (splitAmount == null) {
                    throw new BusinessException("项目 " + matterId + " 缺少分摊金额");
                }
                BigDecimal ratio = splitAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP);

                CostSplit split = CostSplit.builder()
                        .expenseId(command.getExpenseId())
                        .matterId(matterId)
                        .splitAmount(splitAmount)
                        .splitRatio(ratio)
                        .splitMethod("MANUAL")
                        .splitDate(LocalDate.now())
                        .splitBy(SecurityUtils.getUserId())
                        .remark(command.getRemark())
                        .createdAt(LocalDateTime.now())
                        .createdBy(SecurityUtils.getUserId())
                        .build();
                splits.add(split);
            }
        } else {
            throw new BusinessException("不支持的分摊方式: " + command.getSplitMethod());
        }

        // 验证分摊总额必须等于原始总额
        BigDecimal splitTotal = splits.stream()
                .map(CostSplit::getSplitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (splitTotal.compareTo(totalAmount) != 0) {
            throw new BusinessException(
                    String.format("分摊金额总和(%s)与原始总额(%s)不一致，请检查分摊数据", 
                            splitTotal, totalAmount));
        }

        // 保存所有分摊记录
        for (CostSplit split : splits) {
            costSplitRepository.getBaseMapper().insert(split);
        }

        log.info("成本分摊完成: expenseId={}, matterCount={}, totalAmount={}, splitTotal={}",
                command.getExpenseId(), command.getMatterIds().size(), totalAmount, splitTotal);
    }

    /**
     * 查询项目的成本分摊记录
     */
    public List<com.lawfirm.application.finance.dto.CostSplitDTO> listCostSplits(Long matterId) {
        List<CostSplit> splits = costSplitRepository.findByMatterId(matterId);
        return splits.stream()
                .map(this::toCostSplitDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除费用报销（仅未审批状态可删除，且只能删除自己的）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id);
        if (expense == null) {
            throw new BusinessException("费用报销记录不存在");
        }
        
        // 权限检查：只有申请人或管理员可以删除
        Long currentUserId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin() && !currentUserId.equals(expense.getApplicantId())) {
            throw new BusinessException("只能删除自己申请的报销单");
        }

        if (!ExpenseStatus.canDelete(expense.getStatus())) {
            throw new BusinessException("只能删除待审批状态的报销单");
        }

        expenseRepository.softDelete(id);
    }

    // ========== 权限检查方法 ==========
    
    /**
     * 检查是否有审批权限（ADMIN/DIRECTOR/TEAM_LEADER/FINANCE）
     */
    private void checkApprovalPermission() {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("DIRECTOR") && !roleCodes.contains("TEAM_LEADER") && !roleCodes.contains("FINANCE")) {
            throw new BusinessException("只有管理层和财务人员可以审批费用报销");
        }
    }
    
    /**
     * 检查是否有财务权限（ADMIN/FINANCE）
     */
    private void checkFinancePermission() {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("FINANCE")) {
            throw new BusinessException("只有财务人员可以确认支付");
        }
    }

    // ========== 工具方法 ==========

    /**
     * 生成报销单号
     */
    private String generateExpenseNo() {
        String prefix = "EXP";
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + date + random;
    }

    /**
     * 转换为DTO
     */
    private ExpenseDTO toDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        BeanUtils.copyProperties(expense, dto);

        // 查询关联信息
        if (expense.getApplicantId() != null) {
            User applicant = userRepository.findById(expense.getApplicantId());
            if (applicant != null) {
                dto.setApplicantName(applicant.getRealName());
            }
        }

        if (expense.getApproverId() != null) {
            User approver = userRepository.findById(expense.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }

        if (expense.getPaidBy() != null) {
            User paidBy = userRepository.findById(expense.getPaidBy());
            if (paidBy != null) {
                dto.setPaidByName(paidBy.getRealName());
            }
        }

        // 查询项目名称
        if (expense.getMatterId() != null) {
            com.lawfirm.domain.matter.entity.Matter matter = matterRepository.findById(expense.getMatterId());
            if (matter != null) {
                dto.setMatterName(matter.getName());
            }
        }
        if (expense.getAllocatedToMatterId() != null) {
            com.lawfirm.domain.matter.entity.Matter matter = matterRepository.findById(expense.getAllocatedToMatterId());
            if (matter != null) {
                dto.setMatterName(matter.getName());
            }
        }

        return dto;
    }

    /**
     * 转换为成本归集DTO
     */
    private CostAllocationDTO toCostAllocationDTO(CostAllocation allocation) {
        CostAllocationDTO dto = new CostAllocationDTO();
        BeanUtils.copyProperties(allocation, dto);

        // 查询费用信息
        Expense expense = expenseRepository.findById(allocation.getExpenseId());
        if (expense != null) {
            dto.setExpenseNo(expense.getExpenseNo());
            dto.setExpenseDescription(expense.getDescription());
            dto.setExpenseType(expense.getExpenseType());
            dto.setExpenseDate(expense.getExpenseDate());
        }

        // 查询操作人
        if (allocation.getAllocatedBy() != null) {
            User user = userRepository.findById(allocation.getAllocatedBy());
            if (user != null) {
                dto.setAllocatedByName(user.getRealName());
            }
        }

        // 查询项目名称
        if (allocation.getMatterId() != null) {
            com.lawfirm.domain.matter.entity.Matter matter = matterRepository.findById(allocation.getMatterId());
            if (matter != null) {
                dto.setMatterName(matter.getName());
            }
        }

        return dto;
    }

    /**
     * 转换为成本分摊DTO
     */
    private com.lawfirm.application.finance.dto.CostSplitDTO toCostSplitDTO(CostSplit split) {
        com.lawfirm.application.finance.dto.CostSplitDTO dto = new com.lawfirm.application.finance.dto.CostSplitDTO();
        BeanUtils.copyProperties(split, dto);

        // 查询费用信息
        Expense expense = expenseRepository.findById(split.getExpenseId());
        if (expense != null) {
            dto.setExpenseNo(expense.getExpenseNo());
            dto.setExpenseDescription(expense.getDescription());
            dto.setExpenseType(expense.getExpenseType());
            dto.setExpenseDate(expense.getExpenseDate());
        }

        // 查询操作人
        if (split.getSplitBy() != null) {
            User user = userRepository.findById(split.getSplitBy());
            if (user != null) {
                dto.setSplitByName(user.getRealName());
            }
        }

        // 查询项目名称
        if (split.getMatterId() != null) {
            com.lawfirm.domain.matter.entity.Matter matter = matterRepository.findById(split.getMatterId());
            if (matter != null) {
                dto.setMatterName(matter.getName());
            }
        }

        return dto;
    }
}

