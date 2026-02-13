package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.service.FileAccessService;
import com.lawfirm.application.finance.command.CreateInvoiceCommand;
import com.lawfirm.application.finance.dto.InvoiceDTO;
import com.lawfirm.application.finance.dto.InvoiceStatisticsDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Invoice;
import com.lawfirm.domain.finance.repository.InvoiceRepository;
import com.lawfirm.infrastructure.persistence.mapper.InvoiceMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 发票管理应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceAppService {

  /** 发票仓储. */
  private final InvoiceRepository invoiceRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 发票Mapper. */
  private final InvoiceMapper invoiceMapper;

  /** 文件访问服务. */
  private final FileAccessService fileAccessService;

  /**
   * 分页查询发票
   *
   * @param query 分页查询条件
   * @param clientId 客户ID
   * @param status 状态
   * @return 发票分页结果
   */
  public PageResult<InvoiceDTO> listInvoices(
      final PageQuery query, final Long clientId, final String status) {
    LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();

    // 数据权限过滤
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    if ("SELF".equals(dataScope)) {
      // SELF 权限只能看自己申请的发票
      wrapper.eq(Invoice::getApplicantId, currentUserId);
    }
    // ALL/DEPT_AND_CHILD/DEPT: 财务和管理层可以查看所有发票

    if (clientId != null) {
      wrapper.eq(Invoice::getClientId, clientId);
    }
    if (status != null) {
      wrapper.eq(Invoice::getStatus, status);
    }

    wrapper.orderByDesc(Invoice::getCreatedAt);

    IPage<Invoice> page =
        invoiceRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<InvoiceDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 申请开票 支持含税/不含税两种金额输入方式
   *
   * @param command 创建发票命令
   * @return 发票DTO
   */
  @Transactional
  public InvoiceDTO applyInvoice(final CreateInvoiceCommand command) {
    clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

    // 计算税额 - 修复含税/不含税处理
    BigDecimal taxRate =
        command.getTaxRate() != null ? command.getTaxRate() : new BigDecimal("0.06");

    BigDecimal amount; // 不含税金额
    BigDecimal taxAmount; // 税额
    BigDecimal totalAmount; // 价税合计

    if (Boolean.TRUE.equals(command.getTaxIncluded())) {
      // 含税价: amount 已经包含税
      totalAmount = command.getAmount();
      // 不含税价 = 含税价 / (1 + 税率)
      amount = totalAmount.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
      // 税额 = 含税价 - 不含税价
      taxAmount = totalAmount.subtract(amount);
    } else {
      // 不含税价
      amount = command.getAmount();
      // 税额 = 不含税价 × 税率
      taxAmount = amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
      // 含税价 = 不含税价 + 税额
      totalAmount = amount.add(taxAmount);
    }

    Invoice invoice =
        Invoice.builder()
            .feeId(command.getFeeId())
            .contractId(command.getContractId())
            .clientId(command.getClientId())
            .invoiceType(command.getInvoiceType())
            .title(command.getTitle())
            .taxNo(command.getTaxNo())
            .amount(amount) // 存储不含税金额
            .taxRate(taxRate)
            .taxAmount(taxAmount)
            .content(command.getContent())
            .status("PENDING")
            .applicantId(SecurityUtils.getUserId())
            .remark(command.getRemark())
            .build();

    invoiceRepository.save(invoice);
    log.info(
        "发票申请成功: id={}, 不含税金额={}, 税额={}, 价税合计={}", invoice.getId(), amount, taxAmount, totalAmount);
    return toDTO(invoice);
  }

  /**
   * 获取发票详情
   *
   * @param id 发票ID
   * @return 发票DTO
   */
  public InvoiceDTO getInvoiceById(final Long id) {
    Invoice invoice = invoiceRepository.getByIdOrThrow(id, "发票不存在");
    return toDTO(invoice);
  }

  /**
   * 开票（确认开票）
   *
   * @param id 发票ID
   * @param invoiceNo 发票号码
   */
  @Transactional
  public void issueInvoice(final Long id, final String invoiceNo) {
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
   *
   * @param id 发票ID
   * @param reason 作废原因
   */
  @Transactional
  public void cancelInvoice(final Long id, final String reason) {
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
   *
   * @param type 发票类型代码
   * @return 发票类型名称
   */
  private String getInvoiceTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "SPECIAL" -> "增值税专用发票";
      case "NORMAL" -> "增值税普通发票";
      case "ELECTRONIC" -> "电子发票";
      default -> type;
    };
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
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
   *
   * @param invoice 发票实体
   * @return 发票DTO
   */
  private InvoiceDTO toDTO(final Invoice invoice) {
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
   * 获取发票统计（M4-034） 问题252修复：添加权限检查，仅管理员和财务可以查看统计数据
   *
   * @return 发票统计数据
   */
  public InvoiceStatisticsDTO getInvoiceStatistics() {
    // 权限检查：仅管理员和财务可以查看发票统计
    if (!SecurityUtils.isAdmin() && !SecurityUtils.getRoles().contains("FINANCE")) {
      throw new BusinessException("仅管理员和财务人员可以查看发票统计");
    }

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

  /**
   * 上传发票文件
   *
   * @param file 文件
   * @param invoiceId 发票ID
   * @return 文件URL
   */
  @Transactional
  public String uploadInvoiceFile(final MultipartFile file, final Long invoiceId) {
    Invoice invoice = invoiceRepository.getByIdOrThrow(invoiceId, "发票不存在");

    // 获取关联的项目ID（从合同或费用中获取）
    Long matterId = null;
    if (invoice.getContractId() != null) {
      // 可以从合同获取matterId，这里简化处理
      // 实际应该查询合同表获取matterId
      // 待实现：从合同表查询matterId的逻辑
      matterId = null; // 占位符，待实现
    }

    // 使用FileAccessService上传文件
    Map<String, String> storageInfo =
        fileAccessService.uploadFile(file, MinioPathGenerator.FileType.INVOICE, matterId, "发票文件");

    // 设置存储信息
    invoice.setFileUrl(storageInfo.get("fileUrl"));
    invoice.setBucketName(storageInfo.get("bucketName"));
    invoice.setStoragePath(storageInfo.get("storagePath"));
    invoice.setPhysicalName(storageInfo.get("physicalName"));
    invoice.setFileHash(storageInfo.get("fileHash"));

    invoiceRepository.updateById(invoice);

    log.info(
        "发票文件上传成功: invoiceId={}, fileName={}, storagePath={}",
        invoiceId,
        file.getOriginalFilename(),
        storageInfo.get("storagePath"));

    return storageInfo.get("fileUrl");
  }
}
