package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateQualityIssueCommand;
import com.lawfirm.application.knowledge.dto.QualityIssueDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.QualityIssue;
import com.lawfirm.domain.knowledge.repository.QualityIssueRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityIssueMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QualityIssueAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QualityIssueAppService 质量问题服务测试")
class QualityIssueAppServiceTest {

    private static final Long TEST_ISSUE_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_CHECK_ID = 300L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long RESPONSIBLE_USER_ID = 2L;

    @Mock
    private QualityIssueRepository issueRepository;

    @Mock
    private QualityIssueMapper issueMapper;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QualityIssueAppService issueAppService;

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
    @DisplayName("创建问题测试")
    class CreateIssueTests {

        @Test
        @DisplayName("应该成功创建问题")
        void createIssue_shouldSuccess() {
            // Given
            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试项目")
                    .build();

            CreateQualityIssueCommand command = new CreateQualityIssueCommand();
            command.setCheckId(TEST_CHECK_ID);
            command.setMatterId(TEST_MATTER_ID);
            command.setIssueType(QualityIssue.TYPE_MAJOR);
            command.setIssueDescription("问题描述");
            command.setResponsibleUserId(RESPONSIBLE_USER_ID);
            command.setPriority(QualityIssue.PRIORITY_HIGH);
            command.setDueDate(LocalDate.now().plusDays(7));

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(issueRepository.save(any(QualityIssue.class))).thenAnswer(invocation -> {
                QualityIssue issue = invocation.getArgument(0);
                issue.setId(TEST_ISSUE_ID);
                return true;
            });

            Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            User responsibleUser = User.builder().id(RESPONSIBLE_USER_ID).realName("责任人").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);
            when(userRepository.getById(RESPONSIBLE_USER_ID)).thenReturn(responsibleUser);

            // When
            QualityIssueDTO result = issueAppService.createIssue(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
            assertThat(result.getIssueType()).isEqualTo(QualityIssue.TYPE_MAJOR);
            assertThat(result.getStatus()).isEqualTo(QualityIssue.STATUS_OPEN);
            verify(issueRepository).save(any(QualityIssue.class));
        }

        @Test
        @DisplayName("应该使用默认优先级当未指定")
        void createIssue_shouldUseDefaultPriority() {
            // Given
            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试项目")
                    .build();

            CreateQualityIssueCommand command = new CreateQualityIssueCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setIssueType(QualityIssue.TYPE_MINOR);
            command.setIssueDescription("问题描述");

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(issueRepository.save(any(QualityIssue.class))).thenAnswer(invocation -> {
                QualityIssue issue = invocation.getArgument(0);
                issue.setId(TEST_ISSUE_ID);
                return true;
            });

            Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);

            // When
            QualityIssueDTO result = issueAppService.createIssue(command);

            // Then
            assertThat(result).isNotNull();
            verify(issueRepository).save(argThat(issue ->
                    QualityIssue.PRIORITY_MEDIUM.equals(issue.getPriority())
            ));
        }

        @Test
        @DisplayName("应该失败当项目不存在")
        void createIssue_shouldFail_whenMatterNotExists() {
            // Given
            CreateQualityIssueCommand command = new CreateQualityIssueCommand();
            command.setMatterId(TEST_MATTER_ID);

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString()))
                    .thenThrow(new BusinessException("项目不存在"));

            // When & Then
            assertThrows(BusinessException.class, () -> issueAppService.createIssue(command));
        }
    }

    @Nested
    @DisplayName("更新问题状态测试")
    class UpdateIssueStatusTests {

        @Test
        @DisplayName("应该成功更新为已解决状态")
        void updateIssueStatus_shouldSuccess_whenResolved() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(QualityIssue.STATUS_OPEN)
                    .build();

            when(issueRepository.getByIdOrThrow(eq(TEST_ISSUE_ID), anyString())).thenReturn(issue);
            when(issueRepository.updateById(any(QualityIssue.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            QualityIssueDTO result = issueAppService.updateIssueStatus(TEST_ISSUE_ID, QualityIssue.STATUS_RESOLVED, "已解决");

            // Then
            assertThat(result).isNotNull();
            assertThat(issue.getStatus()).isEqualTo(QualityIssue.STATUS_RESOLVED);
            assertThat(issue.getResolution()).isEqualTo("已解决");
            assertThat(issue.getResolvedAt()).isNotNull();
            assertThat(issue.getResolvedBy()).isEqualTo(TEST_USER_ID);
            verify(issueRepository).updateById(issue);
        }

        @Test
        @DisplayName("应该成功更新为整改中状态")
        void updateIssueStatus_shouldSuccess_whenInProgress() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(QualityIssue.STATUS_OPEN)
                    .build();

            when(issueRepository.getByIdOrThrow(eq(TEST_ISSUE_ID), anyString())).thenReturn(issue);
            when(issueRepository.updateById(any(QualityIssue.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            QualityIssueDTO result = issueAppService.updateIssueStatus(TEST_ISSUE_ID, QualityIssue.STATUS_IN_PROGRESS, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(issue.getStatus()).isEqualTo(QualityIssue.STATUS_IN_PROGRESS);
            verify(issueRepository).updateById(issue);
        }

        @Test
        @DisplayName("应该成功更新为已关闭状态")
        void updateIssueStatus_shouldSuccess_whenClosed() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(QualityIssue.STATUS_RESOLVED)
                    .build();

            when(issueRepository.getByIdOrThrow(eq(TEST_ISSUE_ID), anyString())).thenReturn(issue);
            when(issueRepository.updateById(any(QualityIssue.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            QualityIssueDTO result = issueAppService.updateIssueStatus(TEST_ISSUE_ID, QualityIssue.STATUS_CLOSED, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(issue.getStatus()).isEqualTo(QualityIssue.STATUS_CLOSED);
            assertThat(issue.getVerifiedAt()).isNotNull();
            assertThat(issue.getVerifiedBy()).isEqualTo(TEST_USER_ID);
            verify(issueRepository).updateById(issue);
        }
    }

    @Nested
    @DisplayName("查询问题测试")
    class QueryIssueTests {

        @Test
        @DisplayName("应该成功获取问题详情")
        void getIssueById_shouldSuccess() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .issueType(QualityIssue.TYPE_MAJOR)
                    .status(QualityIssue.STATUS_OPEN)
                    .priority(QualityIssue.PRIORITY_HIGH)
                    .build();

            when(issueRepository.getByIdOrThrow(eq(TEST_ISSUE_ID), anyString())).thenReturn(issue);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            QualityIssueDTO result = issueAppService.getIssueById(TEST_ISSUE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ISSUE_ID);
        }

        @Test
        @DisplayName("应该成功获取项目的所有问题")
        void getIssuesByMatterId_shouldSuccess() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .build();

            when(issueMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(List.of(issue));

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            List<QualityIssueDTO> result = issueAppService.getIssuesByMatterId(TEST_MATTER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取待整改的问题")
        void getPendingIssues_shouldSuccess() {
            // Given
            QualityIssue issue = QualityIssue.builder()
                    .id(TEST_ISSUE_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(QualityIssue.STATUS_OPEN)
                    .build();

            when(issueMapper.selectPendingIssues()).thenReturn(List.of(issue));

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            List<QualityIssueDTO> result = issueAppService.getPendingIssues();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }
    }
}
