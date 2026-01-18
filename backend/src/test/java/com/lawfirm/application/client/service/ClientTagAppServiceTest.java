package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.AssignClientTagsCommand;
import com.lawfirm.application.client.command.CreateClientTagCommand;
import com.lawfirm.application.client.command.UpdateClientTagCommand;
import com.lawfirm.application.client.dto.ClientTagDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientTag;
import com.lawfirm.domain.client.entity.ClientTagRelation;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ClientTagRepository;
import com.lawfirm.infrastructure.persistence.mapper.ClientTagRelationMapper;
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
 * ClientTagAppService 单元测试
 * 测试客户标签管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientTagAppService 客户标签服务测试")
class ClientTagAppServiceTest {

    private static final Long TEST_TAG_ID = 100L;
    private static final Long TEST_CLIENT_ID = 200L;
    private static final Long TEST_USER_ID = 300L;

    @Mock
    private ClientTagRepository clientTagRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientTagRelationMapper clientTagRelationMapper;

    @InjectMocks
    private ClientTagAppService clientTagAppService;

    @Nested
    @DisplayName("创建标签测试")
    class CreateTagTests {

        @Test
        @DisplayName("应该成功创建标签")
        void createTag_shouldSuccess() {
            // Given
            CreateClientTagCommand command = new CreateClientTagCommand();
            command.setTagName("VIP客户");
            command.setTagColor("#ff0000");
            command.setDescription("重要客户标签");
            command.setSortOrder(1);

            when(clientTagRepository.existsByTagName("VIP客户")).thenReturn(false);
            when(clientTagRepository.save(any(ClientTag.class))).thenAnswer(invocation -> {
                ClientTag tag = invocation.getArgument(0);
                tag.setId(TEST_TAG_ID);
                return true;
            });

            // When
            ClientTagDTO result = clientTagAppService.createTag(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTagName()).isEqualTo("VIP客户");
            verify(clientTagRepository).save(any(ClientTag.class));
        }

        @Test
        @DisplayName("标签名称已存在应该失败")
        void createTag_shouldFail_whenNameExists() {
            // Given
            CreateClientTagCommand command = new CreateClientTagCommand();
            command.setTagName("VIP客户");

            when(clientTagRepository.existsByTagName("VIP客户")).thenReturn(true);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientTagAppService.createTag(command));
            assertThat(exception.getMessage()).contains("标签名称已存在");
        }
    }

    @Nested
    @DisplayName("更新标签测试")
    class UpdateTagTests {

        @Test
        @DisplayName("应该成功更新标签")
        void updateTag_shouldSuccess() {
            // Given
            UpdateClientTagCommand command = new UpdateClientTagCommand();
            command.setId(TEST_TAG_ID);
            command.setTagName("新标签名");
            command.setTagColor("#00ff00");

            ClientTag tag = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("原标签名")
                    .tagColor("#ff0000")
                    .build();

            when(clientTagRepository.getByIdOrThrow(eq(TEST_TAG_ID), anyString())).thenReturn(tag);
            when(clientTagRepository.existsByTagName("新标签名")).thenReturn(false);
            lenient().when(clientTagRepository.updateById(any(ClientTag.class))).thenReturn(true);

            // When
            ClientTagDTO result = clientTagAppService.updateTag(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(tag.getTagName()).isEqualTo("新标签名");
            assertThat(tag.getTagColor()).isEqualTo("#00ff00");
        }

        @Test
        @DisplayName("更新为已存在的标签名称应该失败")
        void updateTag_shouldFail_whenNameExists() {
            // Given
            UpdateClientTagCommand command = new UpdateClientTagCommand();
            command.setId(TEST_TAG_ID);
            command.setTagName("已存在的标签");

            ClientTag tag = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("原标签名")
                    .build();

            when(clientTagRepository.getByIdOrThrow(eq(TEST_TAG_ID), anyString())).thenReturn(tag);
            when(clientTagRepository.existsByTagName("已存在的标签")).thenReturn(true);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientTagAppService.updateTag(command));
            assertThat(exception.getMessage()).contains("标签名称已存在");
        }
    }

    @Nested
    @DisplayName("删除标签测试")
    class DeleteTagTests {

        @Test
        @DisplayName("应该成功删除标签")
        void deleteTag_shouldSuccess() {
            // Given
            ClientTag tag = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("测试标签")
                    .build();

            when(clientTagRepository.getByIdOrThrow(eq(TEST_TAG_ID), anyString())).thenReturn(tag);
            lenient().when(clientTagRepository.softDelete(TEST_TAG_ID)).thenReturn(true);
            lenient().doNothing().when(clientTagRelationMapper).deleteByTagId(TEST_TAG_ID);

            // When
            clientTagAppService.deleteTag(TEST_TAG_ID);

            // Then
            verify(clientTagRepository).softDelete(TEST_TAG_ID);
            verify(clientTagRelationMapper).deleteByTagId(TEST_TAG_ID);
        }
    }

    @Nested
    @DisplayName("查询标签测试")
    class QueryTagTests {

        @Test
        @DisplayName("应该成功查询所有标签")
        void listTags_shouldSuccess() {
            // Given
            ClientTag tag1 = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("标签1")
                    .build();

            ClientTag tag2 = ClientTag.builder()
                    .id(200L)
                    .tagName("标签2")
                    .build();

            when(clientTagRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                    .thenReturn(List.of(tag1, tag2));

            // When
            List<ClientTagDTO> result = clientTagAppService.listTags();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTagName()).isEqualTo("标签1");
        }

        @Test
        @DisplayName("应该成功根据ID查询标签")
        void getTagById_shouldSuccess() {
            // Given
            ClientTag tag = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("测试标签")
                    .build();

            when(clientTagRepository.getByIdOrThrow(eq(TEST_TAG_ID), anyString())).thenReturn(tag);

            // When
            ClientTagDTO result = clientTagAppService.getTagById(TEST_TAG_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTagName()).isEqualTo("测试标签");
        }

        @Test
        @DisplayName("应该成功查询客户的标签列表")
        void getClientTags_shouldSuccess() {
            // Given
            ClientTag tag = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .tagName("客户标签")
                    .build();

            when(clientTagRelationMapper.selectTagIdsByClientId(TEST_CLIENT_ID))
                    .thenReturn(List.of(TEST_TAG_ID));
            when(clientTagRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                    .thenReturn(Collections.singletonList(tag));

            // When
            List<ClientTagDTO> result = clientTagAppService.getClientTags(TEST_CLIENT_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTagName()).isEqualTo("客户标签");
        }
    }

    @Nested
    @DisplayName("分配标签测试")
    class AssignTagsTests {

        @Test
        @DisplayName("应该成功为客户分配标签")
        void assignTags_shouldSuccess() {
            // Given
            AssignClientTagsCommand command = new AssignClientTagsCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setTagIds(List.of(TEST_TAG_ID, 200L));

            Client client = new Client();
            client.setId(TEST_CLIENT_ID);

            ClientTag tag1 = ClientTag.builder()
                    .id(TEST_TAG_ID)
                    .build();

            ClientTag tag2 = ClientTag.builder()
                    .id(200L)
                    .build();

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            lenient().doNothing().when(clientTagRelationMapper).deleteByClientId(TEST_CLIENT_ID);
            when(clientTagRepository.getByIdOrThrow(eq(TEST_TAG_ID), anyString())).thenReturn(tag1);
            when(clientTagRepository.getByIdOrThrow(eq(200L), anyString())).thenReturn(tag2);
            lenient().when(clientTagRelationMapper.insert(any(ClientTagRelation.class))).thenReturn(1);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                clientTagAppService.assignTags(command);

                // Then
                verify(clientTagRelationMapper).deleteByClientId(TEST_CLIENT_ID);
                verify(clientTagRelationMapper, times(2)).insert(any(ClientTagRelation.class));
            }
        }

        @Test
        @DisplayName("分配空标签列表应该成功")
        void assignTags_shouldSuccess_whenEmptyList() {
            // Given
            AssignClientTagsCommand command = new AssignClientTagsCommand();
            command.setClientId(TEST_CLIENT_ID);
            command.setTagIds(Collections.emptyList());

            Client client = new Client();
            client.setId(TEST_CLIENT_ID);

            when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
            lenient().doNothing().when(clientTagRelationMapper).deleteByClientId(TEST_CLIENT_ID);

            // When
            clientTagAppService.assignTags(command);

            // Then
            verify(clientTagRelationMapper).deleteByClientId(TEST_CLIENT_ID);
            verify(clientTagRelationMapper, never()).insert(any(ClientTagRelation.class));
        }
    }

    @Nested
    @DisplayName("移除标签测试")
    class RemoveClientTagTests {

        @Test
        @DisplayName("应该成功移除客户的标签")
        void removeClientTag_shouldSuccess() {
            // Given
            lenient().when(clientTagRelationMapper.delete(any())).thenReturn(1);

            // When
            clientTagAppService.removeClientTag(TEST_CLIENT_ID, TEST_TAG_ID);

            // Then
            verify(clientTagRelationMapper).delete(any());
        }
    }
}
