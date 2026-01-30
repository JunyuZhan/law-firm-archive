package com.lawfirm.application.archive.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.application.archive.dto.ArchiveLocationDTO;
import com.lawfirm.application.archive.dto.LocationCapacityDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import com.lawfirm.domain.archive.repository.ArchiveLocationRepository;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ArchiveLocationAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveLocationAppService 档案位置服务测试")
class ArchiveLocationAppServiceTest {

  private static final Long TEST_LOCATION_ID = 100L;

  @Mock private ArchiveLocationRepository locationRepository;

  @InjectMocks private ArchiveLocationAppService locationAppService;

  @Nested
  @DisplayName("查询库位测试")
  class QueryLocationTests {

    @Test
    @DisplayName("应该成功查询所有库位")
    void listLocations_shouldSuccess() {
      // Given
      ArchiveLocation location1 =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("1号库位")
              .room("A101")
              .cabinet("C01")
              .shelf("S01")
              .position("P01")
              .totalCapacity(100)
              .usedCapacity(50)
              .status("AVAILABLE")
              .build();

      ArchiveLocation location2 =
          ArchiveLocation.builder()
              .id(200L)
              .locationCode("LOC-002")
              .locationName("2号库位")
              .room("A101")
              .cabinet("C02")
              .totalCapacity(200)
              .usedCapacity(100)
              .status("AVAILABLE")
              .build();

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper = any(LambdaQueryWrapper.class);
      when(locationRepository.list(wrapper)).thenReturn(List.of(location1, location2));

      // When
      List<ArchiveLocationDTO> result = locationAppService.listLocations();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getLocationCode()).isEqualTo("LOC-001");
    }

    @Test
    @DisplayName("应该成功查询可用库位")
    void listAvailableLocations_shouldSuccess() {
      // Given
      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("1号库位")
              .status("AVAILABLE")
              .totalCapacity(100)
              .usedCapacity(50)
              .build();

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper = any(LambdaQueryWrapper.class);
      when(locationRepository.list(wrapper)).thenReturn(List.of(location));

      // When
      List<ArchiveLocationDTO> result = locationAppService.listAvailableLocations();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("应该成功获取库位详情")
    void getLocationById_shouldSuccess() {
      // Given
      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("1号库位")
              .room("A101")
              .cabinet("C01")
              .totalCapacity(100)
              .usedCapacity(50)
              .status("AVAILABLE")
              .build();

      when(locationRepository.getByIdOrThrow(eq(TEST_LOCATION_ID), anyString()))
          .thenReturn(location);

      // When
      ArchiveLocationDTO result = locationAppService.getLocationById(TEST_LOCATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLocationCode()).isEqualTo("LOC-001");
      assertThat(result.getAvailableCapacity()).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("创建库位测试")
  class CreateLocationTests {

    @Test
    @DisplayName("应该成功创建库位")
    void createLocation_shouldSuccess() {
      // Given
      ArchiveLocationDTO dto = new ArchiveLocationDTO();
      dto.setLocationCode("LOC-001");
      dto.setLocationName("1号库位");
      dto.setRoom("A101");
      dto.setCabinet("C01");
      dto.setShelf("S01");
      dto.setPosition("P01");
      dto.setTotalCapacity(100);
      dto.setStatus("AVAILABLE");

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper1 = any(LambdaQueryWrapper.class);
      when(locationRepository.count(wrapper1)).thenReturn(0L);
      when(locationRepository.save(any(ArchiveLocation.class)))
          .thenAnswer(
              invocation -> {
                ArchiveLocation location = invocation.getArgument(0);
                location.setId(TEST_LOCATION_ID);
                return true;
              });

      // When
      ArchiveLocationDTO result = locationAppService.createLocation(dto);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLocationCode()).isEqualTo("LOC-001");
      assertThat(result.getUsedCapacity()).isEqualTo(0);
      verify(locationRepository).save(any(ArchiveLocation.class));
    }

    @Test
    @DisplayName("应该失败当库位编码已存在")
    void createLocation_shouldFail_whenCodeExists() {
      // Given
      ArchiveLocationDTO dto = new ArchiveLocationDTO();
      dto.setLocationCode("LOC-001");

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper2 = any(LambdaQueryWrapper.class);
      when(locationRepository.count(wrapper2)).thenReturn(1L);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> locationAppService.createLocation(dto));
      assertThat(exception.getMessage()).contains("库位编码已存在");
    }

    @Test
    @DisplayName("应该使用默认状态当未指定")
    void createLocation_shouldUseDefaultStatus() {
      // Given
      ArchiveLocationDTO dto = new ArchiveLocationDTO();
      dto.setLocationCode("LOC-001");
      dto.setLocationName("1号库位");
      dto.setTotalCapacity(100);
      // 不设置status

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper3 = any(LambdaQueryWrapper.class);
      when(locationRepository.count(wrapper3)).thenReturn(0L);
      when(locationRepository.save(any(ArchiveLocation.class)))
          .thenAnswer(
              invocation -> {
                ArchiveLocation location = invocation.getArgument(0);
                location.setId(TEST_LOCATION_ID);
                return true;
              });

      // When
      ArchiveLocationDTO result = locationAppService.createLocation(dto);

      // Then
      assertThat(result).isNotNull();
      verify(locationRepository)
          .save(argThat(location -> "AVAILABLE".equals(location.getStatus())));
    }
  }

  @Nested
  @DisplayName("更新库位测试")
  class UpdateLocationTests {

    @Test
    @DisplayName("应该成功更新库位")
    void updateLocation_shouldSuccess() {
      // Given
      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("原名称")
              .room("A101")
              .totalCapacity(100)
              .usedCapacity(50)
              .status("AVAILABLE")
              .build();

      ArchiveLocationDTO dto = new ArchiveLocationDTO();
      dto.setLocationName("新名称");
      dto.setRoom("A102");
      dto.setTotalCapacity(200);
      dto.setStatus("FULL");

      when(locationRepository.getByIdOrThrow(eq(TEST_LOCATION_ID), anyString()))
          .thenReturn(location);
      when(locationRepository.updateById(any(ArchiveLocation.class))).thenReturn(true);

      // When
      ArchiveLocationDTO result = locationAppService.updateLocation(TEST_LOCATION_ID, dto);

      // Then
      assertThat(result).isNotNull();
      assertThat(location.getLocationName()).isEqualTo("新名称");
      assertThat(location.getRoom()).isEqualTo("A102");
      assertThat(location.getTotalCapacity()).isEqualTo(200);
      assertThat(location.getStatus()).isEqualTo("FULL");
      verify(locationRepository).updateById(location);
    }

    @Test
    @DisplayName("应该失败当库位不存在")
    void updateLocation_shouldFail_whenLocationNotExists() {
      // Given
      ArchiveLocationDTO dto = new ArchiveLocationDTO();

      when(locationRepository.getByIdOrThrow(eq(TEST_LOCATION_ID), anyString()))
          .thenThrow(new BusinessException("库位不存在"));

      // When & Then
      assertThrows(
          BusinessException.class, () -> locationAppService.updateLocation(TEST_LOCATION_ID, dto));
    }
  }

  @Nested
  @DisplayName("容量监控测试")
  class MonitorCapacityTests {

    @Test
    @DisplayName("应该成功监控库位容量")
    void monitorCapacity_shouldSuccess() {
      // Given
      ArchiveLocation location1 =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("1号库位")
              .totalCapacity(100)
              .usedCapacity(50)
              .status("AVAILABLE")
              .build();

      ArchiveLocation location2 =
          ArchiveLocation.builder()
              .id(200L)
              .locationCode("LOC-002")
              .locationName("2号库位")
              .totalCapacity(200)
              .usedCapacity(200)
              .status("FULL")
              .build();

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper = any(LambdaQueryWrapper.class);
      when(locationRepository.list(wrapper)).thenReturn(List.of(location1, location2));

      // When
      List<LocationCapacityDTO> result = locationAppService.monitorCapacity();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getUsageRate()).isEqualTo(50.0);
      assertThat(result.get(1).getUsageRate()).isEqualTo(100.0);
      assertThat(result.get(1).getStatus()).isEqualTo("FULL");
    }

    @Test
    @DisplayName("应该正确处理空容量")
    void monitorCapacity_shouldHandleZeroCapacity() {
      // Given
      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(TEST_LOCATION_ID)
              .locationCode("LOC-001")
              .locationName("1号库位")
              .totalCapacity(0)
              .usedCapacity(0)
              .status("AVAILABLE")
              .build();

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveLocation> wrapper = any(LambdaQueryWrapper.class);
      when(locationRepository.list(wrapper)).thenReturn(List.of(location));

      // When
      List<LocationCapacityDTO> result = locationAppService.monitorCapacity();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getUsageRate()).isEqualTo(0.0);
    }
  }
}
