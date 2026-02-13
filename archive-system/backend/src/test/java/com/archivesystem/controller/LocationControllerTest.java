package com.archivesystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private LocationController locationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("位置不存在"));
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
        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("501"))
                .andExpect(jsonPath("$.message").value("功能尚未实现"));
    }
}
