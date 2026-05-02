package com.archivesystem.schedule;

import com.archivesystem.entity.BackupTarget;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.service.BackupService;
import com.archivesystem.service.ConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份中心定时任务.
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduledTasks {

    private static final String DEFAULT_CRON = "0 0 2 * * ?";

    private final BackupTargetMapper backupTargetMapper;
    private final BackupService backupService;
    private final ConfigService configService;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelay = 30_000L, initialDelay = 90_000L)
    public void triggerScheduledBackups() {
        if (!configService.getBooleanValue("system.backup.enabled", true)) {
            return;
        }

        String cron = configService.getValue("system.backup.cron", DEFAULT_CRON);
        if (!StringUtils.hasText(cron)) {
            cron = DEFAULT_CRON;
        }
        if (!isDue(cron)) {
            return;
        }

        List<BackupTarget> targets = backupTargetMapper.selectList(new LambdaQueryWrapper<BackupTarget>()
                .eq(BackupTarget::getEnabled, true));
        if (targets.isEmpty()) {
            return;
        }

        for (BackupTarget target : targets) {
            try {
                backupService.runScheduledBackup(target.getId());
            } catch (Exception e) {
                log.error("定时备份执行失败: targetId={}", target.getId(), e);
            }
        }
    }

    private boolean isDue(String cron) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slot = now.withSecond(0).withNano(0);
        CronExpression expression;
        try {
            expression = CronExpression.parse(cron);
        } catch (Exception e) {
            log.error("备份定时表达式无效: cron={}", cron, e);
            return false;
        }
        LocalDateTime expected = expression.next(slot.minusMinutes(1));
        if (expected == null || expected.isBefore(slot) || !expected.isBefore(slot.plusMinutes(1))) {
            return false;
        }

        String key = "backup:schedule:" + slot;
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(2));
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.error("获取备份调度锁失败: key={}", key, e);
            return false;
        }
    }
}
