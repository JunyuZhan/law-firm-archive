package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.dto.AdminContractQueryDTO;
import com.lawfirm.application.admin.dto.AdminContractViewDTO;
import com.lawfirm.application.admin.service.AdminContractQueryService;
import com.lawfirm.application.admin.service.JudicialFilingExportService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行政合同查询 Controller（只读）
 *
 * <p>Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2
 */
@RestController
@RequestMapping("/admin/contract")
@RequiredArgsConstructor
public class AdminContractController {

  /** 文件缓冲区大小：4KB */
  private static final int FILE_BUFFER_SIZE = 4096;

  /** 合同查询服务 */
  private final AdminContractQueryService contractQueryService;

  /** 司法立案导出服务 */
  private final JudicialFilingExportService exportService;

  /**
   * 分页查询已审批合同列表（只读）
   *
   * <p>Requirements: 5.1, 5.2, 5.3, 5.4
   *
   * @param query 查询条件
   * @return 合同列表
   */
  @GetMapping("/list")
  @RequirePermission("admin:contract:list")
  public Result<PageResult<AdminContractViewDTO>> listContracts(final AdminContractQueryDTO query) {
    PageResult<AdminContractViewDTO> result = contractQueryService.listApprovedContracts(query);
    return Result.success(result);
  }

  /**
   * 获取合同详情（只读）
   *
   * <p>Requirement: 5.5
   *
   * @param id 合同ID
   * @return 合同详情
   */
  @GetMapping("/{id}")
  @RequirePermission("admin:contract:list")
  public Result<AdminContractViewDTO> getContract(@PathVariable final Long id) {
    AdminContractViewDTO contract = contractQueryService.getContractById(id);
    if (contract == null) {
      return Result.fail("合同不存在或未审批通过");
    }
    return Result.success(contract);
  }

  /**
   * 导出司法局报备收案清单
   *
   * <p>Requirements: 6.1, 6.2
   *
   * @param year 年份
   * @param month 月份
   * @param customFields 自定义字段
   * @param response HTTP响应
   * @throws IOException IO异常
   */
  @GetMapping("/export/judicial-filing")
  @RequirePermission("admin:contract:export")
  public void exportJudicialFiling(
      @RequestParam final int year,
      @RequestParam final int month,
      @RequestParam(required = false) final Set<String> customFields,
      final HttpServletResponse response)
      throws IOException {

    Long operatorId = SecurityUtils.getCurrentUserId();

    ByteArrayInputStream excelStream;
    if (customFields != null && !customFields.isEmpty()) {
      excelStream = exportService.exportMonthlyFilingReport(year, month, customFields, operatorId);
    } else {
      excelStream = exportService.exportMonthlyFilingReport(year, month, operatorId);
    }

    String fileName = String.format("收案清单_%d年%d月.xlsx", year, month);
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

    try (OutputStream out = response.getOutputStream()) {
      byte[] buffer = new byte[FILE_BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = excelStream.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.flush();
    }
  }

  /**
   * 导出合同列表（根据查询条件） 导出当前查询结果到Excel
   *
   * @param query 查询条件
   * @param response HTTP响应
   * @throws IOException IO异常
   */
  @GetMapping("/export/list")
  @RequirePermission("admin:contract:export")
  public void exportContractList(
      final AdminContractQueryDTO query, final HttpServletResponse response) throws IOException {
    Long operatorId = SecurityUtils.getCurrentUserId();

    // 获取所有符合条件的合同（不分页）
    query.setPageNum(1);
    query.setPageSize(Integer.MAX_VALUE);
    PageResult<AdminContractViewDTO> result = contractQueryService.listApprovedContracts(query);

    ByteArrayInputStream excelStream =
        exportService.exportContractList(result.getRecords(), operatorId);

    String fileName =
        String.format(
            "合同列表_%s.xlsx", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

    try (OutputStream out = response.getOutputStream()) {
      byte[] buffer = new byte[FILE_BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = excelStream.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.flush();
    }
  }
}
