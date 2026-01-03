package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateInvoiceCommand;
import com.lawfirm.application.finance.dto.InvoiceDTO;
import com.lawfirm.application.finance.dto.InvoiceStatisticsDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Invoice;
import com.lawfirm.domain.finance.repository.InvoiceRepository;
import com.lawfirm.infrastructure.persistence.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 发票管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceAppService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceMapper invoiceMapper;

    /**
     * 分页查询发票
     */
    public PageResult<InvoiceDTO> listInvoices(PageQuery query, Long clientId, String status) {
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        
        if (clientId != null) {
            wrapper.eq(Invoice::getClientId, clientId);
        }
        if (status != null) {
            wrapper.eq(Invoice::getStatus, status);
        }
        
        wrapper.orderByDesc(Invoice::getCreatedAt);

        IPage<Invoice> page = invoiceRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<InvoiceDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 申请开票
     */
    @Transactional
    public InvoiceDTO applyInvoice(CreateInvoiceCommand command) {
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 计算税额
        BigDecimal taxRate = command.getTaxRate() != null ? command.getTaxRate() : new BigDecimal("0.06");
        BigDecimal taxAmount = command.getAmount().multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .feeId(command.getFeeId())
                .contractId(command.getContractId())
                .clientId(command.getClientId())
                .invoiceType(command.getInvoiceType())
                .title(command.getTitle())
                .taxNo(command.getTaxNo())
                .amount(command.getAmount())
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .content(command.getContent())
                .status("PENDING")
                .applicantId(SecurityUtils.getUserId())
                .remark(command.getRemark())
                .build();

        invoiceRepository.save(invoice);
        log.info("发票申请成功: {}", invoice.getId());
        return toDTO(invoice);
    }

    /**
     * 获取发票详情
     */
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.getByIdOrThrow(id, "发票不存在");
        return toDTO(invoice);
    }

    /**
     * 开票（确认开票）
     */
    @Transactional
    public void issueInvoice(Long id, String invoiceNo) {
        Invoice invoice = invoiceRepository.getByIdOrThrow(id, "发票不存在");
        
        if (!"PENDING".equals(invoice.getStatus())) {
            throw new BusinessException("当前状态不允许开票");
        }

        invoice.setInvoiceNo(invoiceNo);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus("ISSUED");
        invoice.setIssuerId(SecurityUtils.getUserId());
        invoiceRepository.updateById(invoice);
        log.info("发票开具成功: {}", invoiceNo);
    }

    /**
     * 作废发票
     */
    @Transactional
    public void cancelInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.getByIdOrThrow(id, "发票不存在");
        
        if (!"ISSUED".equals(invoice.getStatus())) {
            throw new BusinessException("只有已开票状态可以作废");
        }

        invoice.setStatus("CANCELLED");
        invoice.setRemark(reason);
        invoiceRepository.updateById(invoice);
        log.info("发票作废成功: {}", invoice.getInvoiceNo());
    }

    /**
     * 获取发票类型名称
     */
    private String getInvoiceTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "SPECIAL" -> "增值税专用发票";
            case "NORMAL" -> "增值税普通发票";
            case "ELECTRONIC" -> "电子发票";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待开票";
            case "ISSUED" -> "已开票";
            case "CANCELLED" -> "已作废";
            case "RED" -> "已红冲";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private InvoiceDTO toDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNo(invoice.getInvoiceNo());
        dto.setFeeId(invoice.getFeeId());
        dto.setContractId(invoice.getContractId());
        dto.setClientId(invoice.getClientId());
        dto.setInvoiceType(invoice.getInvoiceType());
        dto.setInvoiceTypeName(getInvoiceTypeName(invoice.getInvoiceType()));
        dto.setTitle(invoice.getTitle());
        dto.setTaxNo(invoice.getTaxNo());
        dto.setAmount(invoice.getAmount());
        dto.setTaxRate(invoice.getTaxRate());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setContent(invoice.getContent());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus());
        dto.setStatusName(getStatusName(invoice.getStatus()));
        dto.setApplicantId(invoice.getApplicantId());
        dto.setIssuerId(invoice.getIssuerId());
        dto.setFileUrl(invoice.getFileUrl());
        dto.setRemark(invoice.getRemark());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }

    /**
     * 获取发票统计（M4-034）
     */
    public InvoiceStatisticsDTO getInvoiceStatistics() {
        InvoiceStatisticsDTO statistics = new InvoiceStatisticsDTO();

        // 总开票金额（已开票状态）
        BigDecimal totalAmount = invoiceMapper.sumTotalInvoiceAmount();
        statistics.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);

        // 本月开票金额
        BigDecimal monthlyAmount = invoiceMapper.sumMonthlyInvoiceAmount();
        statistics.setMonthlyAmount(monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO);

        // 本年开票金额
        BigDecimal yearlyAmount = invoiceMapper.sumYearlyInvoiceAmount();
        statistics.setYearlyAmount(yearlyAmount != null ? yearlyAmount : BigDecimal.ZERO);

        // 按客户统计
        List<Map<String, Object>> byClient = invoiceMapper.countByClient();
        statistics.setByClient(byClient);

        // 按发票类型统计
        List<Map<String, Object>> byType = invoiceMapper.countByType();
        statistics.setByType(byType);

        // 按状态统计
        List<Map<String, Object>> byStatus = invoiceMapper.countByStatus();
        statistics.setByStatus(byStatus);

        // 按时间统计（趋势，最近12个月）
        List<Map<String, Object>> byDate = invoiceMapper.countByDate();
        statistics.setByDate(byDate);

        log.info("获取发票统计: total={}, monthly={}, yearly={}", totalAmount, monthlyAmount, yearlyAmount);
        return statistics;
    }
}

