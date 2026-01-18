package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateClientChangeHistoryCommand;
import com.lawfirm.application.client.dto.ClientChangeHistoryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientChangeHistory;
import com.lawfirm.domain.client.repository.ClientChangeHistoryRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.infrastructure.persistence.mapper.ClientChangeHistoryMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClientChangeHistoryAppService 单元测试
 * 测试客户变更历史服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientChangeHistoryAppService 客户变更历史服务测试")
class ClientChangeHistoryAppServiceTest {

    private static final Long TEST_HISTORY_ID = 100L;
    private static final Long TEST_CLIENT_ID = 200L;

    @Mock
    private ClientChangeHistoryRepository changeHistoryRepository;

    @Mock
    private ClientChangeHistoryMapper changeHistoryMapper;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientChangeHistoryAppService clientChangeHistoryAppService;

    @Nested
    @DisplayName("创建变更记录测试")
    class CreateChangeHistoryTests {

        @Test
        @DisplayName("应该成功为企业客户创建变更记录")
        void createChangeHistory_shouldSuccess_forEnterprise() {
            // Given
            CreateClientChangeHistoryCommand command = new CreateClientChangeHistoryCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setChangeType("NAME");
            command.setChangeDate(LocalDate.now());
            command.setBeforeValue("原公司名");
            command.setAfterValue("新公司名");
            command.setChangeDescription("公司名称变更");
            command.setRegistrationAuthority("市场监督管理局");
            command.setRegistrationNumber("CHG2024001");

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .name("测试企业")
                    .clientType("ENTERPRISE")
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(changeHistoryRepository.save(any(ClientChangeHistory.class))).thenAnswer(invocation -> {
                ClientChangeHistory history = invocation.getArgument(0);
                history.setId(TEST_HISTORY_ID);
                return true;
            });
            lenient().when(clientRepository.getById(TEST_CLIENT_ID)).thenReturn(client);

            // When
            ClientChangeHistoryDTO result = clientChangeHistoryAppService.createChangeHistory(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChangeType()).isEqualTo("NAME");
            verify(changeHistoryRepository).save(any(ClientChangeHistory.class));
        }

        @Test
        @DisplayName("非企业客户不能创建变更记录")
        void createChangeHistory_shouldFail_whenNotEnterprise() {
            // Given
            CreateClientChangeHistoryCommand command = new CreateClientChangeHistoryCommand();
            command.setClientId(TEST_CLIENT_ID);

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientType("INDIVIDUAL") // 个人客户
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientChangeHistoryAppService.createChangeHistory(command));
            assertThat(exception.getMessage()).contains("企业客户");
        }
    }

    @Nested
    @DisplayName("查询变更记录测试")
    class QueryChangeHistoryTests {

        @Test
        @DisplayName("应该成功获取客户的所有变更记录")
        void getClientChangeHistories_shouldSuccess() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .name("测试企业")
                    .build();

            ClientChangeHistory history1 = ClientChangeHistory.builder()
                    .id(TEST_HISTORY_ID)
                    .clientId(TEST_CLIENT_ID)
                    .changeType("NAME")
                    .changeDate(LocalDate.now())
                    .build();

            ClientChangeHistory history2 = ClientChangeHistory.builder()
                    .id(200L)
                    .clientId(TEST_CLIENT_ID)
                    .changeType("LEGAL_REPRESENTATIVE")
                    .changeDate(LocalDate.now().minusDays(10))
                    .build();

            when(changeHistoryMapper.selectByClientId(TEST_CLIENT_ID))
                    .thenReturn(List.of(history1, history2));
            when(clientRepository.getById(TEST_CLIENT_ID)).thenReturn(client);

            // When
            List<ClientChangeHistoryDTO> result = clientChangeHistoryAppService.getClientChangeHistories(TEST_CLIENT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getChangeType()).isEqualTo("NAME");
        }

        @Test
        @DisplayName("应该成功获取指定类型的变更记录")
        void getChangeHistoriesByType_shouldSuccess() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .name("测试企业")
                    .build();

            ClientChangeHistory history = ClientChangeHistory.builder()
                    .id(TEST_HISTORY_ID)
                    .clientId(TEST_CLIENT_ID)
                    .changeType("NAME")
                    .changeDate(LocalDate.now())
                    .build();

            when(changeHistoryMapper.selectByClientIdAndType(TEST_CLIENT_ID, "NAME"))
                    .thenReturn(Collections.singletonList(history));
            when(clientRepository.getById(TEST_CLIENT_ID)).thenReturn(client);

            // When
            List<ClientChangeHistoryDTO> result = clientChangeHistoryAppService.getChangeHistoriesByType(TEST_CLIENT_ID, "NAME");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChangeType()).isEqualTo("NAME");
        }
    }

    @Nested
    @DisplayName("删除变更记录测试")
    class DeleteChangeHistoryTests {

        @Test
        @DisplayName("应该成功删除变更记录")
        void deleteChangeHistory_shouldSuccess() {
            // Given
            ClientChangeHistory history = ClientChangeHistory.builder()
                    .id(TEST_HISTORY_ID)
                    .clientId(TEST_CLIENT_ID)
                    .build();

            when(changeHistoryRepository.getByIdOrThrow(eq(TEST_HISTORY_ID), anyString())).thenReturn(history);
            lenient().when(changeHistoryRepository.removeById(TEST_HISTORY_ID)).thenReturn(true);

            // When
            clientChangeHistoryAppService.deleteChangeHistory(TEST_HISTORY_ID);

            // Then
            verify(changeHistoryRepository).removeById(TEST_HISTORY_ID);
        }
    }
}
