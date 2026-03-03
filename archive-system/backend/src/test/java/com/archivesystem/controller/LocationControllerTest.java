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
        ArchiveLocation location = new ArchiveLocation();
        location.setId(1L);
        location.setLocationCode("LOC001");
        location.setLocationName("档案室A");
        when(locationService.getById(1L)).thenReturn(location);

        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.locationCode").value("LOC001"));
    }

    @Test
    void testCreate() throws Exception {
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode("LOC001");
        location.setLocationName("档案室A");
        
        ArchiveLocation created = new ArchiveLocation();
        created.setId(1L);
        created.setLocationCode("LOC001");
        created.setLocationName("档案室A");
        
        when(locationService.create(any(ArchiveLocation.class))).thenReturn(created);

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testUpdate() throws Exception {
        ArchiveLocation location = new ArchiveLocation();
        location.setLocationCode("LOC001");
        location.setLocationName("档案室B");
        
        ArchiveLocation updated = new ArchiveLocation();
        updated.setId(1L);
        updated.setLocationCode("LOC001");
        updated.setLocationName("档案室B");
        
        when(locationService.update(eq(1L), any(ArchiveLocation.class))).thenReturn(updated);

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.locationName").value("档案室B"));
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(locationService).delete(1L);

        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
