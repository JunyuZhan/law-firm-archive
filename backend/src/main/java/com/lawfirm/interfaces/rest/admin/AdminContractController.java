package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.dto.AdminContractQueryDTO;
import com.lawfirm.application.admin.dto.AdminContractViewDTO;
import com.lawfirm.application.admin.service.AdminContractQueryService;
import com.lawfirm.application.admin.service.JudicialFilingExportService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 行政合同查询 Controller（只读）
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2
 */
@RestController
@RequestMapping("/admin/contract")
@RequiredArgsConstructor
public class AdminContractController {

    private final AdminContractQueryService contractQueryService;
    private final JudicialFilingExportService exportService;

    /**
     * 分页查询已审批合同列表（只读）
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4
     */
    @GetMapping("/list")
    @RequirePermission("admin:contract:list")
    public Result<PageResult<AdminContractViewDTO>> listContracts(AdminContractQueryDTO query) {
        PageResult<AdminContractViewDTO> result = contractQueryService.listApprovedContracts(query);
        return Result.success(result);
    }

    /**
     * 获取合同详情（只读）
     * 
     * Requirement: 5.5
     */
    @GetMapping("/{id}")
    @RequirePermission("admin:contract:list")
    public Result<AdminContractViewDTO> getContract(@PathVariable Long id) {
        AdminContractViewDTO contract = contractQueryService.getContractById(id);
        if (contract == null) {
            return Result.fail("合同不存在或未审批通过");
        }
        return Result.success(contract);
    }

    /**
     * 导出司法局报备收案清单
     * 
     * Requirements: 6.1, 6.2
     */
    @GetMapping("/export/judicial-filing")
    @RequirePermission("admin:contract:export")
    @OperationLog(module = "行政管理", action = "导出司法局报备")
    public void exportJudicialFiling(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Set<String> customFields,
            HttpServletResponse response) throws IOException {
        
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
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = excelStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}
