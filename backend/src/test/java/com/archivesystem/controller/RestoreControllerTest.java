package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.RestoreMaintenanceStatus;
import com.archivesystem.entity.RestoreJob;
import com.archivesystem.service.BackupService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class RestoreControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private RestoreController restoreController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restoreController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetMaintenanceStatus() throws Exception {
        when(backupService.getRestoreMaintenanceStatus()).thenReturn(
                RestoreMaintenanceStatus.builder().enabled(true).build()
        );

        mockMvc.perform(get("/restores/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void testGetRestoreStatusCompat() throws Exception {
        when(backupService.getRestoreMaintenanceStatus()).thenReturn(
                RestoreMaintenanceStatus.builder().enabled(false).build()
        );

        mockMvc.perform(get("/restores/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void testUpdateMaintenanceStatus() throws Exception {
        when(backupService.setRestoreMaintenanceMode(true)).thenReturn(
                RestoreMaintenanceStatus.builder().enabled(true).build()
        );

        mockMvc.perform(put("/restores/maintenance").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("维护模式已更新"));
    }

    @Test
    void testGetBackupSets() throws Exception {
        when(backupService.getBackupSets(eq(1L))).thenReturn(List.of(
                BackupSetResponse.builder().backupSetName("BK-001").build()
        ));

        mockMvc.perform(get("/restores/sets").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].backupSetName").value("BK-001"));
    }

    @Test
    void testRunRestore() throws Exception {
        when(backupService.runRestore(any())).thenReturn(RestoreJob.builder().restoreNo("RS-001").build());

        mockMvc.perform(post("/restores/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetId", 1,
                                "backupSetName", "BK-001",
                                "restoreDatabase", true,
                                "restoreFiles", true,
                                "restoreConfig", true,
                                "rebuildIndex", true,
                                "exitMaintenanceAfterSuccess", true,
                                "confirmationText", "RESTORE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("恢复任务已启动"));
    }

    @Test
    void testGetRestoreJobs() throws Exception {
        when(backupService.getRestoreJobs(eq(1), eq(20))).thenReturn(PageResult.empty());

        mockMvc.perform(get("/restores/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
