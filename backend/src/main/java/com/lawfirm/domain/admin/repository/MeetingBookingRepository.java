package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import org.springframework.stereotype.Repository;

/** 会议预约仓储 */
@Repository
public class MeetingBookingRepository
    extends AbstractRepository<MeetingBookingMapper, MeetingBooking> {}
