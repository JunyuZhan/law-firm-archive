package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.BookMeetingCommand;
import com.lawfirm.application.admin.command.CreateMeetingRoomCommand;
import com.lawfirm.application.admin.dto.MeetingBookingDTO;
import com.lawfirm.application.admin.dto.MeetingBookingQueryDTO;
import com.lawfirm.application.admin.dto.MeetingRoomDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import com.lawfirm.domain.admin.repository.MeetingBookingRepository;
import com.lawfirm.domain.admin.repository.MeetingRoomRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRoomMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会议室应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRoomAppService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingRoomMapper meetingRoomMapper;
    private final MeetingBookingRepository meetingBookingRepository;
    private final MeetingBookingMapper meetingBookingMapper;
    private final ObjectMapper objectMapper;

    // ==================== 会议室管理 ====================

    /**
     * 获取所有会议室
     */
    public List<MeetingRoomDTO> listRooms() {
        return meetingRoomMapper.selectEnabledRooms().stream()
                .map(this::toRoomDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取可用会议室
     */
    public List<MeetingRoomDTO> listAvailableRooms() {
        return meetingRoomMapper.selectAvailableRooms().stream()
                .map(this::toRoomDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建会议室
     */
    @Transactional
    public MeetingRoomDTO createRoom(CreateMeetingRoomCommand command) {
        // 检查编码唯一性
        if (meetingRoomMapper.selectByCode(command.getCode()) != null) {
            throw new BusinessException("会议室编码已存在");
        }

        MeetingRoom room = MeetingRoom.builder()
                .name(command.getName())
                .code(command.getCode())
                .location(command.getLocation())
                .capacity(command.getCapacity())
                .equipment(command.getEquipment())
                .description(command.getDescription())
                .status(MeetingRoom.STATUS_AVAILABLE)
                .enabled(true)
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .build();

        meetingRoomRepository.save(room);
        log.info("会议室创建成功: {} ({})", room.getName(), room.getCode());
        return toRoomDTO(room);
    }

    /**
     * 更新会议室
     */
    @Transactional
    public MeetingRoomDTO updateRoom(Long id, CreateMeetingRoomCommand command) {
        MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");

        // 检查编码唯一性
        MeetingRoom existing = meetingRoomMapper.selectByCode(command.getCode());
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException("会议室编码已存在");
        }

        if (StringUtils.hasText(command.getName())) {
            room.setName(command.getName());
        }
        if (StringUtils.hasText(command.getCode())) {
            room.setCode(command.getCode());
        }
        if (command.getLocation() != null) {
            room.setLocation(command.getLocation());
        }
        if (command.getCapacity() != null) {
            room.setCapacity(command.getCapacity());
        }
        if (command.getEquipment() != null) {
            room.setEquipment(command.getEquipment());
        }
        if (command.getDescription() != null) {
            room.setDescription(command.getDescription());
        }
        if (command.getSortOrder() != null) {
            room.setSortOrder(command.getSortOrder());
        }

        meetingRoomRepository.updateById(room);
        log.info("会议室更新成功: {}", room.getName());
        return toRoomDTO(room);
    }

    /**
     * 删除会议室
     */
    @Transactional
    public void deleteRoom(Long id) {
        MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");

        // 检查是否有未完成的预约
        int count = meetingBookingMapper.countConflicting(id, LocalDateTime.now(), LocalDateTime.now().plusYears(1), null);
        if (count > 0) {
            throw new BusinessException("该会议室有未完成的预约，无法删除");
        }

        meetingRoomMapper.deleteById(id);
        log.info("会议室删除成功: {}", room.getName());
    }

    /**
     * 更新会议室状态
     */
    @Transactional
    public void updateRoomStatus(Long id, String status) {
        MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");
        room.setStatus(status);
        meetingRoomRepository.updateById(room);
        log.info("会议室状态更新: {} -> {}", room.getName(), status);
    }

    // ==================== 会议预约 ====================

    /**
     * 分页查询会议预约
     */
    public PageResult<MeetingBookingDTO> listBookings(MeetingBookingQueryDTO query) {
        IPage<MeetingBooking> page = meetingBookingMapper.selectBookingPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getRoomId(),
                query.getOrganizerId(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime()
        );

        List<MeetingBookingDTO> records = page.getRecords().stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 预约会议室
     */
    @Transactional
    public MeetingBookingDTO bookMeeting(BookMeetingCommand command) {
        Long userId = SecurityUtils.getUserId();

        // 验证会议室
        MeetingRoom room = meetingRoomRepository.getByIdOrThrow(command.getRoomId(), "会议室不存在");
        if (!room.getEnabled()) {
            throw new BusinessException("该会议室已禁用");
        }
        if (MeetingRoom.STATUS_MAINTENANCE.equals(room.getStatus())) {
            throw new BusinessException("该会议室正在维护中");
        }

        // 验证时间
        if (command.getStartTime().isAfter(command.getEndTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if (command.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("开始时间不能早于当前时间");
        }

        // 检查时间冲突
        int conflicting = meetingBookingMapper.countConflicting(
                command.getRoomId(),
                command.getStartTime(),
                command.getEndTime(),
                null
        );
        if (conflicting > 0) {
            throw new BusinessException("该时间段会议室已被预约");
        }

        // 生成预约编号
        String bookingNo = generateBookingNo();

        // 序列化参会人员
        String attendeesJson = null;
        if (command.getAttendeeIds() != null && !command.getAttendeeIds().isEmpty()) {
            try {
                attendeesJson = objectMapper.writeValueAsString(command.getAttendeeIds());
            } catch (JsonProcessingException e) {
                log.warn("序列化参会人员失败", e);
            }
        }

        // 创建预约
        MeetingBooking booking = MeetingBooking.builder()
                .bookingNo(bookingNo)
                .roomId(command.getRoomId())
                .title(command.getTitle())
                .organizerId(userId)
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .attendees(attendeesJson)
                .description(command.getDescription())
                .status(MeetingBooking.STATUS_BOOKED)
                .reminderSent(false)
                .build();

        meetingBookingRepository.save(booking);
        log.info("会议预约成功: {} - {}", bookingNo, room.getName());
        return toBookingDTO(booking);
    }

    /**
     * 取消会议预约
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Long userId = SecurityUtils.getUserId();

        MeetingBooking booking = meetingBookingRepository.getByIdOrThrow(bookingId, "预约不存在");

        if (!booking.getOrganizerId().equals(userId)) {
            throw new BusinessException("只能取消自己的预约");
        }
        if (!MeetingBooking.STATUS_BOOKED.equals(booking.getStatus())) {
            throw new BusinessException("只能取消已预约状态的会议");
        }

        booking.setStatus(MeetingBooking.STATUS_CANCELLED);
        meetingBookingRepository.updateById(booking);
        log.info("会议预约已取消: {}", booking.getBookingNo());
    }

    /**
     * 获取会议室某日预约情况
     */
    public List<MeetingBookingDTO> getRoomDayBookings(Long roomId, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        return meetingBookingMapper.selectByRoomAndTimeRange(roomId, startTime, endTime).stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我的会议预约
     */
    public List<MeetingBookingDTO> getMyBookings() {
        Long userId = SecurityUtils.getUserId();
        return meetingBookingMapper.selectByOrganizer(userId).stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取会议室日程视图（M8-024）
     */
    public List<MeetingBookingDTO> getRoomSchedule(Long roomId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        return meetingBookingMapper.selectByRoomAndTimeRange(roomId, startTime, endTime).stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有会议室日程视图（M8-024）
     */
    public Map<Long, List<MeetingBookingDTO>> getAllRoomsSchedule(LocalDate startDate, LocalDate endDate) {
        List<MeetingRoom> rooms = meetingRoomMapper.selectEnabledRooms();
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        
        return rooms.stream().collect(Collectors.toMap(
                MeetingRoom::getId,
                room -> meetingBookingMapper.selectByRoomAndTimeRange(room.getId(), startTime, endTime).stream()
                        .map(this::toBookingDTO)
                        .collect(Collectors.toList())
        ));
    }

    /**
     * 生成预约编号
     */
    private String generateBookingNo() {
        String prefix = "MT" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取会议室状态名称
     */
    private String getRoomStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case MeetingRoom.STATUS_AVAILABLE -> "可用";
            case MeetingRoom.STATUS_OCCUPIED -> "占用中";
            case MeetingRoom.STATUS_MAINTENANCE -> "维护中";
            default -> status;
        };
    }

    /**
     * 获取预约状态名称
     */
    private String getBookingStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case MeetingBooking.STATUS_BOOKED -> "已预约";
            case MeetingBooking.STATUS_IN_PROGRESS -> "进行中";
            case MeetingBooking.STATUS_COMPLETED -> "已完成";
            case MeetingBooking.STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }

    private MeetingRoomDTO toRoomDTO(MeetingRoom room) {
        MeetingRoomDTO dto = new MeetingRoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setCode(room.getCode());
        dto.setLocation(room.getLocation());
        dto.setCapacity(room.getCapacity());
        dto.setEquipment(room.getEquipment());
        dto.setDescription(room.getDescription());
        dto.setStatus(room.getStatus());
        dto.setStatusName(getRoomStatusName(room.getStatus()));
        dto.setEnabled(room.getEnabled());
        dto.setSortOrder(room.getSortOrder());
        return dto;
    }

    private MeetingBookingDTO toBookingDTO(MeetingBooking booking) {
        MeetingBookingDTO dto = new MeetingBookingDTO();
        dto.setId(booking.getId());
        dto.setBookingNo(booking.getBookingNo());
        dto.setRoomId(booking.getRoomId());
        dto.setTitle(booking.getTitle());
        dto.setOrganizerId(booking.getOrganizerId());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setAttendees(booking.getAttendees());
        dto.setDescription(booking.getDescription());
        dto.setStatus(booking.getStatus());
        dto.setStatusName(getBookingStatusName(booking.getStatus()));
        dto.setCreatedAt(booking.getCreatedAt());

        // 获取会议室名称
        MeetingRoom room = meetingRoomRepository.getById(booking.getRoomId());
        if (room != null) {
            dto.setRoomName(room.getName());
        }

        return dto;
    }
}
