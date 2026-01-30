package com.lawfirm.application.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateCareerLevelCommand;
import com.lawfirm.application.hr.dto.CareerLevelDTO;
import com.lawfirm.application.hr.dto.CareerLevelQueryDTO;
import com.lawfirm.application.hr.dto.PromotionApplicationDTO;
import com.lawfirm.application.hr.dto.PromotionQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.domain.hr.repository.CareerLevelRepository;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** PromotionAppService 单元测试 测试晋升管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionAppService 晋升服务测试")
class PromotionAppServiceTest {

  private static final Long TEST_LEVEL_ID = 100L;
  private static final Long TEST_EMPLOYEE_ID = 200L;

  @Mock private CareerLevelRepository careerLevelRepository;

  @Mock private EmployeeRepository employeeRepository;

  @InjectMocks private PromotionAppService promotionAppService;

  @Nested
  @DisplayName("职级管理测试")
  class CareerLevelManagementTests {

    @Test
    @DisplayName("应该成功创建职级")
    void createCareerLevel_shouldSuccess() {
      // Given
      CreateCareerLevelCommand command = new CreateCareerLevelCommand();
      command.setLevelCode("P5");
      command.setLevelName("高级律师");
      command.setLevelOrder(5);
      command.setCategory("LAWYER");
      command.setMinWorkYears(5);
      command.setSalaryMin(new BigDecimal("20000"));
      command.setSalaryMax(new BigDecimal("30000"));

      when(careerLevelRepository.save(any(CareerLevel.class)))
          .thenAnswer(
              invocation -> {
                CareerLevel level = invocation.getArgument(0);
                level.setId(TEST_LEVEL_ID);
                return true;
              });

      // When
      CareerLevelDTO result = promotionAppService.createCareerLevel(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLevelCode()).isEqualTo("P5");
      assertThat(result.getLevelName()).isEqualTo("高级律师");
      verify(careerLevelRepository).save(any(CareerLevel.class));
    }

    @Test
    @DisplayName("应该成功更新职级")
    void updateCareerLevel_shouldSuccess() {
      // Given
      CareerLevel level =
          CareerLevel.builder().id(TEST_LEVEL_ID).levelCode("P5").levelName("原名称").build();

      CreateCareerLevelCommand command = new CreateCareerLevelCommand();
      command.setLevelName("新名称");

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);
      when(careerLevelRepository.updateById(any(CareerLevel.class))).thenReturn(true);

      // When
      CareerLevelDTO result = promotionAppService.updateCareerLevel(TEST_LEVEL_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(level.getLevelName()).isEqualTo("新名称");
      verify(careerLevelRepository).updateById(level);
    }

    @Test
    @DisplayName("职级不存在应该失败")
    void updateCareerLevel_shouldFail_whenNotFound() {
      // Given
      CreateCareerLevelCommand command = new CreateCareerLevelCommand();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> promotionAppService.updateCareerLevel(TEST_LEVEL_ID, command));
      assertThat(exception.getMessage()).contains("职级不存在");
    }

    @Test
    @DisplayName("有员工使用的职级不能删除")
    void deleteCareerLevel_shouldFail_whenHasEmployees() {
      // Given
      CareerLevel level = CareerLevel.builder().id(TEST_LEVEL_ID).levelCode("P5").build();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);
      when(employeeRepository.countByLevel("P5")).thenReturn(5L);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> promotionAppService.deleteCareerLevel(TEST_LEVEL_ID));
      assertThat(exception.getMessage()).contains("员工正在使用");
    }

    @Test
    @DisplayName("应该成功删除无员工使用的职级")
    void deleteCareerLevel_shouldSuccess_whenNoEmployees() {
      // Given
      CareerLevel level = CareerLevel.builder().id(TEST_LEVEL_ID).levelCode("P5").build();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);
      when(employeeRepository.countByLevel("P5")).thenReturn(0L);
      when(careerLevelRepository.removeById(TEST_LEVEL_ID)).thenReturn(true);

      // When
      promotionAppService.deleteCareerLevel(TEST_LEVEL_ID);

      // Then
      verify(careerLevelRepository).removeById(TEST_LEVEL_ID);
    }

    @Test
    @DisplayName("应该成功启用职级")
    void enableCareerLevel_shouldSuccess() {
      // Given
      CareerLevel level = CareerLevel.builder().id(TEST_LEVEL_ID).status("INACTIVE").build();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);
      when(careerLevelRepository.updateById(any(CareerLevel.class))).thenReturn(true);

      // When
      promotionAppService.enableCareerLevel(TEST_LEVEL_ID);

      // Then
      assertThat(level.getStatus()).isEqualTo("ACTIVE");
      verify(careerLevelRepository).updateById(level);
    }

    @Test
    @DisplayName("应该成功停用职级")
    void disableCareerLevel_shouldSuccess() {
      // Given
      CareerLevel level = CareerLevel.builder().id(TEST_LEVEL_ID).status("ACTIVE").build();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);
      when(careerLevelRepository.updateById(any(CareerLevel.class))).thenReturn(true);

      // When
      promotionAppService.disableCareerLevel(TEST_LEVEL_ID);

      // Then
      assertThat(level.getStatus()).isEqualTo("INACTIVE");
      verify(careerLevelRepository).updateById(level);
    }
  }

  @Nested
  @DisplayName("查询职级测试")
  class QueryCareerLevelTests {

    @Test
    @DisplayName("应该成功分页查询职级列表")
    void listCareerLevels_shouldSuccess() {
      // Given
      CareerLevelQueryDTO query = new CareerLevelQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      CareerLevel level =
          CareerLevel.builder()
              .id(TEST_LEVEL_ID)
              .levelCode("P5")
              .levelName("高级律师")
              .category("LAWYER")
              .status("ACTIVE")
              .build();

      Page<CareerLevel> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(level));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<CareerLevel> pageParam = any(Page.class);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<CareerLevel> wrapper = any(LambdaQueryWrapper.class);
      when(careerLevelRepository.page(pageParam, wrapper)).thenReturn(page);

      // When
      PageResult<CareerLevelDTO> result = promotionAppService.listCareerLevels(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getLevelCode()).isEqualTo("P5");
    }

    @Test
    @DisplayName("应该成功获取职级详情")
    void getCareerLevelById_shouldSuccess() {
      // Given
      CareerLevel level =
          CareerLevel.builder().id(TEST_LEVEL_ID).levelCode("P5").levelName("高级律师").build();

      when(careerLevelRepository.getById(TEST_LEVEL_ID)).thenReturn(level);

      // When
      CareerLevelDTO result = promotionAppService.getCareerLevelById(TEST_LEVEL_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLevelCode()).isEqualTo("P5");
    }

    @Test
    @DisplayName("应该成功按类别查询职级")
    void getCareerLevelsByCategory_shouldSuccess() {
      // Given
      CareerLevel level = CareerLevel.builder().id(TEST_LEVEL_ID).category("LAWYER").build();

      when(careerLevelRepository.findByCategory("LAWYER"))
          .thenReturn(Collections.singletonList(level));

      // When
      List<CareerLevelDTO> result = promotionAppService.getCareerLevelsByCategory("LAWYER");

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCategory()).isEqualTo("LAWYER");
    }
  }

  @Nested
  @DisplayName("晋升申请测试")
  class PromotionApplicationTests {

    @Test
    @DisplayName("晋升申请功能未开放应该抛出异常")
    void submitPromotionApplication_shouldFail_whenNotAvailable() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  promotionAppService.submitPromotionApplication(
                      TEST_EMPLOYEE_ID, TEST_LEVEL_ID, "原因"));
      assertThat(exception.getMessage()).contains("正在开发中");
    }

    @Test
    @DisplayName("查询晋升申请应该返回空列表")
    void listPromotionApplications_shouldReturnEmpty() {
      // Given
      PromotionQueryDTO query = new PromotionQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<PromotionApplicationDTO> result =
          promotionAppService.listPromotionApplications(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    @DisplayName("统计待审批数量应该返回0")
    void countPendingApplications_shouldReturnZero() {
      // When
      long count = promotionAppService.countPendingApplications();

      // Then
      assertThat(count).isEqualTo(0L);
    }
  }
}
