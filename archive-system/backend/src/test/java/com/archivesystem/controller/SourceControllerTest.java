package com.archivesystem.controller;

import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SourceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExternalSourceMapper externalSourceMapper;

    @InjectMocks
    private SourceController sourceController;

    private ObjectMapper objectMapper;
    private ExternalSource testSource;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sourceController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testSource = new ExternalSource();
        testSource.setId(1L);
        testSource.setSourceCode("LAW_FIRM");
        testSource.setSourceName("律所管理系统");
        testSource.setApiUrl("http://lawfirm.example.com");
        testSource.setApiKey("api-key-123");
        testSource.setEnabled(true);
        testSource.setDeleted(false);
        testSource.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testList() throws Exception {
        when(externalSourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(testSource));

        mockMvc.perform(get("/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].sourceCode").value("LAW_FIRM"));
    }

    @Test
    void testGetById_Found() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);

        mockMvc.perform(get("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.sourceCode").value("LAW_FIRM"))
                .andExpect(jsonPath("$.data.sourceName").value("律所管理系统"));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/sources/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testGetById_Deleted() throws Exception {
        testSource.setDeleted(true);
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);

        mockMvc.perform(get("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testCreate() throws Exception {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setApiUrl("http://court.example.com");

        when(externalSourceMapper.insert(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("创建成功"));

        verify(externalSourceMapper).insert(any(ExternalSource.class));
    }

    @Test
    void testUpdate() throws Exception {
        ExternalSource updateSource = new ExternalSource();
        updateSource.setSourceName("更新后的名称");
        updateSource.setApiUrl("http://new-url.example.com");

        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("更新成功"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getId().equals(1L)));
    }

    @Test
    void testToggle_Enable() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已启用"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getEnabled()));
    }

    @Test
    void testToggle_Disable() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1/toggle")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已禁用"));
    }

    @Test
    void testToggle_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(put("/sources/999/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testTestConnection() throws Exception {
        mockMvc.perform(post("/sources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("连接测试成功"));
    }

    @Test
    void testDelete() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(delete("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getDeleted()));
    }

    @Test
    void testDelete_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(delete("/sources/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(externalSourceMapper, never()).updateById(any());
    }
}
