package com.lawfirm.application.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.client.command.ConvertLeadCommand;
import com.lawfirm.application.client.command.CreateFollowUpCommand;
import com.lawfirm.application.client.command.CreateLeadCommand;
import com.lawfirm.application.client.command.UpdateLeadCommand;
import com.lawfirm.application.client.dto.ClientDTO;
import com.lawfirm.application.client.dto.LeadDTO;
import com.lawfirm.application.client.dto.LeadFollowUpDTO;
import com.lawfirm.application.client.dto.LeadQueryDTO;
import com.lawfirm.application.client.dto.LeadStatisticsDTO;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Lead;
import com.lawfirm.domain.client.entity.LeadFollowUp;
import com.lawfirm.domain.client.repository.LeadFollowUpRepository;
import com.lawfirm.domain.client.repository.LeadRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.LeadMapper;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** LeadAppService 单元测试 测试案源管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeadAppService 案源服务测试")
class LeadAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_LEAD_ID = 100L;

  @Mock private LeadRepository leadRepository;

  @Mock private LeadFollowUpRepository leadFollowUpRepository;

  @Mock private LeadMapper leadMapper;

  @Mock private ClientAppService clientAppService;

  @Mock private MatterAppService matterAppService;

  @Mock private UserRepository userRepository;

  @InjectMocks private LeadAppService leadAppService;

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
  @DisplayName("创建案源测试")
  class CreateLeadTests {

    @Test
    @DisplayName("应该成功创建案源")
    void createLead_shouldSuccess() {
      // Given
      CreateLeadCommand command = new CreateLeadCommand();
      command.setLeadName("新案源");
      command.setLeadType("ENTERPRISE");
      command.setContactName("联系人");
      command.setContactPhone("13800138000");
      command.setSourceChannel("WEBSITE");
      command.setPriority("HIGH");

      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.insert(any(Lead.class))).thenReturn(1);

      // When
      LeadDTO result = leadAppService.createLead(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLeadName()).isEqualTo("新案源");
      assertThat(result.getStatus()).isEqualTo("PENDING");
      assertThat(result.getPriority()).isEqualTo("HIGH");
      verify(leadBaseMapper).insert(any(Lead.class));
    }

    @Test
    @DisplayName("应该设置默认优先级")
    void createLead_shouldSetDefaultPriority() {
      // Given
      CreateLeadCommand command = new CreateLeadCommand();
      command.setLeadName("新案源");
      command.setContactPhone("13800138000");
      command.setPriority(null); // 不设置优先级

      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.insert(any(Lead.class))).thenReturn(1);

      // When
      LeadDTO result = leadAppService.createLead(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPriority()).isEqualTo("NORMAL");
    }
  }

  @Nested
  @DisplayName("更新案源测试")
  class UpdateLeadTests {

    @Test
    @DisplayName("应该成功更新案源")
    void updateLead_shouldSuccess() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).leadName("原名称").status("PENDING").build();

      UpdateLeadCommand command = new UpdateLeadCommand();
      command.setLeadName("新名称");
      command.setPriority("HIGH");

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.updateById(any(Lead.class))).thenReturn(1);

      // When
      LeadDTO result = leadAppService.updateLead(TEST_LEAD_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(lead.getLeadName()).isEqualTo("新名称");
      assertThat(lead.getPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("已转化的案源不能修改")
    void updateLead_shouldFail_whenConverted() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("CONVERTED").build();

      UpdateLeadCommand command = new UpdateLeadCommand();
      command.setLeadName("新名称");

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> leadAppService.updateLead(TEST_LEAD_ID, command));
      assertThat(exception.getMessage()).contains("已转化");
    }
  }

  @Nested
  @DisplayName("删除案源测试")
  class DeleteLeadTests {

    @Test
    @DisplayName("应该成功删除案源")
    void deleteLead_shouldSuccess() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("PENDING").build();

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      lenient().when(leadRepository.softDelete(TEST_LEAD_ID)).thenReturn(true);

      // When
      leadAppService.deleteLead(TEST_LEAD_ID);

      // Then
      verify(leadRepository).softDelete(TEST_LEAD_ID);
    }

    @Test
    @DisplayName("已转化的案源不能删除")
    void deleteLead_shouldFail_whenConverted() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("CONVERTED").build();

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> leadAppService.deleteLead(TEST_LEAD_ID));
      assertThat(exception.getMessage()).contains("已转化");
    }
  }

  @Nested
  @DisplayName("案源跟进测试")
  class FollowUpTests {

    @Test
    @DisplayName("应该成功创建跟进记录")
    void createFollowUp_shouldSuccess() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("PENDING").followCount(0).build();

      CreateFollowUpCommand command = new CreateFollowUpCommand();
      command.setLeadId(TEST_LEAD_ID);
      command.setFollowType("PHONE");
      command.setFollowContent("电话沟通");
      command.setFollowResult("有意向");
      command.setNextFollowTime(LocalDateTime.now().plusDays(3));

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      com.lawfirm.infrastructure.persistence.mapper.LeadFollowUpMapper followUpBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadFollowUpMapper.class);
      when(leadFollowUpRepository.getBaseMapper()).thenReturn(followUpBaseMapper);
      when(followUpBaseMapper.insert(any(LeadFollowUp.class))).thenReturn(1);

      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.updateById(any(Lead.class))).thenReturn(1);

      // When
      LeadFollowUpDTO result = leadAppService.createFollowUp(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(lead.getFollowCount()).isEqualTo(1);
      assertThat(lead.getStatus()).isEqualTo("FOLLOWING");
      assertThat(lead.getLastFollowTime()).isNotNull();
    }

    @Test
    @DisplayName("已转化的案源不能跟进")
    void createFollowUp_shouldFail_whenConverted() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("CONVERTED").build();

      CreateFollowUpCommand command = new CreateFollowUpCommand();
      command.setLeadId(TEST_LEAD_ID);

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> leadAppService.createFollowUp(command));
      assertThat(exception.getMessage()).contains("已转化");
    }

    @Test
    @DisplayName("应该成功查询跟进记录")
    void listFollowUps_shouldSuccess() {
      // Given
      LeadFollowUp followUp =
          LeadFollowUp.builder()
              .id(1L)
              .leadId(TEST_LEAD_ID)
              .followType("PHONE")
              .followContent("电话沟通")
              .build();

      when(leadFollowUpRepository.findByLeadId(TEST_LEAD_ID))
          .thenReturn(Collections.singletonList(followUp));

      // When
      List<LeadFollowUpDTO> result = leadAppService.listFollowUps(TEST_LEAD_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getFollowType()).isEqualTo("PHONE");
    }
  }

  @Nested
  @DisplayName("案源转化测试")
  class ConvertLeadTests {

    @Test
    @DisplayName("应该成功转化案源并创建客户")
    void convertLead_shouldSuccess_whenCreateClient() {
      // Given
      Lead lead =
          Lead.builder()
              .id(TEST_LEAD_ID)
              .leadName("新案源")
              .status("PENDING")
              .sourceChannel("WEBSITE")
              .originatorId(TEST_USER_ID)
              .build();

      ConvertLeadCommand command = new ConvertLeadCommand();
      command.setLeadId(TEST_LEAD_ID);
      command.setCreateNewClient(true);
      command.setClientName("新客户");
      command.setClientType("ENTERPRISE");
      command.setContactPhone("13800138000");

      ClientDTO client = new ClientDTO();
      client.setId(200L);
      client.setName("新客户");

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      when(clientAppService.createClient(any())).thenReturn(client);

      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.updateById(any(Lead.class))).thenReturn(1);

      // When
      LeadDTO result = leadAppService.convertLead(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(lead.getStatus()).isEqualTo("CONVERTED");
      assertThat(lead.getConvertedToClientId()).isEqualTo(200L);
      assertThat(lead.getConvertedAt()).isNotNull();
    }

    @Test
    @DisplayName("应该成功转化案源并创建项目和客户")
    void convertLead_shouldSuccess_whenCreateClientAndMatter() {
      // Given
      Lead lead =
          Lead.builder()
              .id(TEST_LEAD_ID)
              .status("PENDING")
              .sourceChannel("WEBSITE")
              .originatorId(TEST_USER_ID)
              .build();

      ConvertLeadCommand command = new ConvertLeadCommand();
      command.setLeadId(TEST_LEAD_ID);
      command.setCreateNewClient(true);
      command.setClientName("新客户");
      command.setCreateMatter(true);
      command.setMatterName("新项目");

      ClientDTO client = new ClientDTO();
      client.setId(200L);

      MatterDTO matter = new MatterDTO();
      matter.setId(300L);

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      when(clientAppService.createClient(any())).thenReturn(client);
      when(matterAppService.createMatter(any())).thenReturn(matter);

      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.updateById(any(Lead.class))).thenReturn(1);

      // When
      LeadDTO result = leadAppService.convertLead(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(lead.getStatus()).isEqualTo("CONVERTED");
      assertThat(lead.getConvertedToClientId()).isEqualTo(200L);
      assertThat(lead.getConvertedToMatterId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("已转化的案源不能重复转化")
    void convertLead_shouldFail_whenAlreadyConverted() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("CONVERTED").build();

      ConvertLeadCommand command = new ConvertLeadCommand();
      command.setLeadId(TEST_LEAD_ID);

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> leadAppService.convertLead(command));
      assertThat(exception.getMessage()).contains("已转化");
    }
  }

  @Nested
  @DisplayName("放弃案源测试")
  class AbandonLeadTests {

    @Test
    @DisplayName("应该成功放弃案源")
    void abandonLead_shouldSuccess() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("PENDING").remark("原备注").build();

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);
      com.lawfirm.infrastructure.persistence.mapper.LeadMapper leadBaseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.LeadMapper.class);
      when(leadRepository.getBaseMapper()).thenReturn(leadBaseMapper);
      when(leadBaseMapper.updateById(any(Lead.class))).thenReturn(1);

      // When
      leadAppService.abandonLead(TEST_LEAD_ID, "客户无需求");

      // Then
      assertThat(lead.getStatus()).isEqualTo("ABANDONED");
      assertThat(lead.getRemark()).contains("放弃原因");
    }

    @Test
    @DisplayName("已转化的案源不能放弃")
    void abandonLead_shouldFail_whenConverted() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).status("CONVERTED").build();

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> leadAppService.abandonLead(TEST_LEAD_ID, "原因"));
      assertThat(exception.getMessage()).contains("已转化");
    }
  }

  @Nested
  @DisplayName("查询案源测试")
  class QueryLeadTests {

    @Test
    @DisplayName("应该成功查询案源列表")
    void listLeads_shouldSuccess() {
      // Given
      LeadQueryDTO query = new LeadQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Lead lead = Lead.builder().id(TEST_LEAD_ID).leadName("案源1").status("PENDING").build();

      when(leadMapper.selectLeadPage(any(), any(), any(), any(), any(), anyList(), anyList()))
          .thenReturn(Collections.singletonList(lead));

      // When
      PageResult<LeadDTO> result = leadAppService.listLeads(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getLeadName()).isEqualTo("案源1");
    }

    @Test
    @DisplayName("应该成功获取案源详情")
    void getLead_shouldSuccess() {
      // Given
      Lead lead = Lead.builder().id(TEST_LEAD_ID).leadName("案源1").build();

      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(lead);

      // When
      LeadDTO result = leadAppService.getLead(TEST_LEAD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLeadName()).isEqualTo("案源1");
    }

    @Test
    @DisplayName("案源不存在应该失败")
    void getLead_shouldFail_whenNotFound() {
      // Given
      when(leadRepository.findById(TEST_LEAD_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> leadAppService.getLead(TEST_LEAD_ID));
      assertThat(exception.getMessage()).contains("案源不存在");
    }
  }

  @Nested
  @DisplayName("案源统计测试")
  class LeadStatisticsTests {

    @Test
    @DisplayName("应该成功获取案源统计")
    void getLeadStatistics_shouldSuccess() {
      // Given
      when(leadMapper.countTotalLeads()).thenReturn(100L);
      when(leadMapper.countConvertedLeads()).thenReturn(30L);
      when(leadMapper.countBySourceChannel()).thenReturn(Collections.emptyList());
      when(leadMapper.countByStatus()).thenReturn(Collections.emptyList());
      when(leadMapper.countByOriginator()).thenReturn(Collections.emptyList());
      when(leadMapper.analyzeConversionRate()).thenReturn(Collections.emptyList());
      when(leadMapper.countConversionTrend()).thenReturn(Collections.emptyList());

      // When
      LeadStatisticsDTO result = leadAppService.getLeadStatistics();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTotalLeads()).isEqualTo(100L);
      assertThat(result.getConvertedLeads()).isEqualTo(30L);
      assertThat(result.getConversionRate()).isEqualTo(30.0); // 30/100 * 100
    }

    @Test
    @DisplayName("总案源数为0时应返回0转化率")
    void getLeadStatistics_shouldReturnZeroRate_whenNoLeads() {
      // Given
      when(leadMapper.countTotalLeads()).thenReturn(0L);
      when(leadMapper.countConvertedLeads()).thenReturn(0L);
      when(leadMapper.countBySourceChannel()).thenReturn(Collections.emptyList());
      when(leadMapper.countByStatus()).thenReturn(Collections.emptyList());
      when(leadMapper.countByOriginator()).thenReturn(Collections.emptyList());
      when(leadMapper.analyzeConversionRate()).thenReturn(Collections.emptyList());
      when(leadMapper.countConversionTrend()).thenReturn(Collections.emptyList());

      // When
      LeadStatisticsDTO result = leadAppService.getLeadStatistics();

      // Then
      assertThat(result.getTotalLeads()).isEqualTo(0L);
      assertThat(result.getConversionRate()).isEqualTo(0.0);
    }
  }
}
