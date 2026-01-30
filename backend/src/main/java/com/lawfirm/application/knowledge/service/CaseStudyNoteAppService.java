package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateCaseStudyNoteCommand;
import com.lawfirm.application.knowledge.dto.CaseStudyNoteDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.CaseLibrary;
import com.lawfirm.domain.knowledge.entity.CaseStudyNote;
import com.lawfirm.domain.knowledge.repository.CaseLibraryRepository;
import com.lawfirm.domain.knowledge.repository.CaseStudyNoteRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.CaseStudyNoteMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 案例学习笔记应用服务（M10-013） */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseStudyNoteAppService {

  /** 案例学习笔记仓储 */
  private final CaseStudyNoteRepository noteRepository;

  /** 案例学习笔记Mapper */
  private final CaseStudyNoteMapper noteMapper;

  /** 案例库仓储 */
  private final CaseLibraryRepository caseLibraryRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 创建或更新学习笔记
   *
   * @param command 创建命令
   * @return 笔记DTO
   */
  @Transactional
  public CaseStudyNoteDTO saveNote(final CreateCaseStudyNoteCommand command) {
    caseLibraryRepository.getByIdOrThrow(command.getCaseId(), "案例不存在");
    Long userId = SecurityUtils.getUserId();

    CaseStudyNote existing = noteMapper.selectByCaseAndUser(command.getCaseId(), userId);
    CaseStudyNote note;

    if (existing != null) {
      // 更新现有笔记
      note = existing;
      note.setNoteContent(command.getNoteContent());
      note.setKeyPoints(command.getKeyPoints());
      note.setPersonalInsights(command.getPersonalInsights());
      noteRepository.updateById(note);
      log.info("更新案例学习笔记: caseId={}, userId={}", command.getCaseId(), userId);
    } else {
      // 创建新笔记
      note =
          CaseStudyNote.builder()
              .caseId(command.getCaseId())
              .userId(userId)
              .noteContent(command.getNoteContent())
              .keyPoints(command.getKeyPoints())
              .personalInsights(command.getPersonalInsights())
              .build();
      noteRepository.save(note);
      log.info("创建案例学习笔记: caseId={}, userId={}", command.getCaseId(), userId);
    }

    return toDTO(note);
  }

  /**
   * 获取我的学习笔记
   *
   * @param caseId 案例ID
   * @return 笔记DTO
   */
  public CaseStudyNoteDTO getMyNote(final Long caseId) {
    Long userId = SecurityUtils.getUserId();
    CaseStudyNote note = noteMapper.selectByCaseAndUser(caseId, userId);
    if (note == null) {
      return null;
    }
    return toDTO(note);
  }

  /**
   * 获取案例的所有学习笔记
   *
   * @param caseId 案例ID
   * @return 笔记列表
   */
  public List<CaseStudyNoteDTO> getCaseNotes(final Long caseId) {
    List<CaseStudyNote> notes = noteMapper.selectByCaseId(caseId);
    return notes.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取我的所有学习笔记
   *
   * @return 笔记列表
   */
  public List<CaseStudyNoteDTO> getMyNotes() {
    Long userId = SecurityUtils.getUserId();
    List<CaseStudyNote> notes = noteMapper.selectByUserId(userId);
    return notes.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 删除学习笔记
   *
   * @param caseId 案例ID
   */
  @Transactional
  public void deleteNote(final Long caseId) {
    Long userId = SecurityUtils.getUserId();
    CaseStudyNote note = noteMapper.selectByCaseAndUser(caseId, userId);
    if (note == null) {
      throw new BusinessException("学习笔记不存在");
    }
    noteRepository.removeById(note.getId());
    log.info("删除案例学习笔记: caseId={}, userId={}", caseId, userId);
  }

  /**
   * Entity转DTO
   *
   * @param note 案例学习笔记实体
   * @return 案例学习笔记DTO
   */
  private CaseStudyNoteDTO toDTO(final CaseStudyNote note) {
    CaseStudyNoteDTO dto = new CaseStudyNoteDTO();
    dto.setId(note.getId());
    dto.setCaseId(note.getCaseId());
    dto.setUserId(note.getUserId());
    dto.setNoteContent(note.getNoteContent());
    dto.setKeyPoints(note.getKeyPoints());
    dto.setPersonalInsights(note.getPersonalInsights());
    dto.setCreatedAt(note.getCreatedAt());
    dto.setUpdatedAt(note.getUpdatedAt());

    // 获取案例信息
    CaseLibrary caseLib = caseLibraryRepository.getById(note.getCaseId());
    if (caseLib != null) {
      dto.setCaseTitle(caseLib.getTitle());
    }

    // 获取用户信息
    User user = userRepository.getById(note.getUserId());
    if (user != null) {
      dto.setUserName(user.getRealName());
    }

    return dto;
  }
}
