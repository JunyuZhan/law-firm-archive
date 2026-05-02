package com.archivesystem.dto.backup;

import com.archivesystem.entity.BackupJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupJobResponseTest {

    @Test
    void shouldExposeFollowUpWarningWhenDatabaseBackupUnavailable() {
        BackupJob job = BackupJob.builder()
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .errorMessage("未识别 PostgreSQL 连接信息，已生成占位数据库文件")
                .build();

        BackupJobResponse response = BackupJobResponse.from(job);

        assertEquals("备份任务已完成，但数据库备份不可用，请检查系统环境", response.getStatusMessage());
        assertEquals(true, response.isFollowUpRequired());
    }

    @Test
    void shouldNotRequireFollowUpForNormalSuccess() {
        BackupJob job = BackupJob.builder()
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .build();

        BackupJobResponse response = BackupJobResponse.from(job);

        assertEquals("备份任务已完成，已备份2个文件记录", response.getStatusMessage());
        assertEquals(false, response.isFollowUpRequired());
    }

    @Test
    void shouldNotRequireFollowUpForUnrelatedSuccessNote() {
        BackupJob job = BackupJob.builder()
                .status(BackupJob.STATUS_SUCCESS)
                .fileCount(2L)
                .errorMessage("历史备份清理失败，但本次备份已完成")
                .build();

        BackupJobResponse response = BackupJobResponse.from(job);

        assertEquals("备份任务已完成，已备份2个文件记录", response.getStatusMessage());
        assertEquals(false, response.isFollowUpRequired());
    }
}
