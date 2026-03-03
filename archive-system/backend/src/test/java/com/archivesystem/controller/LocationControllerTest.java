package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.GlobalExceptionHandler;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        when(locationService.getList(any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 20, 0, Collections.emptyList()));
    }

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testGetById() throws Exception {
        when(locationService.getById(1L)).thenThrow(NotFoundException.of("位置", 1L));

        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void testCreate() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "档案室A");
        data.put("floor", 1);

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("501"))
                .andExpect(jsonPath("$.message").value("功能尚未实现"));
    }

    @Test
    void testUpdate() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "档案室B");

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("501"))
                .andExpect(jsonPath("$.message").value("功能尚未实现"));
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(locationService).delete(1L);

        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
