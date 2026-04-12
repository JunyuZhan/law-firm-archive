package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.Fonds;
import com.archivesystem.service.FondsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class FondsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FondsService fondsService;

    @InjectMocks
    private FondsController fondsController;

    private ObjectMapper objectMapper;
    private Fonds testFonds;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fondsController).build();
        objectMapper = new ObjectMapper();

        testFonds = new Fonds();
        testFonds.setId(1L);
        testFonds.setFondsNo("QZ001");
        testFonds.setFondsName("测试全宗");
        testFonds.setFondsType("企业全宗");
        testFonds.setDescription("测试全宗描述");
        testFonds.setStatus("ACTIVE");
    }

    @Test
    void testCreate_Success() throws Exception {
        Fonds newFonds = new Fonds();
        newFonds.setFondsNo("QZ002");
        newFonds.setFondsName("新全宗");

        when(fondsService.create(any(Fonds.class))).thenReturn(testFonds);

        mockMvc.perform(post("/fonds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newFonds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.fondsNo").value("QZ001"));
    }

    @Test
    void testUpdate_Success() throws Exception {
        Fonds updateFonds = new Fonds();
        updateFonds.setFondsNo("QZ001");
        updateFonds.setFondsName("更新全宗");

        when(fondsService.update(eq(1L), any(Fonds.class))).thenReturn(testFonds);

        mockMvc.perform(put("/fonds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateFonds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(fondsService.getById(1L)).thenReturn(testFonds);

        mockMvc.perform(get("/fonds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.fondsNo").value("QZ001"))
                .andExpect(jsonPath("$.data.fondsName").value("测试全宗"));
    }

    @Test
    void testList_Success() throws Exception {
        Fonds fonds2 = new Fonds();
        fonds2.setId(2L);
        fonds2.setFondsNo("QZ002");
        fonds2.setFondsName("第二个全宗");

        when(fondsService.list()).thenReturn(Arrays.asList(testFonds, fonds2));

        mockMvc.perform(get("/fonds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].fondsNo").value("QZ001"))
                .andExpect(jsonPath("$.data[1].fondsNo").value("QZ002"));
    }

    @Test
    void testQuery_Success() throws Exception {
        PageResult<Fonds> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testFonds));

        when(fondsService.query(any(), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/fonds/page")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testQuery_WithKeyword() throws Exception {
        PageResult<Fonds> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testFonds));

        when(fondsService.query(eq("测试"), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/fonds/page")
                        .param("keyword", "测试")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(fondsService).query("测试", 1, 20);
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(fondsService).delete(1L);

        mockMvc.perform(delete("/fonds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(fondsService).delete(1L);
    }

    @Test
    void testStatistics_Success() throws Exception {
        when(fondsService.getById(1L)).thenReturn(testFonds);
        when(fondsService.countArchives(1L)).thenReturn(500L);

        mockMvc.perform(get("/fonds/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveCount").value(500));
    }
}
