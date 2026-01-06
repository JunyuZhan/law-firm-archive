package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.evidence.command.CreateEvidenceListCommand;
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.application.evidence.service.EvidenceListAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 证据清单接口
 */
@Tag(name = "证据清单")
@RestController
@RequestMapping("/evidence/list")
@RequiredArgsConstructor
public class EvidenceListController {

    private final EvidenceListAppService listAppService;

    @Operation(summary = "分页查询证据清单")
    @GetMapping
    @RequirePermission("evidence:view")
    public Result<PageResult<EvidenceListDTO>> list(
            @RequestParam(required = false) Long matterId,
            @RequestParam(required = false) String listType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(listAppService.listEvidenceLists(matterId, listType, pageNum, pageSize));
    }

    @Operation(summary = "获取清单详情")
    @GetMapping("/{id}")
    @RequirePermission("evidence:view")
    public Result<EvidenceListDTO> getById(@PathVariable Long id) {
        return Result.success(listAppService.getListById(id));
    }

    @Operation(summary = "创建证据清单")
    @PostMapping
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据清单", action = "创建证据清单")
    public Result<EvidenceListDTO> create(@Valid @RequestBody CreateEvidenceListCommand command) {
        return Result.success(listAppService.createList(command));
    }

    @Operation(summary = "更新证据清单")
    @PutMapping("/{id}")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据清单", action = "更新证据清单")
    public Result<EvidenceListDTO> update(@PathVariable Long id,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String listType,
                                          @RequestBody(required = false) List<Long> evidenceIds) {
        return Result.success(listAppService.updateList(id, name, listType, evidenceIds));
    }

    @Operation(summary = "删除证据清单")
    @DeleteMapping("/{id}")
    @RequirePermission("evidence:delete")
    @OperationLog(module = "证据清单", action = "删除证据清单")
    public Result<Void> delete(@PathVariable Long id) {
        listAppService.deleteList(id);
        return Result.success();
    }

    @Operation(summary = "生成清单文件")
    @PostMapping("/{id}/generate")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据清单", action = "生成证据清单文件")
    public Result<String> generate(@PathVariable Long id,
                                   @RequestParam(defaultValue = "docx") String format) {
        return Result.success(listAppService.generateListFile(id, format));
    }

    @Operation(summary = "导出证据清单为Word格式")
    @GetMapping("/{id}/export/word")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据清单", action = "导出证据清单Word")
    public ResponseEntity<byte[]> exportToWord(@PathVariable Long id) {
        EvidenceListDTO list = listAppService.getListById(id);
        byte[] document = listAppService.exportToWord(id);
        
        String fileName = (list.getName() != null ? list.getName() : "证据清单") + ".docx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(document);
    }

    @Operation(summary = "导出证据清单为PDF格式")
    @GetMapping("/{id}/export/pdf")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据清单", action = "导出证据清单PDF")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) {
        EvidenceListDTO list = listAppService.getListById(id);
        byte[] document = listAppService.exportToPdf(id);
        
        String fileName = (list.getName() != null ? list.getName() : "证据清单") + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(document);
    }

    @Operation(summary = "按案件获取清单列表")
    @GetMapping("/matter/{matterId}")
    @RequirePermission("evidence:view")
    public Result<List<EvidenceListDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(listAppService.getListsByMatter(matterId));
    }

    /**
     * 获取案件的所有历史清单（M6-044）
     */
    @Operation(summary = "获取证据清单历史", description = "按时间倒序获取案件的所有历史清单")
    @GetMapping("/matter/{matterId}/history")
    @RequirePermission("evidence:view")
    public Result<List<EvidenceListDTO>> getListHistory(@PathVariable Long matterId) {
        return Result.success(listAppService.getListHistory(matterId));
    }

    /**
     * 对比两个清单的差异（M6-044）
     */
    @Operation(summary = "对比清单差异", description = "对比两个证据清单的差异，显示新增、删除、保留的证据")
    @GetMapping("/compare")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> compareLists(
            @RequestParam Long listId1,
            @RequestParam Long listId2) {
        return Result.success(listAppService.compareLists(listId1, listId2));
    }
}
