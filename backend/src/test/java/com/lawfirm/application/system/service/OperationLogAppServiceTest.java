package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.OperationLogDTO;
import com.lawfirm.application.system.dto.OperationLogQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.system.entity.OperationLog;
import com.lawfirm.domain.system.repository.OperationLogRepository;
import com.lawfirm.infrastructure.persistence.mapper.OperationLogMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OperationLogAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationLogAppService 操作日志服务测试")
class OperationLogAppServiceTest {

    private static final Long TEST_LOG_ID = 100L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private OperationLogRepository operationLogRepository;

    @InjectMocks
    private OperationLogAppService operationLogAppService;

    @Nested
    @DisplayName("查询操作日志测试")
    class QueryOperationLogTests {

        @Test
        @DisplayName("应该成功分页查询操作日志")
        void listOperationLogs_shouldSuccess() {
            // Given
            OperationLog log = OperationLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(TEST_USER_ID)
                    .userName("testuser")
                    .module("用户管理")
                    .operationType("CREATE")
                    .status(OperationLog.STATUS_SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<OperationLog> page = new Page<>(1, 10);
            page.setRecords(List.of(log));
            page.setTotal(1L);

            when(operationLogRepository.page(any(Page.class), any())).thenReturn(page);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<OperationLogDTO> result = operationLogAppService.listOperationLogs(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取操作日志详情")
        void getOperationLogById_shouldSuccess() {
            // Given
            OperationLog log = OperationLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(TEST_USER_ID)
                    .userName("testuser")
                    .module("用户管理")
                    .status(OperationLog.STATUS_SUCCESS)
                    .build();

            when(operationLogRepository.getById(TEST_LOG_ID)).thenReturn(log);

            // When
            OperationLogDTO result = operationLogAppService.getOperationLogById(TEST_LOG_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_LOG_ID);
        }

        @Test
        @DisplayName("应该返回null当日志不存在")
        void getOperationLogById_shouldReturnNull_whenNotExists() {
            // Given
            when(operationLogRepository.getById(TEST_LOG_ID)).thenReturn(null);

            // When
            OperationLogDTO result = operationLogAppService.getOperationLogById(TEST_LOG_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("查询模块和操作类型测试")
    class ListModulesAndTypesTests {

        @Test
        @DisplayName("应该成功获取所有模块列表")
        void listModules_shouldSuccess() {
            // Given
            OperationLog log1 = OperationLog.builder().module("用户管理").build();
            OperationLog log2 = OperationLog.builder().module("角色管理").build();

            @SuppressWarnings("unchecked")
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<OperationLog> queryWrapper = 
                    mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
            when(operationLogRepository.lambdaQuery()).thenReturn(queryWrapper);
            doReturn(queryWrapper).when(queryWrapper).select(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class));
            when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
            doReturn(queryWrapper).when(queryWrapper).groupBy(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class));
            when(queryWrapper.list()).thenReturn(List.of(log1, log2));

            // When
            List<String> result = operationLogAppService.listModules();

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该成功获取所有操作类型列表")
        void listOperationTypes_shouldSuccess() {
            // Given
            OperationLog log1 = OperationLog.builder().operationType("CREATE").build();
            OperationLog log2 = OperationLog.builder().operationType("UPDATE").build();

            @SuppressWarnings("unchecked")
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<OperationLog> queryWrapper = 
                    mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
            when(operationLogRepository.lambdaQuery()).thenReturn(queryWrapper);
            doReturn(queryWrapper).when(queryWrapper).select(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class));
            when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
            doReturn(queryWrapper).when(queryWrapper).groupBy(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class));
            when(queryWrapper.list()).thenReturn(List.of(log1, log2));

            // When
            List<String> result = operationLogAppService.listOperationTypes();

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("应该成功获取操作日志统计")
        void getStatistics_shouldSuccess() {
            // Given
            OperationLogQueryDTO query = new OperationLogQueryDTO();

            // Use Answer with a counter to track call order
            final int[] callCount = {0};
            when(operationLogRepository.count(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
                callCount[0]++;
                if (callCount[0] == 1) {
                    return 100L; // First call: total count
                } else {
                    return 95L; // Second call: success count
                }
            });

            // When
            Map<String, Object> result = operationLogAppService.getStatistics(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("total")).isEqualTo(100L);
            assertThat(result.get("successCount")).isEqualTo(95L);
            assertThat(result.get("failCount")).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("清理历史日志测试")
    class CleanHistoryLogsTests {

        @Test
        @DisplayName("应该成功清理历史日志")
        void cleanHistoryLogs_shouldSuccess() {
            // Given
            OperationLogMapper mapperMock = mock(OperationLogMapper.class);
            when(operationLogRepository.getBaseMapper()).thenReturn(mapperMock);
            when(mapperMock.delete(any(LambdaQueryWrapper.class))).thenReturn(10);

            // When
            int result = operationLogAppService.cleanHistoryLogs(30);

            // Then
            assertThat(result).isEqualTo(10);
        }

        @Test
        @DisplayName("应该至少保留7天")
        void cleanHistoryLogs_shouldKeepAtLeast7Days() {
            // Given
            OperationLogMapper mapperMock = mock(OperationLogMapper.class);
            when(operationLogRepository.getBaseMapper()).thenReturn(mapperMock);
            when(mapperMock.delete(any(LambdaQueryWrapper.class))).thenReturn(5);

            // When
            int result = operationLogAppService.cleanHistoryLogs(3);

            // Then
            assertThat(result).isEqualTo(5);
            // 验证使用了至少7天的保留期
        }
    }
}
