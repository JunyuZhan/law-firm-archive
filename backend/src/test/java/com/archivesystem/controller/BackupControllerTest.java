package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.common.exception.GlobalExceptionHandler;
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
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor methodValidationPostProcessor = new MethodValidationPostProcessor();
        methodValidationPostProcessor.setValidator(validator);
        Object validatedController = methodValidationPostProcessor.postProcessAfterInitialization(
                backupController, "backupController");

        mockMvc = MockMvcBuilders.standaloneSetup(validatedController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler(null))
                .build();
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
        when(backupService.getTargets()).thenReturn(List.of(BackupTargetResponse.builder()
                .id(1L)
                .name("NAS")
                .targetType("SMB")
                .enabled(true)
                .displayAddress("NAS")
                .verifyStatus("SUCCESS")
                .verifyMessage("连接正常")
                .localPath("/private/backups")
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbSubPath("nightly")
                .remarks("nightly target")
                .build()));

        mockMvc.perform(get("/backups/targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("NAS"))
                .andExpect(jsonPath("$.data[0].targetType").value("SMB"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].displayAddress").value("NAS"))
                .andExpect(jsonPath("$.data[0].verifyStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].localPath").doesNotExist())
                .andExpect(jsonPath("$.data[0].smbHost").doesNotExist())
                .andExpect(jsonPath("$.data[0].smbShare").doesNotExist())
                .andExpect(jsonPath("$.data[0].smbUsername").doesNotExist())
                .andExpect(jsonPath("$.data[0].smbSubPath").doesNotExist())
                .andExpect(jsonPath("$.data[0].remarks").doesNotExist())
                .andExpect(jsonPath("$.data[0].verifyMessage").value("连接正常"))
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());
    }

    @Test
    void testGetTarget() throws Exception {
        when(backupService.getTarget(1L)).thenReturn(BackupTargetResponse.builder()
                .id(1L)
                .name("NAS")
                .displayAddress("NAS")
                .targetType("SMB")
                .verifyMessage("连接正常")
                .smbHost("192.168.50.5")
                .smbPort(1445)
                .smbShare("archive")
                .smbUsername("backup")
                .smbSubPath("nightly")
                .build());

        mockMvc.perform(get("/backups/targets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("NAS"))
                .andExpect(jsonPath("$.data.smbHost").value("192.168.50.5"))
                .andExpect(jsonPath("$.data.smbShare").value("archive"))
                .andExpect(jsonPath("$.data.smbPort").value(1445))
                .andExpect(jsonPath("$.data.hasSmbPassword").doesNotExist())
                .andExpect(jsonPath("$.data.lastVerifiedAt").doesNotExist())
                .andExpect(jsonPath("$.data.verifyMessage").value("连接正常"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void testGetTarget_ShouldRejectNonPositiveId() throws Exception {
        mockMvc.perform(get("/backups/targets/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("id 必须为正数"));
    }

    @Test
    void testCreateTarget() throws Exception {
        when(backupService.createTarget(any())).thenReturn(BackupTargetResponse.builder()
                .id(1L)
                .name("NAS")
                .targetType("LOCAL")
                .enabled(true)
                .displayAddress("/tmp/backups")
                .verifyStatus("PENDING")
                .localPath("/tmp/backups")
                .remarks("nightly target")
                .build());

        mockMvc.perform(post("/backups/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", "NAS");
                            put("targetType", "LOCAL");
                            put("enabled", true);
                            put("localPath", "/tmp/backups");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("NAS"))
                .andExpect(jsonPath("$.data.targetType").value("LOCAL"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.displayAddress").value("/tmp/backups"))
                .andExpect(jsonPath("$.data.verifyStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.localPath").doesNotExist())
                .andExpect(jsonPath("$.data.remarks").doesNotExist());
    }

    @Test
    void testUpdateTarget() throws Exception {
        when(backupService.updateTarget(eq(1L), any())).thenReturn(BackupTargetResponse.builder()
                .id(1L)
                .name("NAS")
                .targetType("SMB")
                .enabled(false)
                .displayAddress("\\\\192.168.50.5\\archive")
                .verifyStatus("SUCCESS")
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .remarks("nightly target")
                .build());

        mockMvc.perform(put("/backups/targets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", "NAS");
                            put("targetType", "SMB");
                            put("enabled", false);
                            put("smbHost", "192.168.50.5");
                            put("smbShare", "archive");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("NAS"))
                .andExpect(jsonPath("$.data.targetType").value("SMB"))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.displayAddress").value("\\\\192.168.50.5\\archive"))
                .andExpect(jsonPath("$.data.verifyStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.smbHost").doesNotExist())
                .andExpect(jsonPath("$.data.smbShare").doesNotExist())
                .andExpect(jsonPath("$.data.remarks").doesNotExist());
    }

    @Test
    void testCreateTarget_ShouldRejectInvalidTargetType() throws Exception {
        mockMvc.perform(post("/backups/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", "NAS");
                            put("targetType", "FTP");
                            put("enabled", true);
                            put("localPath", "/tmp/backups");
                        }})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("targetType 仅支持 LOCAL 或 SMB"));
    }

    @Test
    void testCreateTarget_ShouldRejectInvalidSmbPort() throws Exception {
        mockMvc.perform(post("/backups/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", "NAS");
                            put("targetType", "SMB");
                            put("enabled", true);
                            put("smbHost", "192.168.50.5");
                            put("smbShare", "archive");
                            put("smbPort", 70000);
                        }})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("SMB 端口最大为65535"));
    }

    @Test
    void testVerifyTarget() throws Exception {
        when(backupService.verifyTarget(1L)).thenReturn(BackupTargetResponse.builder()
                .id(1L)
                .name("NAS")
                .targetType("SMB")
                .enabled(true)
                .displayAddress("NAS")
                .verifyStatus("SUCCESS")
                .verifyMessage("连接正常")
                .localPath("/private/backups")
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .remarks("nightly target")
                .build());

        mockMvc.perform(post("/backups/targets/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("NAS"))
                .andExpect(jsonPath("$.data.targetType").value("SMB"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.displayAddress").value("NAS"))
                .andExpect(jsonPath("$.data.verifyStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.localPath").doesNotExist())
                .andExpect(jsonPath("$.data.smbHost").doesNotExist())
                .andExpect(jsonPath("$.data.smbShare").doesNotExist())
                .andExpect(jsonPath("$.data.remarks").doesNotExist())
                .andExpect(jsonPath("$.data.verifyMessage").value("连接正常"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist());
    }

    @Test
    void testGetBackupSets() throws Exception {
        when(backupService.getBackupSets(eq(1L))).thenReturn(List.of(com.archivesystem.dto.backup.BackupSetResponse.builder()
                .backupSetName("BK-20260330150000-aaaa1111")
                .backupSetPath("/private/backups/BK-20260330150000-aaaa1111")
                .displayPath("/private/backups/BK-20260330150000-aaaa1111")
                .databaseRestorable(true)
                .filesRestorable(false)
                .configRestorable(true)
                .verifyStatus("READY")
                .verifyMessage("备份集完整，可用于恢复")
                .build()));

        mockMvc.perform(get("/backups/sets").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].backupSetName").value("BK-20260330150000-aaaa1111"))
                .andExpect(jsonPath("$.data[0].displayPath").value("/private/backups/BK-20260330150000-aaaa1111"))
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
        mockMvc.perform(get("/backups/sets").param("targetId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("targetId 必须为正数"));
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
        BackupJob failedJob = BackupJob.builder()
                .id(1L)
                .backupNo("BK-001")
                .status(BackupJob.STATUS_FAILED)
                .backupSetPath("/private/backups/BK-001")
                .errorMessage("disk full")
                .operatorId(99L)
                .operatorName("admin")
                .build();
        BackupJob successJob = BackupJob.builder()
                .backupNo("BK-002")
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .build();
        when(backupService.getBackupJobs(eq(1), eq(20))).thenReturn(PageResult.of(1, 20, 2, List.of(failedJob, successJob)));

        mockMvc.perform(get("/backups/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].backupNo").value("BK-001"))
                .andExpect(jsonPath("$.data.records[0].statusMessage").value("备份执行失败，请联系系统管理员查看系统日志"))
                .andExpect(jsonPath("$.data.records[0].followUpRequired").value(false))
                .andExpect(jsonPath("$.data.records[1].backupNo").value("BK-002"))
                .andExpect(jsonPath("$.data.records[1].statusMessage").value("备份任务已完成，已备份2个文件记录"))
                .andExpect(jsonPath("$.data.records[1].followUpRequired").value(false))
                .andExpect(jsonPath("$.data.records[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].backupSetPath").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].errorMessage").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].operatorId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].operatorName").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].targetId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].triggerType").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].backupScope").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].fileCount").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].totalBytes").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].finishedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].updatedAt").doesNotExist());
    }

    @Test
    void testRunBackup() throws Exception {
        when(backupService.runManualBackup(1L)).thenReturn(BackupJob.builder()
                .id(1L)
                .backupNo("BK-001")
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .operatorId(99L)
                .operatorName("admin")
                .build());

        mockMvc.perform(post("/backups/run").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("备份任务已完成，已备份2个文件记录"))
                .andExpect(jsonPath("$.data.backupNo").value("BK-001"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.statusMessage").value("备份任务已完成，已备份2个文件记录"))
                .andExpect(jsonPath("$.data.followUpRequired").value(false))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.operatorId").doesNotExist())
                .andExpect(jsonPath("$.data.operatorName").doesNotExist())
                .andExpect(jsonPath("$.data.targetId").doesNotExist())
                .andExpect(jsonPath("$.data.triggerType").doesNotExist())
                .andExpect(jsonPath("$.data.backupScope").doesNotExist())
                .andExpect(jsonPath("$.data.fileCount").doesNotExist())
                .andExpect(jsonPath("$.data.totalBytes").doesNotExist())
                .andExpect(jsonPath("$.data.startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.finishedAt").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void testRunBackup_ShouldRejectNonPositiveTargetId() throws Exception {
        mockMvc.perform(post("/backups/run").param("targetId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("targetId 必须为正数"));
    }

    @Test
    void testRunBackup_ShouldExposeFollowUpWarningWhenDatabaseBackupUnavailable() throws Exception {
        when(backupService.runManualBackup(1L)).thenReturn(BackupJob.builder()
                .backupNo("BK-003")
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .errorMessage("未识别 PostgreSQL 连接信息，已生成占位数据库文件")
                .build());

        mockMvc.perform(post("/backups/run").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("备份任务已完成，但数据库备份不可用，请检查系统环境"))
                .andExpect(jsonPath("$.data.backupNo").value("BK-003"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.statusMessage").value("备份任务已完成，但数据库备份不可用，请检查系统环境"))
                .andExpect(jsonPath("$.data.followUpRequired").value(true));
    }

    @Test
    void testRunBackup_ShouldNotTreatUnrelatedSuccessNoteAsFollowUpWarning() throws Exception {
        when(backupService.runManualBackup(1L)).thenReturn(BackupJob.builder()
                .backupNo("BK-004")
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .errorMessage("历史备份清理失败，但本次备份已完成")
                .build());

        mockMvc.perform(post("/backups/run").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("备份任务已完成，已备份2个文件记录"))
                .andExpect(jsonPath("$.data.backupNo").value("BK-004"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.statusMessage").value("备份任务已完成，已备份2个文件记录"))
                .andExpect(jsonPath("$.data.followUpRequired").value(false));
    }
}
