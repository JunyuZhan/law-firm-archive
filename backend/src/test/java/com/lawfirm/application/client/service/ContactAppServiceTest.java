package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateContactCommand;
import com.lawfirm.application.client.command.UpdateContactCommand;
import com.lawfirm.application.client.dto.ContactDTO;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.Contact;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ContactRepository;
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
 * ContactAppService 单元测试
 * 测试联系人管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContactAppService 联系人服务测试")
class ContactAppServiceTest {

    private static final Long TEST_CONTACT_ID = 100L;
    private static final Long TEST_CLIENT_ID = 200L;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ContactAppService contactAppService;

    @Nested
    @DisplayName("创建联系人测试")
    class CreateContactTests {

        @Test
        @DisplayName("应该成功创建联系人")
        void createContact_shouldSuccess() {
            // Given
            CreateContactCommand command = new CreateContactCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setContactName("张三");
            command.setPosition("法务总监");
            command.setMobilePhone("13800138000");
            command.setEmail("zhangsan@example.com");
            command.setIsPrimary(false);

            Client client = new Client();
            client.setId(TEST_CLIENT_ID);

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
                Contact contact = invocation.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return true;
            });

            // When
            ContactDTO result = contactAppService.createContact(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContactName()).isEqualTo("张三");
            assertThat(result.getIsPrimary()).isFalse();
            verify(contactRepository).save(any(Contact.class));
        }

        @Test
        @DisplayName("应该成功创建主要联系人并清除其他主要联系人")
        void createContact_shouldClearOtherPrimary() {
            // Given
            CreateContactCommand command = new CreateContactCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setContactName("李四");
            command.setIsPrimary(true);

            Client client = new Client();
            client.setId(TEST_CLIENT_ID);

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            lenient().doNothing().when(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
            when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
                Contact contact = invocation.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return true;
            });

            // When
            ContactDTO result = contactAppService.createContact(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsPrimary()).isTrue();
            verify(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("更新联系人测试")
    class UpdateContactTests {

        @Test
        @DisplayName("应该成功更新联系人")
        void updateContact_shouldSuccess() {
            // Given
            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .clientId(TEST_CLIENT_ID)
                    .contactName("原名称")
                    .isPrimary(false)
                    .build();

            UpdateContactCommand command = new UpdateContactCommand();
            command.setContactName("新名称");
            command.setMobilePhone("13900139000");

            when(contactRepository.getByIdOrThrow(eq(TEST_CONTACT_ID), anyString())).thenReturn(contact);
            lenient().when(contactRepository.updateById(any(Contact.class))).thenReturn(true);

            // When
            ContactDTO result = contactAppService.updateContact(TEST_CONTACT_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(contact.getContactName()).isEqualTo("新名称");
            assertThat(contact.getMobilePhone()).isEqualTo("13900139000");
        }

        @Test
        @DisplayName("设置为主要联系人应该清除其他主要联系人")
        void updateContact_shouldClearOtherPrimary() {
            // Given
            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .clientId(TEST_CLIENT_ID)
                    .isPrimary(false)
                    .build();

            UpdateContactCommand command = new UpdateContactCommand();
            command.setIsPrimary(true);

            when(contactRepository.getByIdOrThrow(eq(TEST_CONTACT_ID), anyString())).thenReturn(contact);
            lenient().doNothing().when(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
            lenient().when(contactRepository.updateById(any(Contact.class))).thenReturn(true);

            // When
            ContactDTO result = contactAppService.updateContact(TEST_CONTACT_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(contact.getIsPrimary()).isTrue();
            verify(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("删除联系人测试")
    class DeleteContactTests {

        @Test
        @DisplayName("应该成功删除联系人")
        void deleteContact_shouldSuccess() {
            // Given
            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .contactName("张三")
                    .build();

            when(contactRepository.getByIdOrThrow(eq(TEST_CONTACT_ID), anyString())).thenReturn(contact);
            lenient().when(contactRepository.softDelete(TEST_CONTACT_ID)).thenReturn(true);

            // When
            contactAppService.deleteContact(TEST_CONTACT_ID);

            // Then
            verify(contactRepository).softDelete(TEST_CONTACT_ID);
        }
    }

    @Nested
    @DisplayName("设置主要联系人测试")
    class SetPrimaryContactTests {

        @Test
        @DisplayName("应该成功设置主要联系人")
        void setPrimaryContact_shouldSuccess() {
            // Given
            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .clientId(TEST_CLIENT_ID)
                    .contactName("张三")
                    .isPrimary(false)
                    .build();

            when(contactRepository.getByIdOrThrow(eq(TEST_CONTACT_ID), anyString())).thenReturn(contact);
            lenient().doNothing().when(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
            lenient().when(contactRepository.updateById(any(Contact.class))).thenReturn(true);

            // When
            ContactDTO result = contactAppService.setPrimaryContact(TEST_CONTACT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(contact.getIsPrimary()).isTrue();
            verify(contactRepository).clearPrimaryByClientId(TEST_CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("查询联系人测试")
    class QueryContactTests {

        @Test
        @DisplayName("应该成功查询客户联系人列表")
        void listContacts_shouldSuccess() {
            // Given
            Client client = new Client();
            client.setId(TEST_CLIENT_ID);

            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .clientId(TEST_CLIENT_ID)
                    .contactName("张三")
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            when(contactRepository.findByClientId(TEST_CLIENT_ID))
                    .thenReturn(Collections.singletonList(contact));

            // When
            List<ContactDTO> result = contactAppService.listContacts(TEST_CLIENT_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContactName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("应该成功获取联系人详情")
        void getContactById_shouldSuccess() {
            // Given
            Contact contact = Contact.builder()
                    .id(TEST_CONTACT_ID)
                    .contactName("张三")
                    .build();

            when(contactRepository.getByIdOrThrow(eq(TEST_CONTACT_ID), anyString())).thenReturn(contact);

            // When
            ContactDTO result = contactAppService.getContactById(TEST_CONTACT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContactName()).isEqualTo("张三");
        }
    }
}
