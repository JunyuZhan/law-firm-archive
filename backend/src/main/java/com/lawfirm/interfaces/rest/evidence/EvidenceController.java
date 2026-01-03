package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.application.evidence.service.EvidenceAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 证据管理接口
 */
@RestController
@RequestMapping("/api/evidences")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceAppService evidenceAppService;

    /**
     * 分页查询证据
     */
    @GetMapping
    @RequirePermission("evidence:view")
    public Result<PageResult<EvidenceDTO>> list(EvidenceQueryDTO query) {
        return Result.success(evidenceAppService.listEvidence(query));
    }

    /**
     * 获取证据详情
     */
    @GetMapping("/{id}")
    @RequirePermission("evidence:view")
    public Result<EvidenceDTO> getById(@PathVariable Long id) {
        return Result.success(evidenceAppService.getEvidenceById(id));
    }

    /**
     * 创建证据
     */
    @PostMapping
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据管理", action = "添加证据")
    public Result<EvidenceDTO> create(@Valid @RequestBody CreateEvidenceCommand command) {
        return Result.success(evidenceAppService.createEvidence(command));
    }

    /**
     * 更新证据
     */
    @PutMapping("/{id}")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "更新证据")
    public Result<EvidenceDTO> update(@PathVariable Long id,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String evidenceType,
                                      @RequestParam(required = false) String source,
                                      @RequestParam(required = false) String groupName,
                                      @RequestParam(required = false) String provePurpose,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) Boolean isOriginal,
                                      @RequestParam(required = false) Integer originalCount,
                                      @RequestParam(required = false) Integer copyCount,
                                      @RequestParam(required = false) Integer pageStart,
                                      @RequestParam(required = false) Integer pageEnd) {
        return Result.success(evidenceAppService.updateEvidence(id, name, evidenceType, source,
                groupName, provePurpose, description, isOriginal, originalCount, copyCount, pageStart, pageEnd));
    }

    /**
     * 删除证据
     */
    @DeleteMapping("/{id}")
    @RequirePermission("evidence:delete")
    @OperationLog(module = "证据管理", action = "删除证据")
    public Result<Void> delete(@PathVariable Long id) {
        evidenceAppService.deleteEvidence(id);
        return Result.success();
    }

    /**
     * 调整排序
     */
    @PutMapping("/{id}/sort")
    @RequirePermission("evidence:edit")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        evidenceAppService.updateSortOrder(id, sortOrder);
        return Result.success();
    }

    /**
     * 批量调整分组
     */
    @PostMapping("/batch-group")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "批量调整分组")
    public Result<Void> batchUpdateGroup(@RequestBody List<Long> ids, @RequestParam String groupName) {
        evidenceAppService.batchUpdateGroup(ids, groupName);
        return Result.success();
    }

    /**
     * 添加质证记录
     */
    @PostMapping("/{id}/cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "添加质证记录")
    public Result<EvidenceCrossExamDTO> addCrossExam(@PathVariable Long id,
                                                     @Valid @RequestBody CreateCrossExamCommand command) {
        command.setEvidenceId(id);
        return Result.success(evidenceAppService.addCrossExam(command));
    }

    /**
     * 完成质证
     */
    @PostMapping("/{id}/complete-cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "完成质证")
    public Result<Void> completeCrossExam(@PathVariable Long id) {
        evidenceAppService.completeCrossExam(id);
        return Result.success();
    }

    /**
     * 按案件获取证据列表
     */
    @GetMapping("/matter/{matterId}")
    @RequirePermission("evidence:view")
    public Result<List<EvidenceDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceByMatter(matterId));
    }

    /**
     * 获取案件的证据分组
     */
    @GetMapping("/matter/{matterId}/groups")
    @RequirePermission("evidence:view")
    public Result<List<String>> getGroups(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceGroups(matterId));
    }
}
