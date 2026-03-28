package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.repository.ArchiveLocationMapper;
import com.archivesystem.service.impl.LocationServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LocationService测试类.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private ArchiveLocationMapper locationMapper;

    @InjectMocks
    private LocationServiceImpl locationService;

    @Test
    void testCreate_Success() {
        // Given
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode("LOC001");
        location.setLocationName("位置1");
        when(locationMapper.selectByCode("LOC001")).thenReturn(null);
        when(locationMapper.insert(any())).thenReturn(1);

        // When
        ArchiveLocation result = locationService.create(location);

        // Then
        assertNotNull(result);
        assertEquals(ArchiveLocation.STATUS_AVAILABLE, result.getStatus());
        assertEquals(0, result.getTotalCapacity());
        assertEquals(0, result.getUsedCapacity());
        verify(locationMapper).insert(any());
    }

    @Test
    void testCreate_DuplicateCode_ShouldThrowException() {
        // Given
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode("LOC001");
        ArchiveLocation existing = new ArchiveLocation();
        when(locationMapper.selectByCode("LOC001")).thenReturn(existing);

        // When & Then
        assertThrows(BusinessException.class, () -> locationService.create(location));
    }

    @Test
    void testCreate_WithCustomValues() {
        // Given
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode("LOC002");
        location.setTotalCapacity(100);
        location.setUsedCapacity(50);
        location.setStatus(ArchiveLocation.STATUS_FULL);
        when(locationMapper.selectByCode("LOC002")).thenReturn(null);
        when(locationMapper.insert(any())).thenReturn(1);

        // When
        ArchiveLocation result = locationService.create(location);

        // Then
        assertEquals(100, result.getTotalCapacity());
        assertEquals(50, result.getUsedCapacity());
        assertEquals(ArchiveLocation.STATUS_FULL, result.getStatus());
    }

    @Test
    void testUpdate_Success() {
        // Given
        Long id = 1L;
        ArchiveLocation existing = new ArchiveLocation();
        existing.setId(id);
        existing.setLocationCode("LOC001");
        existing.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(existing);
        when(locationMapper.updateById(any())).thenReturn(1);

        ArchiveLocation update = new ArchiveLocation();
        update.setLocationName("新名称");
        update.setRoomName("新房间");

        // When
        ArchiveLocation result = locationService.update(id, update);

        // Then
        assertEquals("新名称", result.getLocationName());
        assertEquals("新房间", result.getRoomName());
        verify(locationMapper).updateById(any());
    }

    @Test
    void testUpdate_ChangeCode_Success() {
        // Given
        Long id = 1L;
        ArchiveLocation existing = new ArchiveLocation();
        existing.setId(id);
        existing.setLocationCode("LOC001");
        existing.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(existing);
        when(locationMapper.selectByCode("LOC002")).thenReturn(null);
        when(locationMapper.updateById(any())).thenReturn(1);

        ArchiveLocation update = new ArchiveLocation();
        update.setLocationCode("LOC002");

        // When
        ArchiveLocation result = locationService.update(id, update);

        // Then
        assertEquals("LOC002", result.getLocationCode());
    }

    @Test
    void testUpdate_DuplicateCode_ShouldThrowException() {
        // Given
        Long id = 1L;
        ArchiveLocation existing = new ArchiveLocation();
        existing.setId(id);
        existing.setLocationCode("LOC001");
        existing.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(existing);
        
        ArchiveLocation duplicate = new ArchiveLocation();
        when(locationMapper.selectByCode("LOC002")).thenReturn(duplicate);

        ArchiveLocation update = new ArchiveLocation();
        update.setLocationCode("LOC002");

        // When & Then
        assertThrows(BusinessException.class, () -> locationService.update(id, update));
    }

    @Test
    void testDelete_Success() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(0);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.delete(id);

        // Then
        verify(locationMapper).updateById(argThat(l -> Boolean.TRUE.equals(l.getDeleted())));
    }

    @Test
    void testDelete_HasArchives_ShouldThrowException() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(5);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);

        // When & Then
        assertThrows(BusinessException.class, () -> locationService.delete(id));
    }

    @Test
    void testGetById_Success() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);

        // When
        ArchiveLocation result = locationService.getById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void testGetById_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        when(locationMapper.selectById(id)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> locationService.getById(id));
    }

    @Test
    void testGetById_Deleted_ShouldThrowException() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setDeleted(true);
        when(locationMapper.selectById(id)).thenReturn(location);

        // When & Then
        assertThrows(NotFoundException.class, () -> locationService.getById(id));
    }

    @Test
    void testGetByCode() {
        // Given
        String code = "LOC001";
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode(code);
        when(locationMapper.selectByCode(code)).thenReturn(location);

        // When
        ArchiveLocation result = locationService.getByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getLocationCode());
    }

    @Test
    void testGetList_WithFilters() {
        // Given
        List<ArchiveLocation> locations = Arrays.asList(new ArchiveLocation());
        Page<ArchiveLocation> page = new Page<>(1, 10);
        page.setRecords(locations);
        page.setTotal(1);
        when(locationMapper.selectPage(any(), any())).thenReturn(page);

        // When
        PageResult<ArchiveLocation> result = locationService.getList(
                "房间1", ArchiveLocation.STATUS_AVAILABLE, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetList_WithoutFilters() {
        // Given
        List<ArchiveLocation> locations = Arrays.asList(new ArchiveLocation());
        Page<ArchiveLocation> page = new Page<>(1, 10);
        page.setRecords(locations);
        page.setTotal(1);
        when(locationMapper.selectPage(any(), any())).thenReturn(page);

        // When
        PageResult<ArchiveLocation> result = locationService.getList(null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetAll() {
        // Given
        List<ArchiveLocation> locations = Arrays.asList(new ArchiveLocation());
        when(locationMapper.selectList(any())).thenReturn(locations);

        // When
        List<ArchiveLocation> result = locationService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAvailable() {
        // Given
        List<ArchiveLocation> locations = Arrays.asList(new ArchiveLocation());
        when(locationMapper.selectAvailable()).thenReturn(locations);

        // When
        List<ArchiveLocation> result = locationService.getAvailable();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetByRoom() {
        // Given
        String roomName = "房间1";
        List<ArchiveLocation> locations = Arrays.asList(new ArchiveLocation());
        when(locationMapper.selectByRoom(roomName)).thenReturn(locations);

        // When
        List<ArchiveLocation> result = locationService.getByRoom(roomName);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetRoomNames() {
        // Given
        List<String> roomNames = Arrays.asList("房间1", "房间2");
        when(locationMapper.selectRoomNames()).thenReturn(roomNames);

        // When
        List<String> result = locationService.getRoomNames();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testUpdateUsage_Increase() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(5);
        location.setTotalCapacity(100);
        location.setStatus(ArchiveLocation.STATUS_AVAILABLE);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, 10);

        // Then
        verify(locationMapper).updateById(argThat(l -> l.getUsedCapacity() == 15));
    }

    @Test
    void testUpdateUsage_Decrease() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(10);
        location.setTotalCapacity(100);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, -5);

        // Then
        verify(locationMapper).updateById(argThat(l -> l.getUsedCapacity() == 5));
    }

    @Test
    void testUpdateUsage_BelowZero_ShouldSetToZero() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(5);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, -10);

        // Then
        verify(locationMapper).updateById(argThat(l -> l.getUsedCapacity() == 0));
    }

    @Test
    void testUpdateUsage_ReachCapacity_ShouldSetFull() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(90);
        location.setTotalCapacity(100);
        location.setStatus(ArchiveLocation.STATUS_AVAILABLE);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, 10);

        // Then
        verify(locationMapper).updateById(argThat(l -> 
                ArchiveLocation.STATUS_FULL.equals(l.getStatus())));
    }

    @Test
    void testUpdateUsage_BelowCapacity_ShouldSetAvailable() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(100);
        location.setTotalCapacity(100);
        location.setStatus(ArchiveLocation.STATUS_FULL);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, -10);

        // Then
        verify(locationMapper).updateById(argThat(l -> 
                ArchiveLocation.STATUS_AVAILABLE.equals(l.getStatus())));
    }

    @Test
    void testUpdateUsage_NullUsedCapacity_ShouldHandleGracefully() {
        // Given
        Long id = 1L;
        ArchiveLocation location = new ArchiveLocation();
        location.setId(id);
        location.setUsedCapacity(null);
        location.setTotalCapacity(100);
        location.setDeleted(false);
        when(locationMapper.selectById(id)).thenReturn(location);
        when(locationMapper.updateById(any())).thenReturn(1);

        // When
        locationService.updateUsage(id, 5);

        // Then
        verify(locationMapper).updateById(argThat(l -> l.getUsedCapacity() == 5));
    }
}
