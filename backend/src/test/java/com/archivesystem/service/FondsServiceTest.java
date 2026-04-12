package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Fonds;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.FondsMapper;
import com.archivesystem.service.impl.FondsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class FondsServiceTest {

    @Mock
    private FondsMapper fondsMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private FondsServiceImpl fondsService;

    private Fonds testFonds;

    @BeforeEach
    void setUp() {
        testFonds = new Fonds();
        testFonds.setId(1L);
        testFonds.setFondsNo("F001");
        testFonds.setFondsName("律所档案");
        testFonds.setFondsType("LAW_FIRM");
        testFonds.setDescription("律师事务所档案全宗");
        testFonds.setStatus(Fonds.STATUS_ACTIVE);
    }

    @Test
    void testCreateFonds_Success() {
        when(fondsMapper.selectByFondsNo("F001")).thenReturn(null);
        when(fondsMapper.insert(any(Fonds.class))).thenReturn(1);

        Fonds result = fondsService.create(testFonds);

        assertNotNull(result);
        assertEquals(Fonds.STATUS_ACTIVE, result.getStatus());
        verify(fondsMapper).insert(any(Fonds.class));
    }

    @Test
    void testCreateFonds_FondsNoExists() {
        when(fondsMapper.selectByFondsNo("F001")).thenReturn(testFonds);

        assertThrows(BusinessException.class, () -> fondsService.create(testFonds));
    }

    @Test
    void testGetById_Success() {
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);

        Fonds result = fondsService.getById(1L);

        assertNotNull(result);
        assertEquals("F001", result.getFondsNo());
    }

    @Test
    void testGetById_NotFound() {
        when(fondsMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> fondsService.getById(999L));
    }

    @Test
    void testGetByFondsNo() {
        when(fondsMapper.selectByFondsNo("F001")).thenReturn(testFonds);

        Fonds result = fondsService.getByFondsNo("F001");

        assertNotNull(result);
        assertEquals("律所档案", result.getFondsName());
    }

    @Test
    void testList() {
        List<Fonds> fondsList = Arrays.asList(testFonds);
        when(fondsMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Fonds>>any())).thenReturn(fondsList);

        List<Fonds> result = fondsService.list();

        assertEquals(1, result.size());
        assertEquals("F001", result.get(0).getFondsNo());
    }

    @Test
    void testQuery() {
        List<Fonds> records = Arrays.asList(testFonds);
        Page<Fonds> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);

        when(fondsMapper.selectPage(
                ArgumentMatchers.<Page<Fonds>>any(),
                ArgumentMatchers.<LambdaQueryWrapper<Fonds>>any()))
                .thenReturn(page);

        PageResult<Fonds> result = fondsService.query(null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testDeleteFonds_Success() {
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(fondsMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> fondsService.delete(1L));

        verify(fondsMapper).deleteById(1L);
    }

    @Test
    void testDeleteFonds_HasArchives() {
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(archiveMapper.selectCount(any())).thenReturn(5L);

        assertThrows(BusinessException.class, () -> fondsService.delete(1L));
    }

    @Test
    void testCountArchives() {
        when(archiveMapper.selectCount(any())).thenReturn(10L);

        long count = fondsService.countArchives(1L);

        assertEquals(10L, count);
    }

    @Test
    void testUpdateFonds_Success() {
        Fonds updateData = new Fonds();
        updateData.setFondsNo("F001"); // 同一全宗号
        updateData.setFondsName("更新名称");
        updateData.setFondsType("UPDATED");
        updateData.setDescription("更新描述");
        updateData.setStatus(Fonds.STATUS_ACTIVE);
        
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(fondsMapper.updateById(any(Fonds.class))).thenReturn(1);

        Fonds result = fondsService.update(1L, updateData);

        assertNotNull(result);
        verify(fondsMapper).updateById(any(Fonds.class));
    }

    @Test
    void testUpdateFonds_NotFound() {
        when(fondsMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> fondsService.update(999L, testFonds));
    }

    @Test
    void testUpdateFonds_FondsNoExistsForOther() {
        Fonds otherFonds = new Fonds();
        otherFonds.setId(2L);
        otherFonds.setFondsNo("F002");
        
        Fonds updateData = new Fonds();
        updateData.setFondsNo("F002");
        
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(fondsMapper.selectByFondsNo("F002")).thenReturn(otherFonds);

        assertThrows(BusinessException.class, () -> fondsService.update(1L, updateData));
    }

    @Test
    void testUpdateFonds_ChangeFondsNoSameId() {
        Fonds sameFonds = new Fonds();
        sameFonds.setId(1L);
        sameFonds.setFondsNo("F999");
        
        Fonds updateData = new Fonds();
        updateData.setFondsNo("F999");
        updateData.setFondsName("更新");
        updateData.setStatus(Fonds.STATUS_ACTIVE);
        
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(fondsMapper.selectByFondsNo("F999")).thenReturn(sameFonds);
        when(fondsMapper.updateById(any(Fonds.class))).thenReturn(1);

        Fonds result = fondsService.update(1L, updateData);

        assertNotNull(result);
    }

    @Test
    void testQueryWithKeyword() {
        List<Fonds> records = Arrays.asList(testFonds);
        Page<Fonds> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);

        when(fondsMapper.selectPage(
                ArgumentMatchers.<Page<Fonds>>any(),
                ArgumentMatchers.<LambdaQueryWrapper<Fonds>>any()))
                .thenReturn(page);

        PageResult<Fonds> result = fondsService.query("律所", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testDeleteFonds_NotFound() {
        when(fondsMapper.selectById(999L)).thenReturn(null);

        // 不应该抛出异常
        assertDoesNotThrow(() -> fondsService.delete(999L));
        
        verify(fondsMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetByFondsNo_NotFound() {
        when(fondsMapper.selectByFondsNo("NOTEXIST")).thenReturn(null);

        Fonds result = fondsService.getByFondsNo("NOTEXIST");

        assertNull(result);
    }
}
