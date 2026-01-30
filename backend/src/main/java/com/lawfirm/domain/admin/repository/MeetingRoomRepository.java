package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRoomMapper;
import org.springframework.stereotype.Repository;

/** 会议室仓储 */
@Repository
public class MeetingRoomRepository extends AbstractRepository<MeetingRoomMapper, MeetingRoom> {}
