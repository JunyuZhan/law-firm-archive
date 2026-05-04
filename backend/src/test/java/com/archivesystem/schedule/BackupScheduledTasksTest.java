package com.archivesystem.schedule;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.archivesystem.entity.BackupTarget;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.service.BackupService;
import com.archivesystem.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class BackupScheduledTasksTest {

    private static final Logger SCHEDULED_TASKS_LOGGER = (Logger) LoggerFactory.getLogger(BackupScheduledTasks.class);

    @Mock
    private BackupTargetMapper backupTargetMapper;

    @Mock
    private BackupService backupService;

    @Mock
    private ConfigService configService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BackupScheduledTasks backupScheduledTasks;

    @Test
    void testTriggerScheduledBackups() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        String cron = String.format("0 %d %d * * *", now.getMinute(), now.getHour());
        when(configService.getBooleanValue("system.backup.enabled", true)).thenReturn(true);
        when(configService.getValue("system.backup.cron", "0 0 2 * * ?")).thenReturn(cron);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), any())).thenReturn(true);
        when(backupTargetMapper.selectList(any())).thenReturn(List.of(
                BackupTarget.builder().id(1L).enabled(true).targetType(BackupTarget.TYPE_LOCAL).build()
        ));

        backupScheduledTasks.triggerScheduledBackups();

        verify(backupService).runScheduledBackup(1L);
    }

    @Test
    void testTriggerScheduledBackups_ShouldIncludeSmbTargets() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        String cron = String.format("0 %d %d * * *", now.getMinute(), now.getHour());
        when(configService.getBooleanValue("system.backup.enabled", true)).thenReturn(true);
        when(configService.getValue("system.backup.cron", "0 0 2 * * ?")).thenReturn(cron);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), any())).thenReturn(true);
        when(backupTargetMapper.selectList(any())).thenReturn(List.of(
                BackupTarget.builder().id(2L).enabled(true).targetType(BackupTarget.TYPE_SMB).build()
        ));

        backupScheduledTasks.triggerScheduledBackups();

        verify(backupService).runScheduledBackup(2L);
    }

    @Test
    void testTriggerScheduledBackupsSkippedWhenDisabled() {
        when(configService.getBooleanValue("system.backup.enabled", true)).thenReturn(false);

        backupScheduledTasks.triggerScheduledBackups();

        verify(backupTargetMapper, never()).selectList(any());
        verify(backupService, never()).runScheduledBackup(any());
    }

    @Test
    void testTriggerScheduledBackupsSkippedWhenCronInvalid() {
        when(configService.getBooleanValue("system.backup.enabled", true)).thenReturn(true);
        when(configService.getValue("system.backup.cron", "0 0 2 * * ?")).thenReturn("invalid-cron");

        withLoggerLevel(Level.OFF, backupScheduledTasks::triggerScheduledBackups);

        verify(backupTargetMapper, never()).selectList(any());
        verify(backupService, never()).runScheduledBackup(any());
    }

    @Test
    void testTriggerScheduledBackupsSkippedWhenRedisLockFails() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        String cron = String.format("0 %d %d * * *", now.getMinute(), now.getHour());
        when(configService.getBooleanValue("system.backup.enabled", true)).thenReturn(true);
        when(configService.getValue("system.backup.cron", "0 0 2 * * ?")).thenReturn(cron);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), any())).thenThrow(new RuntimeException("redis down"));

        withLoggerLevel(Level.OFF, backupScheduledTasks::triggerScheduledBackups);

        verify(backupTargetMapper, never()).selectList(any());
        verify(backupService, never()).runScheduledBackup(any());
    }

    private void withLoggerLevel(Level level, Runnable action) {
        Level originalLevel = SCHEDULED_TASKS_LOGGER.getLevel();
        SCHEDULED_TASKS_LOGGER.setLevel(level);
        try {
            action.run();
        } finally {
            SCHEDULED_TASKS_LOGGER.setLevel(originalLevel);
        }
    }
}
