package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.dto.TimesheetDTO;
import com.lawfirm.application.matter.dto.TimesheetQueryDTO;
import com.lawfirm.application.matter.dto.TimesheetSummaryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.HourlyRate;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.domain.matter.repository.HourlyRateRepository;
import com.lawfirm.domain.matter.repository.TimesheetRepository;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工时应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetAppService {

    private final TimesheetRepository timesheetRepository;
    private final HourlyRateRepository hourlyRateRepository;
    private final TimesheetMapper timesheetMapper;

    /**
     * 分页查询工时
     */
    public PageResult<TimesheetDTO> listTimesheets(TimesheetQueryDTO query) {
        IPage<Timesheet> page = timesheetMapper.selectTimesheetPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getMatterId(),
                query.getUserId(),
                query.getWorkType(),
                query.getStatus(),
                query.getStartDate(),
                query.getEndDate(),
                query.getBillable()
        );

        List<TimesheetDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建工时记录
     */
    @Transactional
    public TimesheetDTO createTimesheet(CreateTimesheetCommand command) {
        Long userId = SecurityUtils.getUserId();
        String timesheetNo = generateTimesheetNo();

        // 获取小时费率
        BigDecimal hourlyRate = command.getHourlyRate();
        if (hourlyRate == null) {
            HourlyRate rate = hourlyRateRepository.findCurrentRate(userId, command.getWorkDate());
            hourlyRate = rate != null ? rate.getRate() : BigDecimal.ZERO;
        }

        // 计算金额
        boolean billable = command.getBillable() != null ? command.getBillable() : true;
        BigDecimal amount = billable ? command.getHours().multiply(hourlyRate) : BigDecimal.ZERO;

        Timesheet timesheet = Timesheet.builder()
                .timesheetNo(timesheetNo)
                .matterId(command.getMatterId())
                .userId(userId)
                .workDate(command.getWorkDate())
                .hours(command.getHours())
                .workType(command.getWorkType())
                .workContent(command.getWorkContent())
                .billable(billable)
                .hourlyRate(hourlyRate)
                .amount(amount)
                .status("DRAFT")
                .build();

        timesheetRepository.save(timesheet);
        log.info("工时记录创建成功: {} ({}小时)", timesheet.getTimesheetNo(), timesheet.getHours());
        return toDTO(timesheet);
    }

    /**
     * 获取工时详情
     */
    public TimesheetDTO getTimesheetById(Long id) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");
        return toDTO(timesheet);
    }

    /**
     * 更新工时记录
     */
    @Transactional
    public TimesheetDTO updateTimesheet(Long id, LocalDate workDate, BigDecimal hours,
                                        String workType, String workContent, Boolean billable) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

        // 只有草稿状态可以修改
        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessException("只有草稿状态的工时记录可以修改");
        }

        // 验证是否是本人的记录
        if (!timesheet.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能修改自己的工时记录");
        }

        if (workDate != null) timesheet.setWorkDate(workDate);
        if (hours != null) {
            timesheet.setHours(hours);
            // 重新计算金额
            if (timesheet.getBillable() && timesheet.getHourlyRate() != null) {
                timesheet.setAmount(hours.multiply(timesheet.getHourlyRate()));
            }
        }
        if (StringUtils.hasText(workType)) timesheet.setWorkType(workType);
        if (StringUtils.hasText(workContent)) timesheet.setWorkContent(workContent);
        if (billable != null) {
            timesheet.setBillable(billable);
            if (!billable) {
                timesheet.setAmount(BigDecimal.ZERO);
            } else if (timesheet.getHourlyRate() != null) {
                timesheet.setAmount(timesheet.getHours().multiply(timesheet.getHourlyRate()));
            }
        }

        timesheetRepository.updateById(timesheet);
        log.info("工时记录更新成功: {}", timesheet.getTimesheetNo());
        return toDTO(timesheet);
    }

    /**
     * 删除工时记录
     */
    @Transactional
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessException("只有草稿状态的工时记录可以删除");
        }

        if (!timesheet.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能删除自己的工时记录");
        }

        timesheetRepository.removeById(id);
        log.info("工时记录删除成功: {}", timesheet.getTimesheetNo());
    }

    /**
     * 提交工时
     */
    @Transactional
    public TimesheetDTO submitTimesheet(Long id) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new BusinessException("只有草稿状态的工时记录可以提交");
        }

        timesheet.setStatus("SUBMITTED");
        timesheet.setSubmittedAt(LocalDateTime.now());
        timesheetRepository.updateById(timesheet);

        log.info("工时记录已提交: {}", timesheet.getTimesheetNo());
        return toDTO(timesheet);
    }

    /**
     * 批量提交工时
     */
    @Transactional
    public void batchSubmit(List<Long> ids) {
        Long userId = SecurityUtils.getUserId();
        for (Long id : ids) {
            Timesheet timesheet = timesheetRepository.findById(id);
            if (timesheet != null && "DRAFT".equals(timesheet.getStatus())
                    && timesheet.getUserId().equals(userId)) {
                timesheet.setStatus("SUBMITTED");
                timesheet.setSubmittedAt(LocalDateTime.now());
                timesheetRepository.updateById(timesheet);
            }
        }
        log.info("批量提交工时成功，共{}条", ids.size());
    }

    /**
     * 审批通过
     */
    @Transactional
    public TimesheetDTO approveTimesheet(Long id, String comment) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

        if (!"SUBMITTED".equals(timesheet.getStatus())) {
            throw new BusinessException("只能审批已提交的工时记录");
        }

        timesheet.setStatus("APPROVED");
        timesheet.setApprovedBy(SecurityUtils.getUserId());
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setApprovalComment(comment);
        timesheetRepository.updateById(timesheet);

        log.info("工时记录审批通过: {}", timesheet.getTimesheetNo());
        return toDTO(timesheet);
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public TimesheetDTO rejectTimesheet(Long id, String comment) {
        Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

        if (!"SUBMITTED".equals(timesheet.getStatus())) {
            throw new BusinessException("只能审批已提交的工时记录");
        }

        timesheet.setStatus("REJECTED");
        timesheet.setApprovedBy(SecurityUtils.getUserId());
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setApprovalComment(comment);
        timesheetRepository.updateById(timesheet);

        log.info("工时记录审批拒绝: {}", timesheet.getTimesheetNo());
        return toDTO(timesheet);
    }

    /**
     * 获取待审批列表
     */
    public List<TimesheetDTO> getPendingApproval() {
        List<Timesheet> timesheets = timesheetRepository.findPendingApproval();
        return timesheets.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取我的工时（按日期范围）
     */
    public List<TimesheetDTO> getMyTimesheets(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getUserId();
        List<Timesheet> timesheets = timesheetRepository.findByUserAndDateRange(userId, startDate, endDate);
        return timesheets.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取用户月度工时汇总
     */
    public TimesheetSummaryDTO getUserMonthlySummary(Long userId, int year, int month) {
        BigDecimal totalHours = timesheetRepository.sumHoursByUserAndMonth(userId, year, month);

        TimesheetSummaryDTO summary = new TimesheetSummaryDTO();
        summary.setUserId(userId);
        summary.setYear(year);
        summary.setMonth(month);
        summary.setTotalHours(totalHours);
        return summary;
    }

    /**
     * 获取案件工时汇总
     */
    public TimesheetSummaryDTO getMatterSummary(Long matterId) {
        BigDecimal totalHours = timesheetRepository.sumHoursByMatter(matterId);

        TimesheetSummaryDTO summary = new TimesheetSummaryDTO();
        summary.setMatterId(matterId);
        summary.setTotalHours(totalHours);
        return summary;
    }

    /**
     * 生成工时编号
     */
    private String generateTimesheetNo() {
        String prefix = "TS" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取工作类型名称
     */
    private String getWorkTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "RESEARCH" -> "法律研究";
            case "DRAFTING" -> "文书起草";
            case "MEETING" -> "会议";
            case "COURT" -> "出庭";
            case "NEGOTIATION" -> "谈判";
            case "COMMUNICATION" -> "沟通";
            case "TRAVEL" -> "差旅";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "SUBMITTED" -> "已提交";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private TimesheetDTO toDTO(Timesheet timesheet) {
        TimesheetDTO dto = new TimesheetDTO();
        dto.setId(timesheet.getId());
        dto.setTimesheetNo(timesheet.getTimesheetNo());
        dto.setMatterId(timesheet.getMatterId());
        dto.setUserId(timesheet.getUserId());
        dto.setWorkDate(timesheet.getWorkDate());
        dto.setHours(timesheet.getHours());
        dto.setWorkType(timesheet.getWorkType());
        dto.setWorkTypeName(getWorkTypeName(timesheet.getWorkType()));
        dto.setWorkContent(timesheet.getWorkContent());
        dto.setBillable(timesheet.getBillable());
        dto.setHourlyRate(timesheet.getHourlyRate());
        dto.setAmount(timesheet.getAmount());
        dto.setStatus(timesheet.getStatus());
        dto.setStatusName(getStatusName(timesheet.getStatus()));
        dto.setSubmittedAt(timesheet.getSubmittedAt());
        dto.setApprovedBy(timesheet.getApprovedBy());
        dto.setApprovedAt(timesheet.getApprovedAt());
        dto.setApprovalComment(timesheet.getApprovalComment());
        dto.setCreatedAt(timesheet.getCreatedAt());
        dto.setUpdatedAt(timesheet.getUpdatedAt());
        return dto;
    }
}
