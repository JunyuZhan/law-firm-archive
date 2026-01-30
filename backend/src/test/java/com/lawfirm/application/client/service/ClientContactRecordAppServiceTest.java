package com.lawfirm.application.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.CreateContactRecordCommand;
import com.lawfirm.application.client.command.UpdateContactRecordCommand;
import com.lawfirm.application.client.dto.ClientContactRecordDTO;
import com.lawfirm.application.client.dto.ContactRecordQueryDTO;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import com.lawfirm.domain.client.repository.ClientContactRecordRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** ClientContactRecordAppService 单元测试 测试客户联系记录服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientContactRecordAppService 客户联系记录服务测试")
class ClientContactRecordAppServiceTest {

  private static final Long TEST_RECORD_ID = 100L;
  private static final Long TEST_CLIENT_ID = 200L;
  private static final Long TEST_USER_ID = 400L;

  @Mock private ClientContactRecordRepository contactRecordRepository;

  @Mock private ClientRepository clientRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private ClientContactRecordAppService clientContactRecordAppService;

  @Nested
  @DisplayName("创建联系记录测试")
  class CreateContactRecordTests {

    @Test
    @DisplayName("应该成功创建联系记录")
    void createContactRecord_shouldSuccess() {
      // Given
      CreateContactRecordCommand command = new CreateContactRecordCommand();
      command.setClientId(TEST_CLIENT_ID);
      command.setContactMethod("PHONE");
      command.setContactDate(LocalDateTime.now());
      command.setContactContent("电话沟通");
      command.setFollowUpReminder(true);

      Client client = new Client();
      client.setId(TEST_CLIENT_ID);

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      when(contactRecordRepository.save(any(ClientContactRecord.class)))
          .thenAnswer(
              invocation -> {
                ClientContactRecord record = invocation.getArgument(0);
                record.setId(TEST_RECORD_ID);
                return true;
              });
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(client);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        ClientContactRecordDTO result = clientContactRecordAppService.createContactRecord(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContactMethod()).isEqualTo("PHONE");
        verify(contactRecordRepository).save(any(ClientContactRecord.class));
      }
    }
  }

  @Nested
  @DisplayName("更新联系记录测试")
  class UpdateContactRecordTests {

    @Test
    @DisplayName("应该成功更新联系记录")
    void updateContactRecord_shouldSuccess() {
      // Given
      UpdateContactRecordCommand command = new UpdateContactRecordCommand();
      command.setId(TEST_RECORD_ID);
      command.setContactContent("更新后的内容");
      command.setContactResult("已达成初步意向");

      ClientContactRecord record =
          ClientContactRecord.builder()
              .id(TEST_RECORD_ID)
              .clientId(TEST_CLIENT_ID)
              .contactContent("原内容")
              .build();

      Client client = new Client();
      client.setId(TEST_CLIENT_ID);
      client.setName("测试客户");

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(contactRecordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString()))
          .thenReturn(record);
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(client);
      lenient().when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      lenient()
          .when(contactRecordRepository.updateById(any(ClientContactRecord.class)))
          .thenReturn(true);

      // When
      ClientContactRecordDTO result = clientContactRecordAppService.updateContactRecord(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(record.getContactContent()).isEqualTo("更新后的内容");
      assertThat(record.getContactResult()).isEqualTo("已达成初步意向");
    }
  }

  @Nested
  @DisplayName("删除联系记录测试")
  class DeleteContactRecordTests {

    @Test
    @DisplayName("应该成功删除联系记录")
    void deleteContactRecord_shouldSuccess() {
      // Given
      ClientContactRecord record =
          ClientContactRecord.builder().id(TEST_RECORD_ID).clientId(TEST_CLIENT_ID).build();

      com.lawfirm.infrastructure.persistence.mapper.ClientContactRecordMapper baseMapper =
          mock(com.lawfirm.infrastructure.persistence.mapper.ClientContactRecordMapper.class);
      when(contactRecordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString()))
          .thenReturn(record);
      lenient().when(contactRecordRepository.getBaseMapper()).thenReturn(baseMapper);
      lenient().when(baseMapper.deleteById(TEST_RECORD_ID)).thenReturn(1);

      // When
      clientContactRecordAppService.deleteContactRecord(TEST_RECORD_ID);

      // Then
      verify(contactRecordRepository.getBaseMapper()).deleteById(TEST_RECORD_ID);
    }
  }

  @Nested
  @DisplayName("查询联系记录测试")
  class QueryContactRecordTests {

    @Test
    @DisplayName("应该成功分页查询联系记录")
    @SuppressWarnings("unchecked")
    void listContactRecords_shouldSuccess() {
      // Given
      ContactRecordQueryDTO query = new ContactRecordQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);
      query.setClientId(TEST_CLIENT_ID);

      ClientContactRecord record =
          ClientContactRecord.builder()
              .id(TEST_RECORD_ID)
              .clientId(TEST_CLIENT_ID)
              .contactMethod("PHONE")
              .build();

      Page<ClientContactRecord> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(record));
      page.setTotal(1);

      when(contactRecordRepository.findByConditions(
              any(Page.class), eq(TEST_CLIENT_ID), any(), any(), any(), any(), any()))
          .thenReturn(page);
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(new Client());

      // When
      var result = clientContactRecordAppService.listContactRecords(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("应该成功获取客户的联系记录列表")
    void getContactRecordsByClientId_shouldSuccess() {
      // Given
      Client client = new Client();
      client.setId(TEST_CLIENT_ID);

      ClientContactRecord record =
          ClientContactRecord.builder()
              .id(TEST_RECORD_ID)
              .clientId(TEST_CLIENT_ID)
              .contactMethod("EMAIL")
              .build();

      when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
      when(contactRecordRepository.findAllByClientId(TEST_CLIENT_ID))
          .thenReturn(Collections.singletonList(record));
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(client);

      // When
      List<ClientContactRecordDTO> result =
          clientContactRecordAppService.getContactRecordsByClientId(TEST_CLIENT_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getContactMethod()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("应该成功查询需要跟进的联系记录")
    void getFollowUpRecords_shouldSuccess() {
      // Given
      LocalDate followUpDate = LocalDate.now().plusDays(1);

      ClientContactRecord record =
          ClientContactRecord.builder()
              .id(TEST_RECORD_ID)
              .clientId(TEST_CLIENT_ID)
              .nextFollowUpDate(followUpDate)
              .followUpReminder(true)
              .build();

      when(contactRecordRepository.findFollowUpRecords(followUpDate))
          .thenReturn(Collections.singletonList(record));
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(new Client());

      // When
      List<ClientContactRecordDTO> result =
          clientContactRecordAppService.getFollowUpRecords(followUpDate);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getFollowUpReminder()).isTrue();
    }

    @Test
    @DisplayName("查询跟进记录时日期为空应该使用当前日期")
    void getFollowUpRecords_shouldUseToday_whenDateNull() {
      // Given
      ClientContactRecord record =
          ClientContactRecord.builder()
              .id(TEST_RECORD_ID)
              .clientId(TEST_CLIENT_ID)
              .nextFollowUpDate(LocalDate.now())
              .build();

      when(contactRecordRepository.findFollowUpRecords(LocalDate.now()))
          .thenReturn(Collections.singletonList(record));
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(new Client());

      // When
      List<ClientContactRecordDTO> result = clientContactRecordAppService.getFollowUpRecords(null);

      // Then
      assertThat(result).hasSize(1);
    }
  }
}
