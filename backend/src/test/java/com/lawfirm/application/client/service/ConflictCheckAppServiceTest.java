package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.ApplyExemptionCommand;
import com.lawfirm.application.client.command.CreateConflictCheckCommand;
import com.lawfirm.application.client.dto.ConflictCheckDTO;
import com.lawfirm.application.client.dto.ConflictCheckQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.constant.ConflictStatus;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ConflictCheck;
import com.lawfirm.domain.client.entity.ConflictCheckItem;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ConflictCheckRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckItemMapper;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.domain.system.entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConflictCheckAppService 单元测试
 * 测试利冲审查服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConflictCheckAppService 利冲审查服务测试")
class ConflictCheckAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CHECK_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_CLIENT_ID = 300L;

    @Mock
    private ConflictCheckRepository conflictCheckRepository;

    @Mock
    private ConflictCheckMapper conflictCheckMapper;

    @Mock
    private ConflictCheckItemMapper conflictCheckItemMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private ApprovalService approvalService;

    @Mock
    private ApproverService approverService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ConflictCheckAppService conflictCheckAppService;

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
    @DisplayName("创建利冲检查测试")
    class CreateConflictCheckTests {

        @Test
        @DisplayName("应该成功创建无冲突的利冲检查")
        void createConflictCheck_shouldSuccess_whenNoConflict() {
            // Given
            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .clientId(TEST_CLIENT_ID)
                    .name("案件1")
                    .build();

            CreateConflictCheckCommand command = new CreateConflictCheckCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setClientId(TEST_CLIENT_ID);
            CreateConflictCheckCommand.PartyCommand party = new CreateConflictCheckCommand.PartyCommand();
            party.setPartyName("对方当事人");
            party.setPartyType("OPPOSING");
            command.setParties(Collections.singletonList(party));

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            ConflictCheck savedCheck = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .checkNo("CC001")
                    .status(ConflictStatus.PENDING)
                    .build();
            when(conflictCheckRepository.save(any(ConflictCheck.class))).thenAnswer(invocation -> {
                ConflictCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return check;
            });
            when(clientRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(matterRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(conflictCheckItemMapper.insert(any(ConflictCheckItem.class))).thenAnswer(invocation -> {
                ConflictCheckItem item = invocation.getArgument(0);
                if (item.getId() == null) {
                    item.setId(1L);
                }
                return 1;
            });
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);

            // When
            ConflictCheckDTO result = conflictCheckAppService.createConflictCheck(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ConflictStatus.PASSED);
            verify(conflictCheckRepository).save(any(ConflictCheck.class));
        }

        @Test
        @DisplayName("应该成功创建有冲突的利冲检查")
        void createConflictCheck_shouldSuccess_whenHasConflict() {
            // Given
            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .clientId(TEST_CLIENT_ID)
                    .name("案件1")
                    .build();

            Client conflictingClient = Client.builder()
                    .id(400L)
                    .name("对方当事人")
                    .build();

            CreateConflictCheckCommand command = new CreateConflictCheckCommand();
            command.setMatterId(TEST_MATTER_ID);
            CreateConflictCheckCommand.PartyCommand party = new CreateConflictCheckCommand.PartyCommand();
            party.setPartyName("对方当事人");
            party.setPartyType("OPPOSING");
            command.setParties(Collections.singletonList(party));

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(conflictCheckRepository.save(any(ConflictCheck.class))).thenAnswer(invocation -> {
                ConflictCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return check;
            });
            when(clientRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(conflictingClient));
            when(conflictCheckItemMapper.insert(any(ConflictCheckItem.class))).thenAnswer(invocation -> {
                ConflictCheckItem item = invocation.getArgument(0);
                if (item.getId() == null) {
                    item.setId(1L);
                }
                return 1;
            });
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);
            when(approverService.findConflictCheckApprover()).thenReturn(10L);
            lenient().doNothing().when(approvalService).createApproval(anyString(), anyLong(), anyString(), 
                    anyString(), anyLong(), anyString(), anyString(), any());

            // When
            ConflictCheckDTO result = conflictCheckAppService.createConflictCheck(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ConflictStatus.CONFLICT);
            verify(approvalService).createApproval(anyString(), anyLong(), anyString(), 
                    anyString(), anyLong(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("快速利冲检索测试")
    class QuickConflictCheckTests {

        @Test
        @DisplayName("应该成功进行快速利冲检索-无冲突")
        void quickConflictCheck_shouldReturnNoConflict() {
            // Given
            when(clientRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // When
            ConflictCheckAppService.QuickConflictCheckResult result = 
                    conflictCheckAppService.quickConflictCheck("客户A", "对方当事人");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.hasConflict()).isFalse();
            assertThat(result.riskLevel()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("应该成功进行快速利冲检索-精确匹配")
        void quickConflictCheck_shouldReturnExactMatch() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientNo("CL001")
                    .name("对方当事人")
                    .clientType("ENTERPRISE")
                    .build();

            when(clientRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(client));

            // When
            ConflictCheckAppService.QuickConflictCheckResult result = 
                    conflictCheckAppService.quickConflictCheck("客户A", "对方当事人");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.hasConflict()).isTrue();
            assertThat(result.riskLevel()).isEqualTo("HIGH");
            assertThat(result.candidates()).hasSize(1);
            assertThat(result.candidates().get(0).matchScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("输入名称过短应该返回空结果")
        void quickConflictCheck_shouldReturnEmpty_whenNameTooShort() {
            // When
            ConflictCheckAppService.QuickConflictCheckResult result = 
                    conflictCheckAppService.quickConflictCheck("客户A", "A");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.hasConflict()).isFalse();
            assertThat(result.riskLevel()).isEqualTo("NONE");
            assertThat(result.riskSummary()).contains("输入名称过短");
        }
    }

    @Nested
    @DisplayName("手动申请利冲审查测试")
    class ApplyConflictCheckTests {

        @Test
        @DisplayName("应该成功手动申请利冲审查")
        void applyConflictCheck_shouldSuccess() {
            // Given
            when(conflictCheckRepository.save(any(ConflictCheck.class))).thenAnswer(invocation -> {
                ConflictCheck check = invocation.getArgument(0);
                check.setId(TEST_CHECK_ID);
                return check;
            });
            when(clientRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(matterRepository.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(conflictCheckItemMapper.insert(any(ConflictCheckItem.class))).thenReturn(1);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);

            // When
            ConflictCheckDTO result = conflictCheckAppService.applyConflictCheck(
                    "客户A", "对方当事人", "案件1", "MANUAL", "备注");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCheckNo()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ConflictStatus.PASSED);
        }
    }

    @Nested
    @DisplayName("审核测试")
    class ApprovalTests {

        @Test
        @DisplayName("应该成功审核通过")
        void approve_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.CONFLICT)
                    .matterId(TEST_MATTER_ID)
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);

            // When
            conflictCheckAppService.approve(TEST_CHECK_ID, "审核通过");

            // Then
            assertThat(check.getStatus()).isEqualTo(ConflictStatus.WAIVED);
            assertThat(check.getReviewerId()).isEqualTo(TEST_USER_ID);
            assertThat(check.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("当前状态不允许审核应该失败")
        void approve_shouldFail_whenInvalidStatus() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.PASSED) // 已通过，不能审核
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> conflictCheckAppService.approve(TEST_CHECK_ID, "审核通过"));
            assertThat(exception.getMessage()).contains("不允许审核");
        }

        @Test
        @DisplayName("应该成功审核拒绝")
        void reject_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.CONFLICT)
                    .matterId(TEST_MATTER_ID)
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);

            // When
            conflictCheckAppService.reject(TEST_CHECK_ID, "审核拒绝");

            // Then
            assertThat(check.getStatus()).isEqualTo(ConflictStatus.REJECTED);
            assertThat(check.getReviewComment()).isEqualTo("审核拒绝");
        }
    }

    @Nested
    @DisplayName("豁免申请测试")
    class ExemptionTests {

        @Test
        @DisplayName("应该成功申请豁免")
        void applyExemption_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.CONFLICT)
                    .checkNo("CC001")
                    .build();

            ApplyExemptionCommand command = new ApplyExemptionCommand();
            command.setConflictCheckId(TEST_CHECK_ID);
            command.setExemptionReason("特殊原因");
            command.setExemptionDescription("详细说明");

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            when(approverService.findDefaultApprover()).thenReturn(10L);
            lenient().doNothing().when(approvalService).createApproval(anyString(), anyLong(), anyString(), 
                    anyString(), anyLong(), anyString(), anyString(), any());
            when(conflictCheckItemMapper.selectByCheckId(TEST_CHECK_ID)).thenReturn(Collections.emptyList());

            // When
            ConflictCheckDTO result = conflictCheckAppService.applyExemption(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(check.getStatus()).isEqualTo(ConflictStatus.EXEMPTION_PENDING);
        }

        @Test
        @DisplayName("无冲突的检查不能申请豁免")
        void applyExemption_shouldFail_whenNoConflict() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.PASSED) // 无冲突
                    .build();

            ApplyExemptionCommand command = new ApplyExemptionCommand();
            command.setConflictCheckId(TEST_CHECK_ID);

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> conflictCheckAppService.applyExemption(command));
            assertThat(exception.getMessage()).contains("存在冲突");
        }

        @Test
        @DisplayName("应该成功审批豁免通过")
        void approveExemption_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.EXEMPTION_PENDING)
                    .matterId(TEST_MATTER_ID)
                    .reviewComment("原评论")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);
            when(conflictCheckItemMapper.selectByCheckId(TEST_CHECK_ID)).thenReturn(Collections.emptyList());

            // When
            conflictCheckAppService.approveExemption(TEST_CHECK_ID, "审批通过");

            // Then
            assertThat(check.getStatus()).isEqualTo(ConflictStatus.WAIVED);
            assertThat(check.getReviewComment()).contains("审批意见");
        }

        @Test
        @DisplayName("应该成功审批豁免拒绝")
        void rejectExemption_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .status(ConflictStatus.EXEMPTION_PENDING)
                    .matterId(TEST_MATTER_ID)
                    .reviewComment("原评论")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            
            com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper checkBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper.class);
            when(conflictCheckRepository.getBaseMapper()).thenReturn(checkBaseMapper);
            lenient().when(checkBaseMapper.updateById(any(ConflictCheck.class))).thenReturn(1);
            
            lenient().when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.MatterMapper.class);
            lenient().when(matterRepository.getBaseMapper()).thenReturn(matterBaseMapper);
            lenient().when(matterBaseMapper.updateById(any(Matter.class))).thenReturn(1);

            // When
            conflictCheckAppService.rejectExemption(TEST_CHECK_ID, "拒绝原因");

            // Then
            assertThat(check.getStatus()).isEqualTo(ConflictStatus.CONFLICT);
            assertThat(check.getReviewComment()).contains("拒绝原因");
        }
    }

    @Nested
    @DisplayName("查询利冲检查测试")
    class QueryConflictCheckTests {

        @Test
        @DisplayName("应该成功查询利冲检查列表")
        void listConflictChecks_shouldSuccess() {
            // Given
            ConflictCheckQueryDTO query = new ConflictCheckQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .checkNo("CC001")
                    .status(ConflictStatus.PENDING)
                    .build();

            Page<ConflictCheck> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(check));
            page.setTotal(1);

            when(conflictCheckRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            lenient().when(userMapper.selectBatchIds(anySet())).thenReturn(Collections.emptyList());
            lenient().when(matterRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

            // When
            PageResult<ConflictCheckDTO> result = conflictCheckAppService.listConflictChecks(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getCheckNo()).isEqualTo("CC001");
        }

        @Test
        @DisplayName("应该成功获取利冲检查详情")
        void getConflictCheckById_shouldSuccess() {
            // Given
            ConflictCheck check = ConflictCheck.builder()
                    .id(TEST_CHECK_ID)
                    .checkNo("CC001")
                    .build();

            ConflictCheckItem item = ConflictCheckItem.builder()
                    .id(1L)
                    .checkId(TEST_CHECK_ID)
                    .partyName("当事人")
                    .build();

            when(conflictCheckRepository.getByIdOrThrow(eq(TEST_CHECK_ID), anyString())).thenReturn(check);
            lenient().when(userMapper.selectById(anyLong())).thenReturn(null);
            lenient().when(matterRepository.findById(anyLong())).thenReturn(null);
            when(conflictCheckItemMapper.selectByCheckId(TEST_CHECK_ID)).thenReturn(Collections.singletonList(item));

            // When
            ConflictCheckDTO result = conflictCheckAppService.getConflictCheckById(TEST_CHECK_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCheckNo()).isEqualTo("CC001");
            assertThat(result.getItems()).hasSize(1);
        }
    }
}
