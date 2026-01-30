package com.lawfirm.application.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.hr.command.CreateDevelopmentPlanCommand;
import com.lawfirm.application.hr.dto.DevelopmentPlanDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import com.lawfirm.domain.hr.entity.DevelopmentPlan;
import com.lawfirm.domain.hr.repository.CareerLevelRepository;
import com.lawfirm.domain.hr.repository.DevelopmentMilestoneRepository;
import com.lawfirm.domain.hr.repository.DevelopmentPlanRepository;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentPlanMapper;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** DevelopmentPlanAppService 单元测试 测试发展规划服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DevelopmentPlanAppService 发展规划服务测试")
class DevelopmentPlanAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_PLAN_ID = 100L;
  private static final Long TEST_MILESTONE_ID = 200L;

  @Mock private DevelopmentPlanRepository planRepository;

  @Mock private DevelopmentPlanMapper planMapper;

  @Mock private DevelopmentMilestoneRepository milestoneRepository;

  @Mock private CareerLevelRepository levelRepository;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private DevelopmentPlanAppService developmentPlanAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getRealName).thenReturn("测试用户");
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建发展规划测试")
  class CreatePlanTests {

    @Test
    @DisplayName("应该成功创建发展规划")
    void createPlan_shouldSuccess() throws JsonProcessingException {
      // Given
      CreateDevelopmentPlanCommand command = new CreateDevelopmentPlanCommand();
      command.setPlanYear(2024);
      command.setPlanTitle("2024年发展规划");
      command.setTargetDate(LocalDate.now().plusYears(1));
      command.setCareerGoals(Arrays.asList("目标1", "目标2"));
      command.setSkillGoals(Arrays.asList("技能1"));
      command.setPerformanceGoals(Arrays.asList("绩效1"));
      command.setActionPlans(Arrays.asList("行动1"));

      when(planRepository.findByEmployeeAndYear(eq(TEST_USER_ID), eq(2024)))
          .thenReturn(Optional.empty());
      when(objectMapper.writeValueAsString(anyList())).thenReturn("[\"目标1\",\"目标2\"]");
      when(planRepository.save(any(DevelopmentPlan.class)))
          .thenAnswer(
              invocation -> {
                DevelopmentPlan plan = invocation.getArgument(0);
                plan.setId(TEST_PLAN_ID);
                plan.setPlanNo("PLAN1234567890");
                return true;
              });
      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString()))
          .thenAnswer(
              invocation -> {
                DevelopmentPlan plan = new DevelopmentPlan();
                plan.setId(TEST_PLAN_ID);
                plan.setPlanNo("PLAN1234567890");
                plan.setPlanYear(2024);
                plan.setPlanTitle("2024年发展规划");
                plan.setStatus("DRAFT");
                return plan;
              });
      when(milestoneRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(Collections.emptyList());
      when(milestoneRepository.saveBatch(anyList())).thenReturn(true);

      // When
      DevelopmentPlanDTO result = developmentPlanAppService.createPlan(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPlanTitle()).isEqualTo("2024年发展规划");
      assertThat(result.getStatus()).isEqualTo("DRAFT");
      verify(planRepository).save(any(DevelopmentPlan.class));
    }

    @Test
    @DisplayName("该年度已有规划应该失败")
    void createPlan_shouldFail_whenYearExists() {
      // Given
      CreateDevelopmentPlanCommand command = new CreateDevelopmentPlanCommand();
      command.setPlanYear(2024);

      DevelopmentPlan existingPlan = new DevelopmentPlan();
      when(planRepository.findByEmployeeAndYear(eq(TEST_USER_ID), eq(2024)))
          .thenReturn(Optional.of(existingPlan));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> developmentPlanAppService.createPlan(command));
      assertThat(exception.getMessage()).contains("已存在发展规划");
    }
  }

  @Nested
  @DisplayName("查询发展规划测试")
  class QueryPlanTests {

    @Test
    @DisplayName("应该成功分页查询发展规划")
    void listPlans_shouldSuccess() {
      // Given
      DevelopmentPlan plan =
          DevelopmentPlan.builder()
              .id(TEST_PLAN_ID)
              .planNo("PLAN001")
              .planYear(2024)
              .planTitle("规划1")
              .build();

      Page<DevelopmentPlan> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(plan));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<DevelopmentPlan> pageParam = any(Page.class);
      when(planMapper.selectPlanPage(pageParam, any(), any(), any(), any())).thenReturn(page);

      // When
      PageResult<DevelopmentPlanDTO> result =
          developmentPlanAppService.listPlans(1, 10, null, null, null, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getPlanTitle()).isEqualTo("规划1");
    }

    @Test
    @DisplayName("应该成功获取规划详情")
    void getPlanById_shouldSuccess() {
      // Given
      DevelopmentPlan plan =
          DevelopmentPlan.builder().id(TEST_PLAN_ID).planNo("PLAN001").planTitle("规划1").build();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);
      when(milestoneRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(Collections.emptyList());

      // When
      DevelopmentPlanDTO result = developmentPlanAppService.getPlanById(TEST_PLAN_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPlanTitle()).isEqualTo("规划1");
    }
  }

  @Nested
  @DisplayName("更新发展规划测试")
  class UpdatePlanTests {

    @Test
    @DisplayName("应该成功更新发展规划")
    void updatePlan_shouldSuccess() throws JsonProcessingException {
      // Given
      DevelopmentPlan plan =
          DevelopmentPlan.builder().id(TEST_PLAN_ID).planNo("PLAN001").status("DRAFT").build();

      CreateDevelopmentPlanCommand command = new CreateDevelopmentPlanCommand();
      command.setPlanTitle("更新后的规划");
      command.setCareerGoals(Arrays.asList("新目标"));

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);
      when(objectMapper.writeValueAsString(anyList())).thenReturn("[\"新目标\"]");
      when(planRepository.updateById(any(DevelopmentPlan.class))).thenReturn(true);
      doNothing().when(milestoneRepository).deleteByPlanId(TEST_PLAN_ID);
      when(milestoneRepository.saveBatch(anyList())).thenReturn(true);
      when(milestoneRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(Collections.emptyList());

      // When
      DevelopmentPlanDTO result = developmentPlanAppService.updatePlan(TEST_PLAN_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(plan.getPlanTitle()).isEqualTo("更新后的规划");
      verify(planRepository).updateById(plan);
    }

    @Test
    @DisplayName("已完成的规划不能修改")
    void updatePlan_shouldFail_whenCompleted() {
      // Given
      DevelopmentPlan plan = DevelopmentPlan.builder().id(TEST_PLAN_ID).status("COMPLETED").build();

      CreateDevelopmentPlanCommand command = new CreateDevelopmentPlanCommand();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> developmentPlanAppService.updatePlan(TEST_PLAN_ID, command));
      assertThat(exception.getMessage()).contains("已完成的规划不能修改");
    }
  }

  @Nested
  @DisplayName("删除发展规划测试")
  class DeletePlanTests {

    @Test
    @DisplayName("应该成功删除发展规划")
    void deletePlan_shouldSuccess() {
      // Given
      DevelopmentPlan plan =
          DevelopmentPlan.builder().id(TEST_PLAN_ID).planNo("PLAN001").status("DRAFT").build();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);
      doNothing().when(milestoneRepository).deleteByPlanId(TEST_PLAN_ID);
      when(planRepository.softDelete(TEST_PLAN_ID)).thenReturn(true);

      // When
      developmentPlanAppService.deletePlan(TEST_PLAN_ID);

      // Then
      verify(milestoneRepository).deleteByPlanId(TEST_PLAN_ID);
      verify(planRepository).softDelete(TEST_PLAN_ID);
    }

    @Test
    @DisplayName("执行中的规划不能删除")
    void deletePlan_shouldFail_whenActive() {
      // Given
      DevelopmentPlan plan = DevelopmentPlan.builder().id(TEST_PLAN_ID).status("ACTIVE").build();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> developmentPlanAppService.deletePlan(TEST_PLAN_ID));
      assertThat(exception.getMessage()).contains("执行中的规划不能删除");
    }
  }

  @Nested
  @DisplayName("提交规划测试")
  class SubmitPlanTests {

    @Test
    @DisplayName("应该成功提交规划")
    void submitPlan_shouldSuccess() {
      // Given
      DevelopmentPlan plan =
          DevelopmentPlan.builder().id(TEST_PLAN_ID).planNo("PLAN001").status("DRAFT").build();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);
      when(planRepository.updateById(any(DevelopmentPlan.class))).thenReturn(true);

      // When
      developmentPlanAppService.submitPlan(TEST_PLAN_ID);

      // Then
      assertThat(plan.getStatus()).isEqualTo("ACTIVE");
      verify(planRepository).updateById(plan);
    }

    @Test
    @DisplayName("只能提交草稿状态的规划")
    void submitPlan_shouldFail_whenNotDraft() {
      // Given
      DevelopmentPlan plan = DevelopmentPlan.builder().id(TEST_PLAN_ID).status("ACTIVE").build();

      when(planRepository.getByIdOrThrow(eq(TEST_PLAN_ID), anyString())).thenReturn(plan);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> developmentPlanAppService.submitPlan(TEST_PLAN_ID));
      assertThat(exception.getMessage()).contains("只能提交草稿状态");
    }
  }

  @Nested
  @DisplayName("更新里程碑状态测试")
  class UpdateMilestoneStatusTests {

    @Test
    @DisplayName("应该成功更新里程碑状态")
    void updateMilestoneStatus_shouldSuccess() {
      // Given
      DevelopmentMilestone milestone =
          DevelopmentMilestone.builder()
              .id(TEST_MILESTONE_ID)
              .planId(TEST_PLAN_ID)
              .status("PENDING")
              .build();

      when(milestoneRepository.getById(TEST_MILESTONE_ID)).thenReturn(milestone);
      when(milestoneRepository.updateById(any(DevelopmentMilestone.class))).thenReturn(true);
      when(milestoneRepository.countCompleted(TEST_PLAN_ID)).thenReturn(1);
      when(milestoneRepository.countTotal(TEST_PLAN_ID)).thenReturn(2);
      when(planRepository.getById(TEST_PLAN_ID)).thenReturn(new DevelopmentPlan());
      when(planRepository.updateById(any(DevelopmentPlan.class))).thenReturn(true);

      // When
      developmentPlanAppService.updateMilestoneStatus(TEST_MILESTONE_ID, "COMPLETED", "已完成");

      // Then
      assertThat(milestone.getStatus()).isEqualTo("COMPLETED");
      assertThat(milestone.getCompletedDate()).isNotNull();
      verify(milestoneRepository).updateById(milestone);
    }

    @Test
    @DisplayName("里程碑不存在应该失败")
    void updateMilestoneStatus_shouldFail_whenNotFound() {
      // Given
      when(milestoneRepository.getById(TEST_MILESTONE_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  developmentPlanAppService.updateMilestoneStatus(
                      TEST_MILESTONE_ID, "COMPLETED", ""));
      assertThat(exception.getMessage()).contains("里程碑不存在");
    }
  }
}
