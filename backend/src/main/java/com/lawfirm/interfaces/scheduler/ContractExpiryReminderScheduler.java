package com.lawfirm.interfaces.scheduler;

import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 合同到期提醒定时调度器
 * 问题255实现：合同到期提醒功能
 * 
 * 功能：
 * 1. 每天上午9点检查即将到期的合同（30天内），发送提醒通知给承办律师
 * 2. 每天上午10点检查已逾期的合同，发送逾期警告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractExpiryReminderScheduler {

    private final ContractRepository contractRepository;
    private final ContractParticipantRepository participantRepository;
    private final NotificationAppService notificationAppService;

    // 提前提醒天数
    private static final int REMINDER_DAYS_30 = 30;
    private static final int REMINDER_DAYS_7 = 7;
    private static final int REMINDER_DAYS_3 = 3;

    /**
     * 即将到期合同提醒
     * 每天上午9点执行，提醒30天内到期的合同
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendUpcomingContractReminders() {
        log.info("开始执行合同到期提醒定时任务");
        
        LocalDate today = LocalDate.now();
        int sentCount = 0;
        
        // 1. 30天内到期的合同（首次提醒）
        List<Contract> contracts30Days = contractRepository.findExpiringContracts(today, today.plusDays(REMINDER_DAYS_30));
        for (Contract contract : contracts30Days) {
            long daysRemaining = ChronoUnit.DAYS.between(today, contract.getExpiryDate());
            
            // 根据剩余天数决定是否发送提醒（30天、7天、3天各发送一次）
            if (daysRemaining == REMINDER_DAYS_30 || 
                daysRemaining == REMINDER_DAYS_7 || 
                daysRemaining == REMINDER_DAYS_3 ||
                daysRemaining == 1) {
                
                sentCount += sendContractReminder(contract, daysRemaining, false);
            }
        }
        
        log.info("合同到期提醒定时任务执行完成，共发送{}条提醒", sentCount);
    }

    /**
     * 逾期合同警告
     * 每天上午10点执行，提醒已逾期的合同
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional
    public void sendExpiredContractWarnings() {
        log.info("开始执行合同逾期警告定时任务");
        
        LocalDate today = LocalDate.now();
        List<Contract> expiredContracts = contractRepository.findExpiredContracts(today);
        int sentCount = 0;
        
        for (Contract contract : expiredContracts) {
            long daysOverdue = ChronoUnit.DAYS.between(contract.getExpiryDate(), today);
            
            // 逾期后每7天发送一次警告
            if (daysOverdue == 1 || daysOverdue % 7 == 0) {
                sentCount += sendContractReminder(contract, daysOverdue, true);
            }
        }
        
        log.info("合同逾期警告定时任务执行完成，共发送{}条警告", sentCount);
    }

    /**
     * 发送合同提醒/警告
     * @param contract 合同
     * @param days 剩余天数或逾期天数
     * @param isOverdue 是否逾期
     * @return 发送成功的通知数量
     */
    private int sendContractReminder(Contract contract, long days, boolean isOverdue) {
        int sentCount = 0;
        
        try {
            // 获取承办律师
            ContractParticipant lead = participantRepository.findLeadByContractId(contract.getId());
            if (lead == null || lead.getUserId() == null) {
                log.warn("合同[{}]没有承办律师，跳过提醒", contract.getContractNo());
                return 0;
            }
            
            String title;
            String message;
            
            if (isOverdue) {
                title = "合同逾期警告";
                message = String.format("【逾期警告】合同【%s】（编号：%s）已逾期%d天（到期日期：%s），请尽快处理续签或归档！",
                        contract.getName(), contract.getContractNo(), days, contract.getExpiryDate());
            } else {
                String urgency = days <= 3 ? "【紧急】" : (days <= 7 ? "【重要】" : "");
                title = "合同到期提醒";
                message = String.format("%s合同【%s】（编号：%s）将于%d天后到期（%s），请及时处理续签事宜！",
                        urgency, contract.getName(), contract.getContractNo(), days, contract.getExpiryDate());
            }
            
            notificationAppService.sendSystemNotification(
                    lead.getUserId(),
                    title,
                    message,
                    "CONTRACT",
                    contract.getId()
            );
            
            sentCount++;
            log.debug("发送合同{}提醒: contractId={}, userId={}, days={}", 
                    isOverdue ? "逾期" : "到期", contract.getId(), lead.getUserId(), days);
                    
        } catch (Exception e) {
            log.error("发送合同提醒失败: contractId={}, contractNo={}", 
                    contract.getId(), contract.getContractNo(), e);
        }
        
        return sentCount;
    }
}

