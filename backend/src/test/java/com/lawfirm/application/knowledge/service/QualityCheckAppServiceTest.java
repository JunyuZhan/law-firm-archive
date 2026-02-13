package com.lawfirm.application.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.knowledge.command.CreateQualityCheckCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.QualityCheck;
import com.lawfirm.domain.knowledge.entity.QualityCheckDetail;
import com.lawfirm.domain.knowledge.repository.QualityCheckDetailRepository;
import com.lawfirm.domain.knowledge.repository.QualityCheckRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckDetailMapper;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** QualityCheckAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QualityCheckAppService 质量检查服务测试")
class QualityCheckAppServiceTest {

  private static final Long TEST_CHECK_ID = 100L;
  private static final Long TEST_MATTER_ID = 200L;
  private static final Long TEST_STANDARD_ID = 300L;
  private static final Long TEST_USER_ID = 1L;

  @Mock private QualityCheckRepository checkRepository;

  @Mock private QualityCheckMapper checkMapper;

  @Mock private QualityCheckDetailRepository detailRepository;

  @Mock private QualityCheckDetailMapper detailMapper;

  @Mock private MatterRepository matterRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private QualityCheckAppService checkAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

    // 设置默认配置值
    ReflectionTestUtils.setField(checkAppService, "passThreshold", 80);
    ReflectionTestUtils.setField(checkAppService, "requireAllPass", true);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建检查测试")
  class CreateCheckTests {

    @Test
    @DisplayName("应该成功创建质量检查")
    void createCheck_shouldSuccess() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();

      CreateQualityCheckCommand.CheckDetailCommand detailCmd =
          new CreateQualityCheckCommand.CheckDetailCommand();
      detailCmd.setStandardId(TEST_STANDARD_ID);
      detailCmd.setCheckResult(QualityCheckDetail.RESULT_PASS);
      detailCmd.setScore(BigDecimal.valueOf(90));
      detailCmd.setMaxScore(BigDecimal.valueOf(100));
      detailCmd.setFindings("检查发现");
      detailCmd.setSuggestions("建议");

      CreateQualityCheckCommand command = new CreateQualityCheckCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setCheckDate(LocalDate.now());
      command.setCheckType(QualityCheck.TYPE_ROUTINE);
      command.setCheckSummary("检查总结");
      command.setDetails(List.of(detailCmd));

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(checkRepository.save(any(QualityCheck.class)))
          .thenAnswer(
              invocation -> {
                QualityCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return true;
              });
      when(detailRepository.save(any(QualityCheckDetail.class)))
          .thenAnswer(
              invocation -> {
                QualityCheckDetail detail = invocation.getArgument(0);
                detail.setId(1L);
                return true;
              });
      when(checkRepository.updateById(any(QualityCheck.class))).thenReturn(true);

      Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      QualityCheckDTO result = checkAppService.createCheck(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
      assertThat(result.getCheckType()).isEqualTo(QualityCheck.TYPE_ROUTINE);
      verify(checkRepository).save(any(QualityCheck.class));
      verify(detailRepository).save(any(QualityCheckDetail.class));
    }

    @Test
    @DisplayName("应该成功计算合格结果当所有项目通过且分数达标")
    void createCheck_shouldCalculateQualified_whenAllPassAndScoreMet() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();

      CreateQualityCheckCommand.CheckDetailCommand detailCmd =
          new CreateQualityCheckCommand.CheckDetailCommand();
      detailCmd.setStandardId(TEST_STANDARD_ID);
      detailCmd.setCheckResult(QualityCheckDetail.RESULT_PASS);
      detailCmd.setScore(BigDecimal.valueOf(90));
      detailCmd.setMaxScore(BigDecimal.valueOf(100));

      CreateQualityCheckCommand command = new CreateQualityCheckCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setCheckDate(LocalDate.now());
      command.setCheckType(QualityCheck.TYPE_ROUTINE);
      command.setDetails(List.of(detailCmd));

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(checkRepository.save(any(QualityCheck.class)))
          .thenAnswer(
              invocation -> {
                QualityCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return true;
              });
      when(detailRepository.save(any(QualityCheckDetail.class)))
          .thenAnswer(
              invocation -> {
                QualityCheckDetail detail = invocation.getArgument(0);
                detail.setId(1L);
                return true;
              });
      when(checkRepository.updateById(any(QualityCheck.class)))
          .thenAnswer(
              invocation -> {
                QualityCheck check = invocation.getArgument(0);
                // 验证合格计算
                assertThat(check.getQualified()).isTrue();
                assertThat(check.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(90));
                return true;
              });

      Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      checkAppService.createCheck(command);

      // Then
      verify(checkRepository).updateById(any(QualityCheck.class));
    }

    @Test
    @DisplayName("应该计算不合格当有项目未通过")
    void createCheck_shouldCalculateUnqualified_whenNotAllPass() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();

      CreateQualityCheckCommand.CheckDetailCommand detailCmd =
          new CreateQualityCheckCommand.CheckDetailCommand();
      detailCmd.setStandardId(TEST_STANDARD_ID);
      detailCmd.setCheckResult(QualityCheckDetail.RESULT_FAIL);
      detailCmd.setScore(BigDecimal.valueOf(60));
      detailCmd.setMaxScore(BigDecimal.valueOf(100));

      CreateQualityCheckCommand command = new CreateQualityCheckCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setCheckDate(LocalDate.now());
      command.setCheckType(QualityCheck.TYPE_ROUTINE);
      command.setDetails(List.of(detailCmd));

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(checkRepository.save(any(QualityCheck.class)))
          .thenAnswer(
              invocation -> {
                QualityCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return true;
              });
      when(detailRepository.save(any(QualityCheckDetail.class)))
          .thenAnswer(
              invocation -> {
                QualityCheckDetail detail = invocation.getArgument(0);
                detail.setId(1L);
                return true;
              });
      when(checkRepository.updateById(any(QualityCheck.class)))
          .thenAnswer(
              invocation -> {
                QualityCheck check = invocation.getArgument(0);
                // 验证不合格计算
                assertThat(check.getQualified()).isFalse();
                return true;
              });

      Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      checkAppService.createCheck(command);

      // Then
      verify(checkRepository).updateById(any(QualityCheck.class));
    }

    @Test
    @DisplayName("应该失败当项目不存在")
    void createCheck_shouldFail_whenMatterNotExists() {
      // Given
      CreateQualityCheckCommand command = new CreateQualityCheckCommand();
      command.setMatterId(TEST_MATTER_ID);

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString()))
          .thenThrow(new BusinessException("项目不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> checkAppService.createCheck(command));
    }
  }

  @Nested
  @DisplayName("查询检查测试")
  class QueryCheckTests {

    @Test
    @DisplayName("应该成功获取检查详情")
    void getCheckById_shouldSuccess() {
      // Given
      QualityCheck check =
          QualityCheck.builder()
              .id(TEST_CHECK_ID)
              .matterId(TEST_MATTER_ID)
              .checkerId(TEST_USER_ID)
              .checkType(QualityCheck.TYPE_ROUTINE)
              .status(QualityCheck.STATUS_COMPLETED)
              .qualified(true)
              .totalScore(BigDecimal.valueOf(90))
              .build();

      QualityCheckDetail detail =
          QualityCheckDetail.builder()
              .id(1L)
              .checkId(TEST_CHECK_ID)
              .standardId(TEST_STANDARD_ID)
              .checkResult(QualityCheckDetail.RESULT_PASS)
              .score(BigDecimal.valueOf(90))
              .maxScore(BigDecimal.valueOf(100))
              .build();

      when(checkRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
      when(detailMapper.selectByCheckId(TEST_CHECK_ID)).thenReturn(List.of(detail));

      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      QualityCheckDTO result = checkAppService.getCheckById(TEST_CHECK_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(TEST_CHECK_ID);
      assertThat(result.getDetails()).hasSize(1);
    }

    @Test
    @DisplayName("应该成功获取项目的所有检查")
    void getChecksByMatterId_shouldSuccess() {
      // Given
      QualityCheck check =
          QualityCheck.builder()
              .id(TEST_CHECK_ID)
              .matterId(TEST_MATTER_ID)
              .checkerId(TEST_USER_ID)
              .build();

      when(checkMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(List.of(check));

      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      List<QualityCheckDTO> result = checkAppService.getChecksByMatterId(TEST_MATTER_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("应该成功获取进行中的检查")
    void getInProgressChecks_shouldSuccess() {
      // Given
      QualityCheck check =
          QualityCheck.builder()
              .id(TEST_CHECK_ID)
              .matterId(TEST_MATTER_ID)
              .checkerId(TEST_USER_ID)
              .status(QualityCheck.STATUS_IN_PROGRESS)
              .build();

      when(checkMapper.selectInProgress()).thenReturn(List.of(check));

      Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
      User checker = User.builder().id(TEST_USER_ID).realName("检查人").build();
      when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);
      when(userRepository.getById(TEST_USER_ID)).thenReturn(checker);

      // When
      List<QualityCheckDTO> result = checkAppService.getInProgressChecks();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
    }
  }
}
