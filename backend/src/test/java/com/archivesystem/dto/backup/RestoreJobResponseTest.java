package com.archivesystem.dto.backup;

import com.archivesystem.entity.RestoreJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestoreJobResponseTest {

    @Test
    void shouldExposeMaintenanceFollowUpWarningOnlyWhenMaintenanceStepFailed() {
        RestoreJob job = RestoreJob.builder()
                .status(RestoreJob.STATUS_SUCCESS)
                .rebuildIndexStatus("SUCCESS")
                .restoreReport("""
                        {
                          "status": "SUCCESS",
                          "steps": [
                            {"step": "MAINTENANCE", "status": "SUCCESS"},
                            {"step": "FILES", "status": "FAILED"}
                          ]
                        }
                        """)
                .build();

        RestoreJobResponse response = RestoreJobResponse.from(job);

        assertEquals("恢复任务已完成", response.getStatusMessage());
        assertEquals(false, response.isFollowUpRequired());
    }

    @Test
    void shouldExposeMaintenanceFollowUpWarningWhenMaintenanceStepFailed() {
        RestoreJob job = RestoreJob.builder()
                .status(RestoreJob.STATUS_SUCCESS)
                .rebuildIndexStatus("SUCCESS")
                .restoreReport("""
                        {
                          "status": "SUCCESS",
                          "steps": [
                            {"step": "MAINTENANCE", "status": "FAILED"}
                          ]
                        }
                        """)
                .build();

        RestoreJobResponse response = RestoreJobResponse.from(job);

        assertEquals("恢复任务已完成，但退出维护模式失败，请手动处理", response.getStatusMessage());
        assertEquals(true, response.isFollowUpRequired());
    }
}
