package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.hr.command.CreateTrainingNoticeCommand;
import com.lawfirm.application.hr.dto.TrainingCompletionDTO;
import com.lawfirm.application.hr.dto.TrainingNoticeDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Training;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import com.lawfirm.domain.hr.repository.TrainingRecordRepository;
import com.lawfirm.domain.hr.repository.TrainingRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TrainingNoticeAppService 单元测试
 * 测试培训通知服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TrainingNoticeAppService 培训通知服务测试")
class TrainingNoticeAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_TRAINING_ID = 100L;
    private static final Long TEST_RECORD_ID = 200L;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TrainingNoticeAppService trainingNoticeAppService;

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
    @DisplayName("创建培训通知测试")
    class CreateNoticeTests {

        @Test
        @DisplayName("应该成功创建培训通知")
        void createNotice_shouldSuccess() throws JsonProcessingException {
            // Given
            CreateTrainingNoticeCommand command = new CreateTrainingNoticeCommand();
            command.setTitle("新员工培训");
            command.setContent("培训内容");

            when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");
            when(trainingRepository.save(any(Training.class))).thenReturn(true);
            when(trainingRecordRepository.countCompletedByTrainingId(any())).thenReturn(0);

            // When
            TrainingNoticeDTO result = trainingNoticeAppService.createNotice(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("新员工培训");
            assertThat(result.getStatus()).isEqualTo("PUBLISHED");
            verify(trainingRepository).save(any(Training.class));
        }

        @Test
        @DisplayName("附件序列化失败应该抛出异常")
        void createNotice_shouldFail_whenSerializeFails() throws JsonProcessingException {
            // Given
            CreateTrainingNoticeCommand command = new CreateTrainingNoticeCommand();
            command.setTitle("培训");
            command.setAttachments(Collections.singletonList(new CreateTrainingNoticeCommand.AttachmentCommand()));

            when(objectMapper.writeValueAsString(anyList()))
                    .thenThrow(new JsonProcessingException("Serialization error") {});

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> trainingNoticeAppService.createNotice(command));
            assertThat(exception.getMessage()).contains("序列化失败");
        }
    }

    @Nested
    @DisplayName("查询培训通知测试")
    class QueryNoticeTests {

        @Test
        @DisplayName("应该成功分页查询培训通知")
        void listNotices_shouldSuccess() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .title("培训通知1")
                    .status("PUBLISHED")
                    .build();

            @SuppressWarnings("unchecked")
            Page<Training> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(training));
            page.setTotal(1L);

            when(trainingRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            when(userRepository.count(any(LambdaQueryWrapper.class))).thenReturn(10L);
            when(trainingRecordRepository.countCompletedByTrainingId(any())).thenReturn(0);
            when(trainingRecordRepository.findByTrainingIdAndEmployeeId(any(), any())).thenReturn(null);

            // When
            PageResult<TrainingNoticeDTO> result = trainingNoticeAppService.listNotices(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTitle()).isEqualTo("培训通知1");
        }

        @Test
        @DisplayName("应该成功获取通知详情")
        void getNoticeById_shouldSuccess() {
            // Given
            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .title("培训通知1")
                    .status("PUBLISHED")
                    .build();

            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(training);
            when(userRepository.count(any(LambdaQueryWrapper.class))).thenReturn(10L);
            when(trainingRecordRepository.countCompletedByTrainingId(TEST_TRAINING_ID)).thenReturn(5);
            when(trainingRecordRepository.findByTrainingIdAndEmployeeId(TEST_TRAINING_ID, TEST_USER_ID))
                    .thenReturn(null);

            // When
            TrainingNoticeDTO result = trainingNoticeAppService.getNoticeById(TEST_TRAINING_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("培训通知1");
            assertThat(result.getCompletedCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("通知不存在应该失败")
        void getNoticeById_shouldFail_whenNotFound() {
            // Given
            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> trainingNoticeAppService.getNoticeById(TEST_TRAINING_ID));
            assertThat(exception.getMessage()).contains("培训通知不存在");
        }
    }

    @Nested
    @DisplayName("完成培训测试")
    class CompleteTrainingTests {

        @Test
        @DisplayName("应该成功完成培训")
        void completeTraining_shouldSuccess() {
            // Given
            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .title("培训通知1")
                    .build();

            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(training);
            when(trainingRecordRepository.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(trainingRecordRepository.save(any(TrainingRecord.class))).thenReturn(true);

            // When
            trainingNoticeAppService.completeTraining(TEST_TRAINING_ID, "http://certificate.pdf", "合格证");

            // Then
            verify(trainingRecordRepository).save(any(TrainingRecord.class));
        }

        @Test
        @DisplayName("应该更新已存在的完成记录")
        void completeTraining_shouldUpdate_whenRecordExists() {
            // Given
            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .build();

            TrainingRecord existingRecord = TrainingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .status("COMPLETED")
                    .build();

            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(training);
            when(trainingRecordRepository.getOne(any(LambdaQueryWrapper.class))).thenReturn(existingRecord);
            when(trainingRecordRepository.updateById(any(TrainingRecord.class))).thenReturn(true);

            // When
            trainingNoticeAppService.completeTraining(TEST_TRAINING_ID, "http://new-cert.pdf", "新合格证");

            // Then
            assertThat(existingRecord.getCertificateUrl()).isEqualTo("http://new-cert.pdf");
            assertThat(existingRecord.getRemarks()).isEqualTo("新合格证");
            verify(trainingRecordRepository).updateById(existingRecord);
        }

        @Test
        @DisplayName("培训不存在应该失败")
        void completeTraining_shouldFail_whenTrainingNotFound() {
            // Given
            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> trainingNoticeAppService.completeTraining(TEST_TRAINING_ID, "url", "name"));
            assertThat(exception.getMessage()).contains("培训通知不存在");
        }
    }

    @Nested
    @DisplayName("删除培训通知测试")
    class DeleteNoticeTests {

        @Test
        @DisplayName("应该成功删除培训通知")
        void deleteNotice_shouldSuccess() {
            // Given
            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .title("待删除培训")
                    .build();

            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(training);
            when(trainingRecordRepository.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
            when(trainingRepository.removeById(TEST_TRAINING_ID)).thenReturn(true);

            // When
            trainingNoticeAppService.deleteNotice(TEST_TRAINING_ID);

            // Then
            verify(trainingRecordRepository).remove(any(LambdaQueryWrapper.class));
            verify(trainingRepository).removeById(TEST_TRAINING_ID);
        }

        @Test
        @DisplayName("通知不存在应该失败")
        void deleteNotice_shouldFail_whenNotFound() {
            // Given
            when(trainingRepository.getById(TEST_TRAINING_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> trainingNoticeAppService.deleteNotice(TEST_TRAINING_ID));
            assertThat(exception.getMessage()).contains("培训通知不存在");
        }
    }

    @Nested
    @DisplayName("查询完成情况测试")
    class QueryCompletionsTests {

        @Test
        @DisplayName("应该成功查询完成情况列表")
        void listCompletions_shouldSuccess() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            TrainingRecord record = TrainingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .trainingId(TEST_TRAINING_ID)
                    .employeeId(TEST_USER_ID)
                    .status("COMPLETED")
                    .certificateUrl("http://cert.pdf")
                    .build();

            Training training = Training.builder()
                    .id(TEST_TRAINING_ID)
                    .title("培训1")
                    .build();

            User user = new User();
            user.setId(TEST_USER_ID);
            user.setRealName("测试用户");

            @SuppressWarnings("unchecked")
            Page<TrainingRecord> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(record));
            page.setTotal(1L);

            when(trainingRecordRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            when(trainingRepository.listByIds(anyList())).thenReturn(Collections.singletonList(training));
            when(userRepository.listByIds(anyList())).thenReturn(Collections.singletonList(user));
            when(departmentRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

            // When
            PageResult<TrainingCompletionDTO> result = trainingNoticeAppService.listCompletions(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getCertificateUrl()).isEqualTo("http://cert.pdf");
        }

        @Test
        @DisplayName("空记录应该返回空列表")
        void listCompletions_shouldReturnEmpty_whenNoRecords() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            @SuppressWarnings("unchecked")
            Page<TrainingRecord> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0L);

            when(trainingRecordRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // When
            PageResult<TrainingCompletionDTO> result = trainingNoticeAppService.listCompletions(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).isEmpty();
        }
    }
}
