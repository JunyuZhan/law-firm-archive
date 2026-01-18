package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateShareholderCommand;
import com.lawfirm.application.client.command.UpdateShareholderCommand;
import com.lawfirm.application.client.dto.ClientShareholderDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientShareholder;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ClientShareholderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClientShareholderAppService 单元测试
 * 测试客户股东信息服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientShareholderAppService 客户股东信息服务测试")
class ClientShareholderAppServiceTest {

    private static final Long TEST_SHAREHOLDER_ID = 100L;
    private static final Long TEST_CLIENT_ID = 200L;

    @Mock
    private ClientShareholderRepository shareholderRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientShareholderAppService clientShareholderAppService;

    @Nested
    @DisplayName("创建股东信息测试")
    class CreateShareholderTests {

        @Test
        @DisplayName("应该成功为企业客户创建股东信息")
        void createShareholder_shouldSuccess_forEnterprise() {
            // Given
            CreateShareholderCommand command = new CreateShareholderCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setShareholderName("张三");
            command.setShareholderType("INDIVIDUAL");
            command.setShareholdingRatio(new BigDecimal("30.00"));
            command.setInvestmentAmount(new BigDecimal("1000000.00"));
            command.setInvestmentDate(LocalDate.now());

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientType("ENTERPRISE")
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(shareholderRepository.save(any(ClientShareholder.class))).thenAnswer(invocation -> {
                ClientShareholder shareholder = invocation.getArgument(0);
                shareholder.setId(TEST_SHAREHOLDER_ID);
                return true;
            });

            // When
            ClientShareholderDTO result = clientShareholderAppService.createShareholder(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShareholderName()).isEqualTo("张三");
            verify(shareholderRepository).save(any(ClientShareholder.class));
        }

        @Test
        @DisplayName("非企业客户不能添加股东信息")
        void createShareholder_shouldFail_whenNotEnterprise() {
            // Given
            CreateShareholderCommand command = new CreateShareholderCommand();
            command.setClientId(TEST_CLIENT_ID);

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientType("INDIVIDUAL") // 个人客户
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientShareholderAppService.createShareholder(command));
            assertThat(exception.getMessage()).contains("企业客户");
        }
    }

    @Nested
    @DisplayName("更新股东信息测试")
    class UpdateShareholderTests {

        @Test
        @DisplayName("应该成功更新股东信息")
        void updateShareholder_shouldSuccess() {
            // Given
            UpdateShareholderCommand command = new UpdateShareholderCommand();
            command.setId(TEST_SHAREHOLDER_ID);
            command.setShareholderName("李四");
            command.setShareholdingRatio(new BigDecimal("40.00"));

            ClientShareholder shareholder = ClientShareholder.builder()
                    .id(TEST_SHAREHOLDER_ID)
                    .clientId(TEST_CLIENT_ID)
                    .shareholderName("张三")
                    .shareholdingRatio(new BigDecimal("30.00"))
                    .build();

            when(shareholderRepository.getByIdOrThrow(eq(TEST_SHAREHOLDER_ID), anyString())).thenReturn(shareholder);
            lenient().when(shareholderRepository.updateById(any(ClientShareholder.class))).thenReturn(true);

            // When
            ClientShareholderDTO result = clientShareholderAppService.updateShareholder(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(shareholder.getShareholderName()).isEqualTo("李四");
            assertThat(shareholder.getShareholdingRatio()).isEqualByComparingTo(new BigDecimal("40.00"));
        }
    }

    @Nested
    @DisplayName("删除股东信息测试")
    class DeleteShareholderTests {

        @Test
        @DisplayName("应该成功删除股东信息")
        void deleteShareholder_shouldSuccess() {
            // Given
            ClientShareholder shareholder = ClientShareholder.builder()
                    .id(TEST_SHAREHOLDER_ID)
                    .shareholderName("张三")
                    .build();

            com.lawfirm.infrastructure.persistence.mapper.ClientShareholderMapper baseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ClientShareholderMapper.class);
            when(shareholderRepository.getByIdOrThrow(eq(TEST_SHAREHOLDER_ID), anyString())).thenReturn(shareholder);
            lenient().when(shareholderRepository.getBaseMapper()).thenReturn(baseMapper);
            lenient().when(baseMapper.deleteById(TEST_SHAREHOLDER_ID)).thenReturn(1);

            // When
            clientShareholderAppService.deleteShareholder(TEST_SHAREHOLDER_ID);

            // Then
            verify(baseMapper).deleteById(TEST_SHAREHOLDER_ID);
        }
    }

    @Nested
    @DisplayName("查询股东信息测试")
    class QueryShareholderTests {

        @Test
        @DisplayName("应该成功获取客户的股东列表")
        void getShareholdersByClientId_shouldSuccess() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .build();

            ClientShareholder shareholder1 = ClientShareholder.builder()
                    .id(TEST_SHAREHOLDER_ID)
                    .clientId(TEST_CLIENT_ID)
                    .shareholderName("张三")
                    .shareholdingRatio(new BigDecimal("30.00"))
                    .build();

            ClientShareholder shareholder2 = ClientShareholder.builder()
                    .id(200L)
                    .clientId(TEST_CLIENT_ID)
                    .shareholderName("李四")
                    .shareholdingRatio(new BigDecimal("70.00"))
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(shareholderRepository.findByClientId(TEST_CLIENT_ID))
                    .thenReturn(List.of(shareholder1, shareholder2));

            // When
            List<ClientShareholderDTO> result = clientShareholderAppService.getShareholdersByClientId(TEST_CLIENT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getShareholderName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("客户没有股东应该返回空列表")
        void getShareholdersByClientId_shouldReturnEmpty_whenNoShareholders() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(shareholderRepository.findByClientId(TEST_CLIENT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<ClientShareholderDTO> result = clientShareholderAppService.getShareholdersByClientId(TEST_CLIENT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
