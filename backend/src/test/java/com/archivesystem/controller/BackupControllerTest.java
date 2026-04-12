package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupTargetResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private BackupController backupController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backupController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetOverview() throws Exception {
        when(backupService.getOverview()).thenReturn(BackupOverview.builder().enabledTargetCount(1).build());

        mockMvc.perform(get("/backups/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.enabledTargetCount").value(1));
    }

    @Test
    void testGetTargets() throws Exception {
        when(backupService.getTargets()).thenReturn(List.of(BackupTargetResponse.builder().id(1L).name("NAS").build()));

        mockMvc.perform(get("/backups/targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("NAS"));
    }

    @Test
    void testCreateTarget() throws Exception {
        when(backupService.createTarget(any())).thenReturn(BackupTargetResponse.builder().id(1L).name("NAS").build());

        mockMvc.perform(post("/backups/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", "NAS");
                            put("targetType", "LOCAL");
                            put("enabled", true);
                            put("localPath", "/tmp/backups");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("创建成功"));
    }

    @Test
    void testVerifyTarget() throws Exception {
        when(backupService.verifyTarget(1L)).thenReturn(BackupTargetResponse.builder().id(1L).verifyStatus("SUCCESS").build());

        mockMvc.perform(post("/backups/targets/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verifyStatus").value("SUCCESS"));
    }

    @Test
    void testGetBackupSets() throws Exception {
        when(backupService.getBackupSets(eq(1L))).thenReturn(List.of(com.archivesystem.dto.backup.BackupSetResponse.builder()
                .backupSetName("BK-20260330150000-aaaa1111")
                .verifyStatus("READY")
                .build()));

        mockMvc.perform(get("/backups/sets").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].backupSetName").value("BK-20260330150000-aaaa1111"));
    }

    @Test
    void testDeleteTarget() throws Exception {
        doNothing().when(backupService).deleteTarget(1L);

        mockMvc.perform(delete("/backups/targets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    void testGetBackupJobs() throws Exception {
        when(backupService.getBackupJobs(eq(1), eq(20))).thenReturn(PageResult.empty());

        mockMvc.perform(get("/backups/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testRunBackup() throws Exception {
        when(backupService.runManualBackup(1L)).thenReturn(new com.archivesystem.entity.BackupJob());

        mockMvc.perform(post("/backups/run").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("备份任务已启动"));
    }
}
