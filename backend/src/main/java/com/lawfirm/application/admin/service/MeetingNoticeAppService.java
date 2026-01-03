package com.lawfirm.application.admin.service;

import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.domain.admin.repository.MeetingBookingRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     */
    @Transactional
    public void sendMeetingNotice(Long bookingId) {
        MeetingBooking booking = bookingRepository.getByIdOrThrow(bookingId, "会议预约不存在");
        
        if (booking.getReminderSent() != null && booking.getReminderSent()) {
            log.warn("会议通知已发送: bookingNo={}", booking.getBookingNo());
            return;
        }

        // 标记为已发送（实际发送通知的逻辑可以集成消息推送服务）
        booking.setReminderSent(true);
        booking.setUpdatedBy(SecurityUtils.getUserId());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.updateById(booking);

        log.info("会议通知已发送: bookingNo={}, title={}", booking.getBookingNo(), booking.getTitle());
    }

    /**
     * 批量发送即将开始的会议通知
     */
    @Transactional
    public int sendUpcomingMeetingNotices(int minutesBefore) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusMinutes(minutesBefore);
        
        // 查询即将开始的会议（在指定分钟数内开始，且未发送通知）
        List<MeetingBooking> upcomingBookings = bookingMapper.selectUpcomingMeetings(now, targetTime);
        
        int sentCount = 0;
        for (MeetingBooking booking : upcomingBookings) {
            if (booking.getReminderSent() == null || !booking.getReminderSent()) {
                booking.setReminderSent(true);
                booking.setUpdatedBy(SecurityUtils.getUserId());
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.updateById(booking);
                sentCount++;
                log.info("发送会议通知: bookingNo={}, title={}", booking.getBookingNo(), booking.getTitle());
            }
        }
        
        return sentCount;
    }
}

