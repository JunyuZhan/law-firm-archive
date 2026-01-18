package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateIndicatorCommand;
import com.lawfirm.application.hr.command.CreatePerformanceTaskCommand;
import com.lawfirm.application.hr.command.SubmitEvaluationCommand;
import com.lawfirm.application.hr.dto.PerformanceEvaluationDTO;
import com.lawfirm.application.hr.dto.PerformanceIndicatorDTO;
import com.lawfirm.application.hr.dto.PerformanceTaskDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.*;
import com.lawfirm.domain.hr.repository.*;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PerformanceAppService 单元测试
 * 测试绩效考核服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PerformanceAppService 绩效考核服务测试")
class PerformanceAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_TASK_ID = 100L;
    private static final Long TEST_INDICATOR_ID = 200L;
    private static final Long TEST_EVALUATION_ID = 300L;
    private static final Long TEST_EMPLOYEE_ID = 400L;

    @Mock
    private PerformanceTaskRepository taskRepository;

    @Mock
    private PerformanceIndicatorRepository indicatorRepository;

    @Mock
    private PerformanceEvaluationRepository evaluationRepository;

    @Mock
    private PerformanceScoreRepository scoreRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PerformanceAppService performanceAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("考核任务管理测试")
    class TaskManagementTests {

        @Test
        @DisplayName("应该成功创建考核任务")
        void createTask_shouldSuccess() {
            // Given
            CreatePerformanceTaskCommand command = new CreatePerformanceTaskCommand();
            command.setName("2024年Q1考核");
            command.setPeriodType("QUARTERLY");
            command.setYear(2024);
            command.setPeriod(1);
            command.setStartDate(LocalDate.now());
            command.setEndDate(LocalDate.now().plusMonths(3));

            when(taskRepository.save(any(PerformanceTask.class))).thenAnswer(invocation -> {
                PerformanceTask task = invocation.getArgument(0);
                task.setId(TEST_TASK_ID);
                return true;
            });

            // When
            PerformanceTaskDTO result = performanceAppService.createTask(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("2024年Q1考核");
            assertThat(result.getStatus()).isEqualTo("DRAFT");
            verify(taskRepository).save(any(PerformanceTask.class));
        }

        @Test
        @DisplayName("应该成功启动考核任务")
        void startTask_shouldSuccess() {
            // Given
            PerformanceTask task = PerformanceTask.builder()
                    .id(TEST_TASK_ID)
                    .name("考核任务")
                    .status("DRAFT")
                    .build();

            when(taskRepository.getById(TEST_TASK_ID)).thenReturn(task);
            when(taskRepository.updateById(any(PerformanceTask.class))).thenReturn(true);

            // When
            performanceAppService.startTask(TEST_TASK_ID);

            // Then
            assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
            verify(taskRepository).updateById(task);
        }

        @Test
        @DisplayName("非草稿状态不能启动")
        void startTask_shouldFail_whenNotDraft() {
            // Given
            PerformanceTask task = PerformanceTask.builder()
                    .id(TEST_TASK_ID)
                    .status("IN_PROGRESS")
                    .build();

            when(taskRepository.getById(TEST_TASK_ID)).thenReturn(task);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> performanceAppService.startTask(TEST_TASK_ID));
            assertThat(exception.getMessage()).contains("只有草稿状态");
        }

        @Test
        @DisplayName("应该成功完成考核任务")
        void completeTask_shouldSuccess() {
            // Given
            PerformanceTask task = PerformanceTask.builder()
                    .id(TEST_TASK_ID)
                    .status("IN_PROGRESS")
                    .build();

            when(taskRepository.getById(TEST_TASK_ID)).thenReturn(task);
            when(taskRepository.updateById(any(PerformanceTask.class))).thenReturn(true);

            // When
            performanceAppService.completeTask(TEST_TASK_ID);

            // Then
            assertThat(task.getStatus()).isEqualTo("COMPLETED");
            verify(taskRepository).updateById(task);
        }

        @Test
        @DisplayName("已完成的任务不能再次完成")
        void completeTask_shouldFail_whenCompleted() {
            // Given
            PerformanceTask task = PerformanceTask.builder()
                    .id(TEST_TASK_ID)
                    .status("COMPLETED")
                    .build();

            when(taskRepository.getById(TEST_TASK_ID)).thenReturn(task);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> performanceAppService.completeTask(TEST_TASK_ID));
            assertThat(exception.getMessage()).contains("任务已完成");
        }
    }

    @Nested
    @DisplayName("考核指标管理测试")
    class IndicatorManagementTests {

        @Test
        @DisplayName("应该成功创建考核指标")
        void createIndicator_shouldSuccess() {
            // Given
            CreateIndicatorCommand command = new CreateIndicatorCommand();
            command.setName("工作质量");
            command.setCode("QUALITY");
            command.setCategory("工作表现");
            command.setWeight(new BigDecimal("30"));
            command.setMaxScore(100);

            when(indicatorRepository.save(any(PerformanceIndicator.class))).thenAnswer(invocation -> {
                PerformanceIndicator indicator = invocation.getArgument(0);
                indicator.setId(TEST_INDICATOR_ID);
                return true;
            });

            // When
            PerformanceIndicatorDTO result = performanceAppService.createIndicator(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("工作质量");
            verify(indicatorRepository).save(any(PerformanceIndicator.class));
        }

        @Test
        @DisplayName("应该成功更新考核指标")
        void updateIndicator_shouldSuccess() {
            // Given
            PerformanceIndicator indicator = PerformanceIndicator.builder()
                    .id(TEST_INDICATOR_ID)
                    .name("原名称")
                    .category("WORK")
                    .applicableRole("ALL")
                    .build();

            CreateIndicatorCommand command = new CreateIndicatorCommand();
            command.setName("新名称");
            command.setCategory("WORK");
            command.setApplicableRole("ALL");

            when(indicatorRepository.getById(TEST_INDICATOR_ID)).thenReturn(indicator);
            when(indicatorRepository.updateById(any(PerformanceIndicator.class))).thenReturn(true);

            // When
            PerformanceIndicatorDTO result = performanceAppService.updateIndicator(TEST_INDICATOR_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(indicator.getName()).isEqualTo("新名称");
            verify(indicatorRepository).updateById(indicator);
        }

        @Test
        @DisplayName("指标不存在应该失败")
        void updateIndicator_shouldFail_whenNotFound() {
            // Given
            CreateIndicatorCommand command = new CreateIndicatorCommand();

            when(indicatorRepository.getById(TEST_INDICATOR_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> performanceAppService.updateIndicator(TEST_INDICATOR_ID, command));
            assertThat(exception.getMessage()).contains("考核指标不存在");
        }
    }

    @Nested
    @DisplayName("绩效评价测试")
    class EvaluationTests {

        @Test
        @DisplayName("应该成功提交绩效评价")
        void submitEvaluation_shouldSuccess() {
            // Given
            SubmitEvaluationCommand command = new SubmitEvaluationCommand();
            command.setTaskId(TEST_TASK_ID);
            command.setEmployeeId(TEST_USER_ID); // 自评必须是评价自己
            command.setEvaluationType("SELF");

            SubmitEvaluationCommand.ScoreItem scoreItem = new SubmitEvaluationCommand.ScoreItem();
            scoreItem.setIndicatorId(TEST_INDICATOR_ID);
            scoreItem.setScore(new BigDecimal("85"));
            command.setScores(Collections.singletonList(scoreItem));

            PerformanceIndicator indicator = PerformanceIndicator.builder()
                    .id(TEST_INDICATOR_ID)
                    .weight(new BigDecimal("100"))
                    .maxScore(100)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.getUserId()).thenReturn(TEST_USER_ID);
            securityUtilsMock.when(() -> SecurityUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.getById(TEST_USER_ID)).thenReturn(new User());
            when(indicatorRepository.findAllActive()).thenReturn(Collections.singletonList(indicator));
            when(evaluationRepository.findByTaskEmployeeAndType(any(), any(), any())).thenReturn(null);
            when(evaluationRepository.save(any(PerformanceEvaluation.class))).thenAnswer(invocation -> {
                PerformanceEvaluation eval = invocation.getArgument(0);
                eval.setId(TEST_EVALUATION_ID);
                return true;
            });
            when(scoreRepository.saveBatch(anyList())).thenReturn(true);

            // When
            PerformanceEvaluationDTO result = performanceAppService.submitEvaluation(command);

            // Then
            assertThat(result).isNotNull();
            verify(evaluationRepository).save(any(PerformanceEvaluation.class));
            verify(scoreRepository).saveBatch(anyList());
        }

        @Test
        @DisplayName("已完成评价不能重复提交")
        void submitEvaluation_shouldFail_whenAlreadyCompleted() {
            // Given
            SubmitEvaluationCommand command = new SubmitEvaluationCommand();
            command.setTaskId(TEST_TASK_ID);
            command.setEmployeeId(TEST_USER_ID); // 自评必须是评价自己
            command.setEvaluationType("SELF");

            PerformanceEvaluation existing = PerformanceEvaluation.builder()
                    .id(TEST_EVALUATION_ID)
                    .status("COMPLETED")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.getUserId()).thenReturn(TEST_USER_ID);
            securityUtilsMock.when(() -> SecurityUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
            when(userRepository.getById(TEST_USER_ID)).thenReturn(new User());
            when(evaluationRepository.findByTaskEmployeeAndType(any(), any(), any())).thenReturn(existing);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> performanceAppService.submitEvaluation(command));
            assertThat(exception.getMessage()).contains("不能重复提交");
        }
    }

    @Nested
    @DisplayName("查询测试")
    class QueryTests {

        @Test
        @DisplayName("应该成功分页查询考核任务")
        void listTasks_shouldSuccess() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            PerformanceTask task = PerformanceTask.builder()
                    .id(TEST_TASK_ID)
                    .name("考核任务1")
                    .periodType("QUARTERLY")
                    .status("DRAFT")
                    .build();

            @SuppressWarnings("unchecked")
            Page<PerformanceTask> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(task));
            page.setTotal(1L);

            when(taskRepository.findPage(any(Page.class), any(), any(), any())).thenReturn(page);

            // When
            PageResult<PerformanceTaskDTO> result = performanceAppService.listTasks(query, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("考核任务1");
        }

        @Test
        @DisplayName("应该成功查询考核指标列表")
        void listIndicators_shouldSuccess() {
            // Given
            PerformanceIndicator indicator = PerformanceIndicator.builder()
                    .id(TEST_INDICATOR_ID)
                    .name("指标1")
                    .category("WORK")
                    .applicableRole("ALL")
                    .build();

            when(indicatorRepository.findAllActive()).thenReturn(Collections.singletonList(indicator));

            // When
            List<PerformanceIndicatorDTO> result = performanceAppService.listIndicators(null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("指标1");
        }
    }
}
