package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.evidence.command.CreateEvidenceListCommand;
import com.lawfirm.application.evidence.dto.EvidenceListCompareResult;
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.application.evidence.service.EvidenceListAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 证据清单接口 */
@Tag(name = "证据清单")
@RestController
@RequestMapping("/evidence/list")
@RequiredArgsConstructor
public class EvidenceListController {

  /** 证据清单应用服务 */
  private final EvidenceListAppService listAppService;

  /**
   * 分页查询证据清单
   *
   * @param matterId 案件ID
   * @param listType 清单类型
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  @Operation(summary = "分页查询证据清单")
  @GetMapping
  @RequirePermission("evidence:view")
  public Result<PageResult<EvidenceListDTO>> list(
      @RequestParam(required = false) final Long matterId,
      @RequestParam(required = false) final String listType,
      @RequestParam(defaultValue = "1") final int pageNum,
      @RequestParam(defaultValue = "10") final int pageSize) {
    return Result.success(listAppService.listEvidenceLists(matterId, listType, pageNum, pageSize));
  }

  /**
   * 获取清单详情
   *
   * @param id 清单ID
   * @return 清单信息
   */
  @Operation(summary = "获取清单详情")
  @GetMapping("/{id}")
  @RequirePermission("evidence:view")
  public Result<EvidenceListDTO> getById(@PathVariable final Long id) {
    return Result.success(listAppService.getListById(id));
  }

  /**
   * 创建证据清单
   *
   * @param command 创建清单命令
   * @return 清单信息
   */
  @Operation(summary = "创建证据清单")
  @PostMapping
  @RequirePermission("evidence:create")
  @OperationLog(module = "证据清单", action = "创建证据清单")
  public Result<EvidenceListDTO> create(
      @Valid @RequestBody final CreateEvidenceListCommand command) {
    return Result.success(listAppService.createList(command));
  }

  /**
   * 更新证据清单
   *
   * @param id 清单ID
   * @param name 清单名称
   * @param listType 清单类型
   * @param evidenceIds 证据ID列表
   * @return 清单信息
   */
  @Operation(summary = "更新证据清单")
  @PutMapping("/{id}")
  @RequirePermission("evidence:update")
  @OperationLog(module = "证据清单", action = "更新证据清单")
  public Result<EvidenceListDTO> update(
      @PathVariable final Long id,
      @RequestParam(required = false) final String name,
      @RequestParam(required = false) final String listType,
      @RequestBody(required = false) final List<Long> evidenceIds) {
    return Result.success(listAppService.updateList(id, name, listType, evidenceIds));
  }

  /**
   * 删除证据清单
   *
   * @param id 清单ID
   * @return 空结果
   */
  @Operation(summary = "删除证据清单")
  @DeleteMapping("/{id}")
  @RequirePermission("evidence:delete")
  @OperationLog(module = "证据清单", action = "删除证据清单")
  public Result<Void> delete(@PathVariable final Long id) {
    listAppService.deleteList(id);
    return Result.success();
  }

  /**
   * 生成清单文件
   *
   * @param id 清单ID
   * @param format 文件格式
   * @return 文件URL
   */
  @Operation(summary = "生成清单文件")
  @PostMapping("/{id}/generate")
  @RequirePermission("evidence:update")
  @OperationLog(module = "证据清单", action = "生成证据清单文件")
  public Result<String> generate(
      @PathVariable final Long id, @RequestParam(defaultValue = "docx") final String format) {
    return Result.success(listAppService.generateListFile(id, format));
  }

  /**
   * 导出证据清单为Word格式
   *
   * @param id 清单ID
   * @return Word文件响应
   */
  @Operation(summary = "导出证据清单为Word格式")
  @GetMapping("/{id}/export/word")
  @RequirePermission("evidence:view")
  @OperationLog(module = "证据清单", action = "导出证据清单Word")
  public ResponseEntity<byte[]> exportToWord(@PathVariable final Long id) {
    EvidenceListDTO list = listAppService.getListById(id);
    byte[] document = listAppService.exportToWord(id);

    String fileName = (list.getName() != null ? list.getName() : "证据清单") + ".docx";
    String encodedFileName =
        URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(document);
  }

  /**
   * 导出证据清单为PDF格式
   *
   * @param id 清单ID
   * @return PDF文件响应
   */
  @Operation(summary = "导出证据清单为PDF格式")
  @GetMapping("/{id}/export/pdf")
  @RequirePermission("evidence:view")
  @OperationLog(module = "证据清单", action = "导出证据清单PDF")
  public ResponseEntity<byte[]> exportToPdf(@PathVariable final Long id) {
    EvidenceListDTO list = listAppService.getListById(id);
    byte[] document = listAppService.exportToPdf(id);

    String fileName = (list.getName() != null ? list.getName() : "证据清单") + ".pdf";
    String encodedFileName =
        URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
        .contentType(MediaType.APPLICATION_PDF)
        .body(document);
  }

  /**
   * 按案件获取清单列表
   *
   * @param matterId 案件ID
   * @return 清单列表
   */
  @Operation(summary = "按案件获取清单列表")
  @GetMapping("/matter/{matterId}")
  @RequirePermission("evidence:view")
  public Result<List<EvidenceListDTO>> getByMatter(@PathVariable final Long matterId) {
    return Result.success(listAppService.getListsByMatter(matterId));
  }

  /**
   * 获取案件的所有历史清单（M6-044）
   *
   * @param matterId 案件ID
   * @return 历史清单列表
   */
  @Operation(summary = "获取证据清单历史", description = "按时间倒序获取案件的所有历史清单")
  @GetMapping("/matter/{matterId}/history")
  @RequirePermission("evidence:view")
  public Result<List<EvidenceListDTO>> getListHistory(@PathVariable final Long matterId) {
    return Result.success(listAppService.getListHistory(matterId));
  }

  /**
   * 对比两个清单的差异
   *
   * @param listId1 清单1 ID
   * @param listId2 清单2 ID
   * @return 对比结果
   */
  @Operation(summary = "对比清单差异", description = "对比两个证据清单的差异，显示新增、删除、保留的证据")
  @GetMapping("/compare")
  @RequirePermission("evidence:view")
  public Result<EvidenceListCompareResult> compareLists(
      @RequestParam final Long listId1, @RequestParam final Long listId2) {
    return Result.success(listAppService.compareLists(listId1, listId2));
  }

  /**
   * 将证据清单保存到卷宗
   *
   * @param id 清单ID
   * @param dossierItemId 卷宗目录项ID
   * @return 保存后的文档ID
   */
  @Operation(summary = "保存证据清单到卷宗", description = "将证据清单导出为PDF并保存到指定的卷宗目录")
  @PostMapping("/{id}/save-to-dossier")
  @RequirePermission("evidence:update")
  @OperationLog(module = "证据清单", action = "保存证据清单到卷宗")
  public Result<Long> saveToDossier(
      @PathVariable final Long id, @RequestParam final Long dossierItemId) {
    return Result.success(listAppService.saveToDossier(id, dossierItemId));
  }
}
