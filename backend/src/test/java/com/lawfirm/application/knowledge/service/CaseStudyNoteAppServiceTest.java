package com.lawfirm.application.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** CaseStudyNoteAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CaseStudyNoteAppService 案例学习笔记服务测试")
class CaseStudyNoteAppServiceTest {

  private static final Long TEST_CASE_ID = 100L;
  private static final Long TEST_NOTE_ID = 200L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long OTHER_USER_ID = 999L;

  @Mock private CaseStudyNoteRepository noteRepository;

  @Mock private CaseStudyNoteMapper noteMapper;

  @Mock private CaseLibraryRepository caseLibraryRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private CaseStudyNoteAppService noteAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("保存笔记测试")
  class SaveNoteTests {

    @Test
    @DisplayName("应该成功创建新笔记")
    void saveNote_shouldSuccess_whenCreateNew() {
      // Given
      CaseLibrary caseLib = CaseLibrary.builder().id(TEST_CASE_ID).title("测试案例").build();

      CreateCaseStudyNoteCommand command = new CreateCaseStudyNoteCommand();
      command.setCaseId(TEST_CASE_ID);
      command.setNoteContent("笔记内容");
      command.setKeyPoints("关键点1,关键点2");
      command.setPersonalInsights("个人见解");

      when(caseLibraryRepository.getByIdOrThrow(eq(TEST_CASE_ID), anyString())).thenReturn(caseLib);
      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(null);
      when(noteRepository.save(any(CaseStudyNote.class)))
          .thenAnswer(
              invocation -> {
                CaseStudyNote note = invocation.getArgument(0);
                note.setId(TEST_NOTE_ID);
                return true;
              });

      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);
      when(caseLibraryRepository.getById(TEST_CASE_ID)).thenReturn(caseLib);

      // When
      CaseStudyNoteDTO result = noteAppService.saveNote(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(result.getNoteContent()).isEqualTo("笔记内容");
      verify(noteRepository).save(any(CaseStudyNote.class));
    }

    @Test
    @DisplayName("应该成功更新现有笔记")
    void saveNote_shouldSuccess_whenUpdateExisting() {
      // Given
      CaseLibrary caseLib = CaseLibrary.builder().id(TEST_CASE_ID).title("测试案例").build();

      CaseStudyNote existingNote =
          CaseStudyNote.builder()
              .id(TEST_NOTE_ID)
              .caseId(TEST_CASE_ID)
              .userId(TEST_USER_ID)
              .noteContent("旧内容")
              .build();

      CreateCaseStudyNoteCommand command = new CreateCaseStudyNoteCommand();
      command.setCaseId(TEST_CASE_ID);
      command.setNoteContent("新内容");
      command.setKeyPoints("新关键点");
      command.setPersonalInsights("新见解");

      when(caseLibraryRepository.getByIdOrThrow(eq(TEST_CASE_ID), anyString())).thenReturn(caseLib);
      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(existingNote);
      when(noteRepository.updateById(any(CaseStudyNote.class))).thenReturn(true);

      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);
      when(caseLibraryRepository.getById(TEST_CASE_ID)).thenReturn(caseLib);

      // When
      CaseStudyNoteDTO result = noteAppService.saveNote(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(existingNote.getNoteContent()).isEqualTo("新内容");
      verify(noteRepository).updateById(existingNote);
      verify(noteRepository, never()).save(any(CaseStudyNote.class));
    }

    @Test
    @DisplayName("应该失败当案例不存在")
    void saveNote_shouldFail_whenCaseNotExists() {
      // Given
      CreateCaseStudyNoteCommand command = new CreateCaseStudyNoteCommand();
      command.setCaseId(TEST_CASE_ID);

      when(caseLibraryRepository.getByIdOrThrow(eq(TEST_CASE_ID), anyString()))
          .thenThrow(new BusinessException("案例不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> noteAppService.saveNote(command));
    }
  }

  @Nested
  @DisplayName("查询笔记测试")
  class QueryNoteTests {

    @Test
    @DisplayName("应该成功获取我的笔记")
    void getMyNote_shouldSuccess() {
      // Given
      CaseStudyNote note =
          CaseStudyNote.builder()
              .id(TEST_NOTE_ID)
              .caseId(TEST_CASE_ID)
              .userId(TEST_USER_ID)
              .noteContent("笔记内容")
              .build();

      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(note);

      CaseLibrary caseLib = CaseLibrary.builder().id(TEST_CASE_ID).title("测试案例").build();
      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(caseLibraryRepository.getById(TEST_CASE_ID)).thenReturn(caseLib);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);

      // When
      CaseStudyNoteDTO result = noteAppService.getMyNote(TEST_CASE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(result.getNoteContent()).isEqualTo("笔记内容");
    }

    @Test
    @DisplayName("应该返回null当笔记不存在")
    void getMyNote_shouldReturnNull_whenNoteNotExists() {
      // Given
      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(null);

      // When
      CaseStudyNoteDTO result = noteAppService.getMyNote(TEST_CASE_ID);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("应该成功获取案例的所有笔记")
    void getCaseNotes_shouldSuccess() {
      // Given
      CaseStudyNote note1 =
          CaseStudyNote.builder()
              .id(TEST_NOTE_ID)
              .caseId(TEST_CASE_ID)
              .userId(TEST_USER_ID)
              .noteContent("笔记1")
              .build();

      CaseStudyNote note2 =
          CaseStudyNote.builder()
              .id(300L)
              .caseId(TEST_CASE_ID)
              .userId(OTHER_USER_ID)
              .noteContent("笔记2")
              .build();

      when(noteMapper.selectByCaseId(TEST_CASE_ID)).thenReturn(List.of(note1, note2));

      CaseLibrary caseLib = CaseLibrary.builder().id(TEST_CASE_ID).title("测试案例").build();
      User user1 = User.builder().id(TEST_USER_ID).realName("用户1").build();
      User user2 = User.builder().id(OTHER_USER_ID).realName("用户2").build();
      when(caseLibraryRepository.getById(TEST_CASE_ID)).thenReturn(caseLib);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user1);
      when(userRepository.getById(OTHER_USER_ID)).thenReturn(user2);

      // When
      List<CaseStudyNoteDTO> result = noteAppService.getCaseNotes(TEST_CASE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("应该成功获取我的所有笔记")
    void getMyNotes_shouldSuccess() {
      // Given
      CaseStudyNote note1 =
          CaseStudyNote.builder()
              .id(TEST_NOTE_ID)
              .caseId(TEST_CASE_ID)
              .userId(TEST_USER_ID)
              .noteContent("笔记1")
              .build();

      CaseStudyNote note2 =
          CaseStudyNote.builder()
              .id(300L)
              .caseId(200L)
              .userId(TEST_USER_ID)
              .noteContent("笔记2")
              .build();

      when(noteMapper.selectByUserId(TEST_USER_ID)).thenReturn(List.of(note1, note2));

      CaseLibrary caseLib1 = CaseLibrary.builder().id(TEST_CASE_ID).title("案例1").build();
      CaseLibrary caseLib2 = CaseLibrary.builder().id(200L).title("案例2").build();
      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(caseLibraryRepository.getById(TEST_CASE_ID)).thenReturn(caseLib1);
      when(caseLibraryRepository.getById(200L)).thenReturn(caseLib2);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);

      // When
      List<CaseStudyNoteDTO> result = noteAppService.getMyNotes();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("应该返回空列表当没有笔记")
    void getMyNotes_shouldReturnEmpty_whenNoNotes() {
      // Given
      when(noteMapper.selectByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

      // When
      List<CaseStudyNoteDTO> result = noteAppService.getMyNotes();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("删除笔记测试")
  class DeleteNoteTests {

    @Test
    @DisplayName("应该成功删除笔记")
    void deleteNote_shouldSuccess() {
      // Given
      CaseStudyNote note =
          CaseStudyNote.builder()
              .id(TEST_NOTE_ID)
              .caseId(TEST_CASE_ID)
              .userId(TEST_USER_ID)
              .build();

      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(note);
      when(noteRepository.removeById(TEST_NOTE_ID)).thenReturn(true);

      // When
      noteAppService.deleteNote(TEST_CASE_ID);

      // Then
      verify(noteRepository).removeById(TEST_NOTE_ID);
    }

    @Test
    @DisplayName("应该失败当笔记不存在")
    void deleteNote_shouldFail_whenNoteNotExists() {
      // Given
      when(noteMapper.selectByCaseAndUser(TEST_CASE_ID, TEST_USER_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> noteAppService.deleteNote(TEST_CASE_ID));
      assertThat(exception.getMessage()).contains("学习笔记不存在");
    }
  }
}
