package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.GlobalExceptionHandler;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.service.AlertService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocationService locationService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private LocationController locationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController)
                .setControllerAdvice(new GlobalExceptionHandler(alertService))
                .build();
        objectMapper = new ObjectMapper();

        when(locationService.getList(any(), any(), any(), anyInt(), anyInt()))
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
    void testList_ShouldHideRemarks() throws Exception {
        ArchiveLocation location = new ArchiveLocation();
        location.setId(1L);
        location.setLocationCode("LOC001");
        location.setLocationName("档案室A");
        location.setRemarks("内部备注");
        when(locationService.getList(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 20, 1, List.of(location)));

        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].locationCode").value("LOC001"))
                .andExpect(jsonPath("$.data.records[0].remarks").doesNotExist());
    }

    @Test
    void testGetById() throws Exception {
        ArchiveLocation location = new ArchiveLocation();
        location.setId(1L);
        location.setLocationCode("LOC001");
        location.setLocationName("档案室A");
        location.setCreatedBy(7L);
        location.setUpdatedBy(8L);
        location.setDeleted(false);
        when(locationService.getById(1L)).thenReturn(location);

        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.locationCode").value("LOC001"))
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    @Test
    void testGetAvailable_ShouldHideRemarks() throws Exception {
        ArchiveLocation location = new ArchiveLocation();
        location.setId(1L);
        location.setLocationCode("LOC001");
        location.setLocationName("档案室A");
        location.setRoomName("一号库房");
        location.setShelfNo("03");
        location.setArea("A区");
        location.setLayerNo("2");
        location.setTotalCapacity(100);
        location.setUsedCapacity(80);
        location.setStatus("FULL");
        location.setRemarks("内部备注");
        when(locationService.getAvailable()).thenReturn(List.of(location));

        mockMvc.perform(get("/locations/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].locationCode").doesNotExist())
                .andExpect(jsonPath("$.data[0].locationName").value("档案室A"))
                .andExpect(jsonPath("$.data[0].roomName").value("一号库房"))
                .andExpect(jsonPath("$.data[0].shelfNo").value("03"))
                .andExpect(jsonPath("$.data[0].area").doesNotExist())
                .andExpect(jsonPath("$.data[0].layerNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].totalCapacity").doesNotExist())
                .andExpect(jsonPath("$.data[0].usedCapacity").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].remarks").doesNotExist());
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
        created.setRoomName("一号库房");
        created.setStatus("AVAILABLE");
        created.setRemarks("内部备注");
        created.setCreatedBy(7L);
        
        when(locationService.create(any(ArchiveLocation.class))).thenReturn(created);

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.locationCode").value("LOC001"))
                .andExpect(jsonPath("$.data.locationName").value("档案室A"))
                .andExpect(jsonPath("$.data.roomName").value("一号库房"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.remarks").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist());
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
        updated.setRoomName("二号库房");
        updated.setStatus("FULL");
        updated.setRemarks("内部备注");
        updated.setUpdatedBy(8L);
        
        when(locationService.update(eq(1L), any(ArchiveLocation.class))).thenReturn(updated);

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.locationName").value("档案室B"))
                .andExpect(jsonPath("$.data.roomName").value("二号库房"))
                .andExpect(jsonPath("$.data.status").value("FULL"))
                .andExpect(jsonPath("$.data.remarks").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist());
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(locationService).delete(1L);

        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
