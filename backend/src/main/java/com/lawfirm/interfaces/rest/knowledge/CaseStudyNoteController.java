package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateCaseStudyNoteCommand;
import com.lawfirm.application.knowledge.dto.CaseStudyNoteDTO;
import com.lawfirm.application.knowledge.service.CaseStudyNoteAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 案例学习笔记接口（M10-013） 知识库是知识共享平台： - 全员可查看案例学习笔记 - 只能编辑/删除自己的学习笔记（服务层验证所有权） */
@Tag(name = "案例学习", description = "案例学习笔记相关接口")
@RestController
@RequestMapping("/knowledge/case-study")
@RequiredArgsConstructor
public class CaseStudyNoteController {

  /** 案例学习笔记应用服务. */
  private final CaseStudyNoteAppService noteAppService;

  /**
   * 查询所有学习笔记
   *
   * @return 学习笔记列表
   */
  @Operation(summary = "查询所有学习笔记")
  @GetMapping
  @RequirePermission("knowledge:case:list")
  public Result<List<CaseStudyNoteDTO>> listNotes() {
    return Result.success(noteAppService.getMyNotes());
  }

  /**
   * 保存学习笔记
   *
   * @param command 保存命令
   * @return 保存的笔记
   */
  @Operation(summary = "保存学习笔记")
  @PostMapping("/note")
  @RequirePermission("knowledge:case:list")
  @OperationLog(module = "案例学习", action = "保存学习笔记")
  public Result<CaseStudyNoteDTO> saveNote(@RequestBody final CreateCaseStudyNoteCommand command) {
    return Result.success(noteAppService.saveNote(command));
  }

  /**
   * 获取我的学习笔记
   *
   * @param caseId 案例ID
   * @return 学习笔记
   */
  @Operation(summary = "获取我的学习笔记")
  @GetMapping("/note/{caseId}")
  @RequirePermission("knowledge:case:list")
  public Result<CaseStudyNoteDTO> getMyNote(@PathVariable final Long caseId) {
    CaseStudyNoteDTO note = noteAppService.getMyNote(caseId);
    return Result.success(note);
  }

  /**
   * 获取案例的所有学习笔记
   *
   * @param caseId 案例ID
   * @return 学习笔记列表
   */
  @Operation(summary = "获取案例的所有学习笔记")
  @GetMapping("/note/case/{caseId}")
  @RequirePermission("knowledge:case:list")
  public Result<List<CaseStudyNoteDTO>> getCaseNotes(@PathVariable final Long caseId) {
    return Result.success(noteAppService.getCaseNotes(caseId));
  }

  /**
   * 获取我的所有学习笔记
   *
   * @return 学习笔记列表
   */
  @Operation(summary = "获取我的所有学习笔记")
  @GetMapping("/note/my")
  @RequirePermission("knowledge:case:list")
  public Result<List<CaseStudyNoteDTO>> getMyNotes() {
    return Result.success(noteAppService.getMyNotes());
  }

  /**
   * 删除学习笔记
   *
   * @param caseId 案例ID
   * @return 操作结果
   */
  @Operation(summary = "删除学习笔记")
  @DeleteMapping("/note/{caseId}")
  @RequirePermission("knowledge:case:list")
  @OperationLog(module = "案例学习", action = "删除学习笔记")
  public Result<Void> deleteNote(@PathVariable final Long caseId) {
    // 服务层验证：只能删除自己的笔记
    noteAppService.deleteNote(caseId);
    return Result.success();
  }
}
