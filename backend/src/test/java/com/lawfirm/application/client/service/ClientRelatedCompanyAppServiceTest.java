package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateRelatedCompanyCommand;
import com.lawfirm.application.client.command.UpdateRelatedCompanyCommand;
import com.lawfirm.application.client.dto.ClientRelatedCompanyDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientRelatedCompany;
import com.lawfirm.domain.client.repository.ClientRelatedCompanyRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClientRelatedCompanyAppService 单元测试
 * 测试客户关联企业服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientRelatedCompanyAppService 客户关联企业服务测试")
class ClientRelatedCompanyAppServiceTest {

    private static final Long TEST_COMPANY_ID = 100L;
    private static final Long TEST_CLIENT_ID = 200L;

    @Mock
    private ClientRelatedCompanyRepository relatedCompanyRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientRelatedCompanyAppService clientRelatedCompanyAppService;

    @Nested
    @DisplayName("创建关联企业测试")
    class CreateRelatedCompanyTests {

        @Test
        @DisplayName("应该成功为企业客户创建关联企业")
        void createRelatedCompany_shouldSuccess_forEnterprise() {
            // Given
            CreateRelatedCompanyCommand command = new CreateRelatedCompanyCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setRelatedCompanyName("关联公司A");
            command.setRelatedCompanyType("SUBSIDIARY");
            command.setCreditCode("91110000MA01234567");
            command.setLegalRepresentative("张三");
            command.setRelationshipDescription("全资子公司");

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientType("ENTERPRISE")
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(relatedCompanyRepository.save(any(ClientRelatedCompany.class))).thenAnswer(invocation -> {
                ClientRelatedCompany company = invocation.getArgument(0);
                company.setId(TEST_COMPANY_ID);
                return true;
            });

            // When
            ClientRelatedCompanyDTO result = clientRelatedCompanyAppService.createRelatedCompany(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRelatedCompanyName()).isEqualTo("关联公司A");
            verify(relatedCompanyRepository).save(any(ClientRelatedCompany.class));
        }

        @Test
        @DisplayName("非企业客户不能添加关联企业")
        void createRelatedCompany_shouldFail_whenNotEnterprise() {
            // Given
            CreateRelatedCompanyCommand command = new CreateRelatedCompanyCommand();
            command.setClientId(TEST_CLIENT_ID);

            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .clientType("INDIVIDUAL") // 个人客户
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientRelatedCompanyAppService.createRelatedCompany(command));
            assertThat(exception.getMessage()).contains("企业客户");
        }
    }

    @Nested
    @DisplayName("更新关联企业测试")
    class UpdateRelatedCompanyTests {

        @Test
        @DisplayName("应该成功更新关联企业")
        void updateRelatedCompany_shouldSuccess() {
            // Given
            UpdateRelatedCompanyCommand command = new UpdateRelatedCompanyCommand();
            command.setId(TEST_COMPANY_ID);
            command.setRelatedCompanyName("更新后的公司名");
            command.setLegalRepresentative("李四");

            ClientRelatedCompany company = ClientRelatedCompany.builder()
                    .id(TEST_COMPANY_ID)
                    .clientId(TEST_CLIENT_ID)
                    .relatedCompanyName("原公司名")
                    .legalRepresentative("张三")
                    .build();

            when(relatedCompanyRepository.getByIdOrThrow(eq(TEST_COMPANY_ID), anyString())).thenReturn(company);
            lenient().when(relatedCompanyRepository.updateById(any(ClientRelatedCompany.class))).thenReturn(true);

            // When
            ClientRelatedCompanyDTO result = clientRelatedCompanyAppService.updateRelatedCompany(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(company.getRelatedCompanyName()).isEqualTo("更新后的公司名");
            assertThat(company.getLegalRepresentative()).isEqualTo("李四");
        }
    }

    @Nested
    @DisplayName("删除关联企业测试")
    class DeleteRelatedCompanyTests {

        @Test
        @DisplayName("应该成功删除关联企业")
        void deleteRelatedCompany_shouldSuccess() {
            // Given
            ClientRelatedCompany company = ClientRelatedCompany.builder()
                    .id(TEST_COMPANY_ID)
                    .relatedCompanyName("关联公司A")
                    .build();

            com.lawfirm.infrastructure.persistence.mapper.ClientRelatedCompanyMapper baseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.ClientRelatedCompanyMapper.class);
            when(relatedCompanyRepository.getByIdOrThrow(eq(TEST_COMPANY_ID), anyString())).thenReturn(company);
            lenient().when(relatedCompanyRepository.getBaseMapper()).thenReturn(baseMapper);
            lenient().when(baseMapper.deleteById(TEST_COMPANY_ID)).thenReturn(1);

            // When
            clientRelatedCompanyAppService.deleteRelatedCompany(TEST_COMPANY_ID);

            // Then
            verify(baseMapper).deleteById(TEST_COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("查询关联企业测试")
    class QueryRelatedCompanyTests {

        @Test
        @DisplayName("应该成功获取客户的关联企业列表")
        void getRelatedCompaniesByClientId_shouldSuccess() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .build();

            ClientRelatedCompany company1 = ClientRelatedCompany.builder()
                    .id(TEST_COMPANY_ID)
                    .clientId(TEST_CLIENT_ID)
                    .relatedCompanyName("关联公司A")
                    .relatedCompanyType("SUBSIDIARY")
                    .build();

            ClientRelatedCompany company2 = ClientRelatedCompany.builder()
                    .id(200L)
                    .clientId(TEST_CLIENT_ID)
                    .relatedCompanyName("关联公司B")
                    .relatedCompanyType("AFFILIATE")
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(relatedCompanyRepository.findByClientId(TEST_CLIENT_ID))
                    .thenReturn(List.of(company1, company2));

            // When
            List<ClientRelatedCompanyDTO> result = clientRelatedCompanyAppService.getRelatedCompaniesByClientId(TEST_CLIENT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRelatedCompanyName()).isEqualTo("关联公司A");
        }

        @Test
        @DisplayName("客户没有关联企业应该返回空列表")
        void getRelatedCompaniesByClientId_shouldReturnEmpty_whenNoCompanies() {
            // Given
            Client client = Client.builder()
                    .id(TEST_CLIENT_ID)
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(relatedCompanyRepository.findByClientId(TEST_CLIENT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<ClientRelatedCompanyDTO> result = clientRelatedCompanyAppService.getRelatedCompaniesByClientId(TEST_CLIENT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
