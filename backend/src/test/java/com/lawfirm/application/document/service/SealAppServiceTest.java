package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateSealCommand;
import com.lawfirm.application.document.command.UpdateSealCommand;
import com.lawfirm.application.document.dto.SealDTO;
import com.lawfirm.application.document.dto.SealQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.domain.document.repository.SealApplicationRepository;
import com.lawfirm.domain.document.repository.SealRepository;
import com.lawfirm.infrastructure.persistence.mapper.SealMapper;
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
 * SealAppService 单元测试
 * 测试印章管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SealAppService 印章服务测试")
class SealAppServiceTest {

    private static final Long TEST_SEAL_ID = 100L;
    private static final Long TEST_KEEPER_ID = 200L;

    @Mock
    private SealRepository sealRepository;

    @Mock
    private SealApplicationRepository applicationRepository;

    @Mock
    private SealMapper sealMapper;

    @InjectMocks
    private SealAppService sealAppService;

    @Nested
    @DisplayName("创建印章测试")
    class CreateSealTests {

        @Test
        @DisplayName("应该成功创建印章")
        void createSeal_shouldSuccess() {
            // Given
            CreateSealCommand command = new CreateSealCommand();
            command.setName("公章");
            command.setSealType("OFFICIAL");
            command.setKeeperId(TEST_KEEPER_ID);
            command.setKeeperName("保管人");
            command.setImageUrl("http://example.com/seal.png");
            command.setDescription("公章描述");

            lenient().when(sealRepository.save(any(Seal.class))).thenAnswer(invocation -> {
                Seal seal = invocation.getArgument(0);
                seal.setId(TEST_SEAL_ID);
                return true;
            });

            // When
            SealDTO result = sealAppService.createSeal(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("公章");
            assertThat(result.getSealType()).isEqualTo("OFFICIAL");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(sealRepository).save(any(Seal.class));
        }
    }

    @Nested
    @DisplayName("更新印章测试")
    class UpdateSealTests {

        @Test
        @DisplayName("应该成功更新印章")
        void updateSeal_shouldSuccess() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("原名称")
                    .keeperId(TEST_KEEPER_ID)
                    .build();

            UpdateSealCommand command = new UpdateSealCommand();
            command.setName("新名称");
            command.setDescription("新描述");

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
            lenient().when(sealRepository.updateById(any(Seal.class))).thenReturn(true);

            // When
            SealDTO result = sealAppService.updateSeal(TEST_SEAL_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(seal.getName()).isEqualTo("新名称");
            assertThat(seal.getDescription()).isEqualTo("新描述");
        }
    }

    @Nested
    @DisplayName("变更印章状态测试")
    class ChangeSealStatusTests {

        @Test
        @DisplayName("应该成功变更印章状态")
        void changeSealStatus_shouldSuccess() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("公章")
                    .status("ACTIVE")
                    .build();

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
            lenient().when(sealRepository.updateById(any(Seal.class))).thenReturn(true);

            // When
            sealAppService.changeSealStatus(TEST_SEAL_ID, "DISABLED");

            // Then
            assertThat(seal.getStatus()).isEqualTo("DISABLED");
        }

        @Test
        @DisplayName("已销毁的印章不能变更状态")
        void changeSealStatus_shouldFail_whenDestroyed() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .status("DESTROYED")
                    .build();

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> sealAppService.changeSealStatus(TEST_SEAL_ID, "ACTIVE"));
            assertThat(exception.getMessage()).contains("已销毁");
        }
    }

    @Nested
    @DisplayName("删除印章测试")
    class DeleteSealTests {

        @Test
        @DisplayName("应该成功删除印章")
        void deleteSeal_shouldSuccess() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("公章")
                    .build();

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
            when(applicationRepository.countPendingBySealId(TEST_SEAL_ID)).thenReturn(0);
            lenient().when(sealRepository.removeById(TEST_SEAL_ID)).thenReturn(true);

            // When
            sealAppService.deleteSeal(TEST_SEAL_ID);

            // Then
            verify(sealRepository).removeById(TEST_SEAL_ID);
        }

        @Test
        @DisplayName("存在待处理申请的印章不能删除")
        void deleteSeal_shouldFail_whenHasPendingApplications() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("公章")
                    .build();

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
            when(applicationRepository.countPendingBySealId(TEST_SEAL_ID)).thenReturn(1);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> sealAppService.deleteSeal(TEST_SEAL_ID));
            assertThat(exception.getMessage()).contains("待处理");
        }
    }

    @Nested
    @DisplayName("查询印章测试")
    class QuerySealTests {

        @Test
        @DisplayName("应该成功查询印章列表")
        void listSeals_shouldSuccess() {
            // Given
            SealQueryDTO query = new SealQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("公章")
                    .status("ACTIVE")
                    .build();

            Page<Seal> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(seal));
            page.setTotal(1);

            when(sealMapper.selectSealPage(any(Page.class), any(), any(), any(), any())).thenReturn(page);

            // When
            PageResult<SealDTO> result = sealAppService.listSeals(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("公章");
        }

        @Test
        @DisplayName("应该成功获取印章详情")
        void getSealById_shouldSuccess() {
            // Given
            Seal seal = Seal.builder()
                    .id(TEST_SEAL_ID)
                    .name("公章")
                    .build();

            when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
            when(applicationRepository.countUsageBySealId(TEST_SEAL_ID)).thenReturn(5);

            // When
            SealDTO result = sealAppService.getSealById(TEST_SEAL_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("公章");
            assertThat(result.getUsageCount()).isEqualTo(5);
        }
    }
}
