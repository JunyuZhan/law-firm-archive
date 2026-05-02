package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.GlobalExceptionHandler;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
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
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor methodValidationPostProcessor = new MethodValidationPostProcessor();
        methodValidationPostProcessor.setValidator(validator);
        Object validatedController = methodValidationPostProcessor.postProcessAfterInitialization(
                restoreController, "restoreController");

        mockMvc = MockMvcBuilders.standaloneSetup(validatedController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler(null))
                .build();
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
                BackupSetResponse.builder()
                        .backupSetName("BK-001")
                        .backupSetPath("/private/backups/BK-001")
                        .displayPath("smb://192.168.50.5/archive/BK-001")
                        .databaseRestorable(true)
                        .filesRestorable(false)
                        .configRestorable(true)
                        .verifyMessage("备份集完整，可用于恢复")
                        .build()
        ));

        mockMvc.perform(get("/restores/sets").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].backupSetName").value("BK-001"))
                .andExpect(jsonPath("$.data[0].displayPath").value("smb://192.168.50.5/archive/BK-001"))
                .andExpect(jsonPath("$.data[0].databaseRestorable").value(true))
                .andExpect(jsonPath("$.data[0].filesRestorable").value(false))
                .andExpect(jsonPath("$.data[0].configRestorable").value(true))
                .andExpect(jsonPath("$.data[0].backupNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].backupSetPath").doesNotExist())
                .andExpect(jsonPath("$.data[0].filesIndex").doesNotExist())
                .andExpect(jsonPath("$.data[0].hasManifest").doesNotExist())
                .andExpect(jsonPath("$.data[0].hasChecksums").doesNotExist())
                .andExpect(jsonPath("$.data[0].verifyMessage").value("备份集完整，可用于恢复"));
    }

    @Test
    void testGetBackupSets_ShouldRejectNonPositiveTargetId() throws Exception {
        mockMvc.perform(get("/restores/sets").param("targetId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("targetId 必须为正数"));
    }

    @Test
    void testRunRestore() throws Exception {
        when(backupService.runRestore(any())).thenReturn(RestoreJob.builder()
                .restoreNo("RS-001")
                .targetName("NAS")
                .backupSetName("BK-001")
                .status(RestoreJob.STATUS_SUCCESS)
                .restoreReport("{\"status\":\"SUCCESS\"}")
                .verifyStatus("READY")
                .rebuildIndexStatus("SUCCESS")
                .createdAt(java.time.LocalDateTime.now())
                .operatorId(99L)
                .updatedAt(java.time.LocalDateTime.now())
                .build());

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
                .andExpect(jsonPath("$.message").value("恢复任务已完成"))
                .andExpect(jsonPath("$.data.restoreNo").value("RS-001"))
                .andExpect(jsonPath("$.data.targetName").value("NAS"))
                .andExpect(jsonPath("$.data.backupSetName").value("BK-001"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.statusMessage").value("恢复任务已完成"))
                .andExpect(jsonPath("$.data.followUpRequired").value(false))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.operatorId").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.sourceType").doesNotExist())
                .andExpect(jsonPath("$.data.targetId").doesNotExist())
                .andExpect(jsonPath("$.data.restoreReport").doesNotExist())
                .andExpect(jsonPath("$.data.verifyStatus").doesNotExist())
                .andExpect(jsonPath("$.data.rebuildIndexStatus").doesNotExist())
                .andExpect(jsonPath("$.data.restoredDatabase").doesNotExist())
                .andExpect(jsonPath("$.data.restoredFiles").doesNotExist())
                .andExpect(jsonPath("$.data.restoredConfig").doesNotExist())
                .andExpect(jsonPath("$.data.startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.finishedAt").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.operatorName").doesNotExist());
    }

    @Test
    void testRunRestore_ShouldExposeFollowUpWarningWhenMaintenanceExitFails() throws Exception {
        when(backupService.runRestore(any())).thenReturn(RestoreJob.builder()
                .restoreNo("RS-002")
                .targetName("NAS")
                .backupSetName("BK-002")
                .status(RestoreJob.STATUS_SUCCESS)
                .restoreReport("""
                        {"status":"SUCCESS","steps":[{"step":"MAINTENANCE","status":"FAILED","message":"恢复已完成，但退出维护模式失败，请手动处理"}]}
                        """)
                .verifyStatus("READY")
                .rebuildIndexStatus("SKIPPED")
                .createdAt(java.time.LocalDateTime.now())
                .build());

        mockMvc.perform(post("/restores/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetId", 1,
                                "backupSetName", "BK-002",
                                "restoreDatabase", false,
                                "restoreFiles", false,
                                "restoreConfig", true,
                                "rebuildIndex", false,
                                "exitMaintenanceAfterSuccess", true,
                                "confirmationText", "RESTORE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("恢复任务已完成，但退出维护模式失败，请手动处理"))
                .andExpect(jsonPath("$.data.restoreNo").value("RS-002"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.statusMessage").value("恢复任务已完成，但退出维护模式失败，请手动处理"))
                .andExpect(jsonPath("$.data.followUpRequired").value(true));
    }

    @Test
    void testRunRestore_ShouldRejectNonPositiveTargetId() throws Exception {
        mockMvc.perform(post("/restores/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetId", 0,
                                "backupSetName", "BK-001",
                                "restoreDatabase", true,
                                "restoreFiles", true,
                                "restoreConfig", true,
                                "rebuildIndex", true,
                                "exitMaintenanceAfterSuccess", true,
                                "confirmationText", "RESTORE"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("targetId 必须为正数"));
    }

    @Test
    void testRunRestore_ShouldRejectOverlongConfirmationText() throws Exception {
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
                                "confirmationText", "R".repeat(33)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("confirmationText 长度不能超过32"));
    }

    @Test
    void testGetRestoreJobs() throws Exception {
        when(backupService.getRestoreJobs(eq(1), eq(20))).thenReturn(PageResult.of(1, 20, 1, List.of(
                RestoreJob.builder()
                        .id(1L)
                        .restoreNo("RS-001")
                        .status(RestoreJob.STATUS_FAILED)
                        .restoredFiles(true)
                        .restoreReport("{\"status\":\"FAILED\"}")
                        .errorMessage("psql failed: /tmp/restore-output.log")
                        .operatorId(88L)
                        .updatedAt(java.time.LocalDateTime.now())
                        .rebuildIndexStatus("SKIPPED")
                        .build()
        )));

        mockMvc.perform(get("/restores/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].restoreNo").value("RS-001"))
                .andExpect(jsonPath("$.data.records[0].statusMessage").value("恢复执行失败，已完成：电子文件恢复"))
                .andExpect(jsonPath("$.data.records[0].followUpRequired").value(false))
                .andExpect(jsonPath("$.data.records[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].restoreReport").value("{\"status\":\"FAILED\"}"))
                .andExpect(jsonPath("$.data.records[0].errorMessage").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].operatorId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].sourceType").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].targetId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].restoredDatabase").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].restoredFiles").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].restoredConfig").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].finishedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].operatorName").doesNotExist());
    }

    @Test
    void testGetRestoreJobs_ShouldDescribeSkippedIndexOnSuccess() throws Exception {
        when(backupService.getRestoreJobs(eq(1), eq(20))).thenReturn(PageResult.of(1, 20, 1, List.of(
                RestoreJob.builder()
                        .restoreNo("RS-002")
                        .status(RestoreJob.STATUS_SUCCESS)
                        .rebuildIndexStatus("SKIPPED")
                        .restoreReport("{\"status\":\"SUCCESS\"}")
                        .build()
        )));

        mockMvc.perform(get("/restores/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].restoreNo").value("RS-002"))
                .andExpect(jsonPath("$.data.records[0].statusMessage").value("恢复任务已完成，未执行索引重建"))
                .andExpect(jsonPath("$.data.records[0].followUpRequired").value(false));
    }
}
