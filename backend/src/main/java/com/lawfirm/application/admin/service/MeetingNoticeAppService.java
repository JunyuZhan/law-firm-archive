package com.lawfirm.application.admin.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.domain.admin.repository.MeetingBookingRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会议通知服务（M8-022）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingNoticeAppService {

    private final MeetingBookingRepository bookingRepository;
    private final MeetingBookingMapper bookingMapper;

    /**
     * 发送会议通知
     * 问题414修复：添加权限验证
     */
    @Transactional
    public void sendMeetingNotice(Long bookingId) {
        MeetingBooking booking = bookingRepository.getByIdOrThrow(bookingId, "会议预约不存在");
        Long currentUserId = SecurityUtils.getUserId();
        
        if (booking.getReminderSent() != null && booking.getReminderSent()) {
            log.warn("会议通知已发送: bookingNo={}", booking.getBookingNo());
            return;
        }

        // 问题414修复：验证权限：组织者或管理员
        if (!booking.getOrganizerId().equals(currentUserId)) {
            if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("MEETING_MANAGER")) {
                throw new BusinessException("权限不足：只有组织者或管理员才能发送通知");
            }
        }

        // 标记为已发送（实际发送通知的逻辑可以集成消息推送服务）
        booking.setReminderSent(true);
        booking.setUpdatedBy(currentUserId);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.updateById(booking);

        log.info("会议通知已发送: bookingNo={}, title={}, sentBy={}",
                booking.getBookingNo(), booking.getTitle(), currentUserId);
    }

    /**
     * 批量发送即将开始的会议通知
     * 问题408修复：使用批量更新替代循环更新
     */
    @Transactional
    public int sendUpcomingMeetingNotices(int minutesBefore) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusMinutes(minutesBefore);
        Long userId = SecurityUtils.getUserId();
        
        // 查询即将开始的会议（在指定分钟数内开始，且未发送通知）
        List<MeetingBooking> upcomingBookings = bookingMapper.selectUpcomingMeetings(now, targetTime);
        
        if (upcomingBookings.isEmpty()) {
            return 0;
        }

        // 问题408修复：收集需要更新的预约
        List<MeetingBooking> toUpdate = new ArrayList<>();
        for (MeetingBooking booking : upcomingBookings) {
            if (booking.getReminderSent() == null || !booking.getReminderSent()) {
                booking.setReminderSent(true);
                booking.setUpdatedBy(userId);
                booking.setUpdatedAt(now);
                toUpdate.add(booking);
            }
        }

        // 批量更新
        if (!toUpdate.isEmpty()) {
            bookingRepository.updateBatchById(toUpdate);
            toUpdate.forEach(b -> log.info("发送会议通知: bookingNo={}, title={}", b.getBookingNo(), b.getTitle()));
        }
        
        return toUpdate.size();
    }
}


