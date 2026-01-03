package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.command.CreateMeetingRecordCommand;
import com.lawfirm.application.admin.dto.MeetingRecordDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingRecord;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import com.lawfirm.domain.admin.repository.MeetingRecordRepository;
import com.lawfirm.domain.admin.repository.MeetingRoomRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会议记录服务（M8-023）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRecordAppService {

    private final MeetingRecordRepository recordRepository;
    private final MeetingRecordMapper recordMapper;
    private final MeetingRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建会议记录
     */
    @Transactional
    public MeetingRecordDTO createRecord(CreateMeetingRecordCommand command) {
        Long userId = SecurityUtils.getUserId();

        // 验证会议室
        MeetingRoom room = roomRepository.getByIdOrThrow(command.getRoomId(), "会议室不存在");

        // 生成记录编号
        String recordNo = generateRecordNo();

        // 序列化参会人员
        String attendeesJson = null;
        if (command.getAttendeeIds() != null && !command.getAttendeeIds().isEmpty()) {
            try {
                attendeesJson = objectMapper.writeValueAsString(command.getAttendeeIds());
            } catch (JsonProcessingException e) {
                log.warn("序列化参会人员失败", e);
            }
        }

        MeetingRecord record = MeetingRecord.builder()
                .recordNo(recordNo)
                .bookingId(command.getBookingId())
                .roomId(command.getRoomId())
                .title(command.getTitle())
                .meetingDate(command.getMeetingDate())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .organizerId(userId)
                .attendees(attendeesJson)
                .content(command.getContent())
                .decisions(command.getDecisions())
                .actionItems(command.getActionItems())
                .attachmentUrl(command.getAttachmentUrl())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        recordRepository.save(record);
        log.info("会议记录创建成功: recordNo={}, title={}", recordNo, command.getTitle());
        return toDTO(record);
    }

    /**
     * 根据预约ID创建会议记录
     */
    @Transactional
    public MeetingRecordDTO createRecordFromBooking(Long bookingId, CreateMeetingRecordCommand command) {
        // 检查是否已有记录
        MeetingRecord existing = recordMapper.selectByBookingId(bookingId);
        if (existing != null) {
            throw new BusinessException("该预约已有会议记录");
        }

        command.setBookingId(bookingId);
        return createRecord(command);
    }

    /**
     * 查询会议记录
     */
    public MeetingRecordDTO getRecordById(Long id) {
        MeetingRecord record = recordRepository.getByIdOrThrow(id, "会议记录不存在");
        return toDTO(record);
    }

    /**
     * 查询会议室的会议记录
     */
    public List<MeetingRecordDTO> getRecordsByRoom(Long roomId) {
        List<MeetingRecord> records = recordMapper.selectByRoomId(roomId);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询指定日期范围的会议记录
     */
    public List<MeetingRecordDTO> getRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<MeetingRecord> records = recordMapper.selectByDateRange(startDate, endDate);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private String generateRecordNo() {
        String prefix = "MR" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    private MeetingRecordDTO toDTO(MeetingRecord record) {
        MeetingRecordDTO dto = new MeetingRecordDTO();
        dto.setId(record.getId());
        dto.setRecordNo(record.getRecordNo());
        dto.setBookingId(record.getBookingId());
        dto.setRoomId(record.getRoomId());
        dto.setTitle(record.getTitle());
        dto.setMeetingDate(record.getMeetingDate());
        dto.setStartTime(record.getStartTime());
        dto.setEndTime(record.getEndTime());
        dto.setOrganizerId(record.getOrganizerId());
        dto.setAttendees(record.getAttendees());
        dto.setContent(record.getContent());
        dto.setDecisions(record.getDecisions());
        dto.setActionItems(record.getActionItems());
        dto.setAttachmentUrl(record.getAttachmentUrl());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());

        // 查询会议室名称
        if (record.getRoomId() != null) {
            MeetingRoom room = roomRepository.findById(record.getRoomId());
            if (room != null) {
                dto.setRoomName(room.getName());
            }
        }

        // 查询组织者名称
        if (record.getOrganizerId() != null) {
            User organizer = userRepository.findById(record.getOrganizerId());
            if (organizer != null) {
                dto.setOrganizerName(organizer.getRealName());
            }
        }

        return dto;
    }
}

