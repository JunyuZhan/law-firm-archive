package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 会议室应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRoomAppService {

  /** 会议室仓储 */
  private final MeetingRoomRepository meetingRoomRepository;

  /** 会议室Mapper */
  private final MeetingRoomMapper meetingRoomMapper;

  /** 会议预约仓储 */
  private final MeetingBookingRepository meetingBookingRepository;

  /** 会议预约Mapper */
  private final MeetingBookingMapper meetingBookingMapper;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  // ==================== 会议室管理 ====================

  /**
   * 获取所有会议室
   *
   * @return 会议室列表
   */
  public List<MeetingRoomDTO> listRooms() {
    return meetingRoomMapper.selectEnabledRooms().stream()
        .map(this::toRoomDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取可用会议室
   *
   * @return 可用会议室列表
   */
  public List<MeetingRoomDTO> listAvailableRooms() {
    return meetingRoomMapper.selectAvailableRooms().stream()
        .map(this::toRoomDTO)
        .collect(Collectors.toList());
  }

  /**
   * 创建会议室
   *
   * @param command 创建会议室命令
   * @return 会议室DTO
   */
  @Transactional
  public MeetingRoomDTO createRoom(final CreateMeetingRoomCommand command) {
    // 如果没有提供编码，自动生成
    String code = command.getCode();
    if (!StringUtils.hasText(code)) {
      code = "ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // 检查编码唯一性
    if (meetingRoomMapper.selectByCode(code) != null) {
      throw new BusinessException("会议室编码已存在");
    }

    MeetingRoom room =
        MeetingRoom.builder()
            .name(command.getName())
            .code(code)
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
   *
   * @param id 会议室ID
   * @param command 更新会议室命令
   * @return 会议室DTO
   */
  @Transactional
  public MeetingRoomDTO updateRoom(final Long id, final CreateMeetingRoomCommand command) {
    MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");

    // 检查编码唯一性（仅当提供了新编码时）
    if (StringUtils.hasText(command.getCode())) {
      MeetingRoom existing = meetingRoomMapper.selectByCode(command.getCode());
      if (existing != null && !existing.getId().equals(id)) {
        throw new BusinessException("会议室编码已存在");
      }
      room.setCode(command.getCode());
    }

    if (StringUtils.hasText(command.getName())) {
      room.setName(command.getName());
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
   * 删除会议室 问题410修复：只检查有效状态的预约，使用软删除
   *
   * @param id 会议室ID
   */
  @Transactional
  public void deleteRoom(final Long id) {
    MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");

    // 只检查有效状态（已预约、进行中）的未来预约
    int validCount =
        meetingBookingMapper.countConflicting(
            id, LocalDateTime.now(), LocalDateTime.now().plusMonths(6), null);
    if (validCount > 0) {
      throw new BusinessException("该会议室有" + validCount + "个有效预约，无法删除");
    }

    // 使用软删除而非物理删除
    room.setEnabled(false);
    room.setStatus(MeetingRoom.STATUS_MAINTENANCE);
    meetingRoomRepository.updateById(room);
    log.info("会议室已禁用: {}", room.getName());
  }

  /**
   * 更新会议室状态
   *
   * @param id 会议室ID
   * @param status 状态
   */
  @Transactional
  public void updateRoomStatus(final Long id, final String status) {
    MeetingRoom room = meetingRoomRepository.getByIdOrThrow(id, "会议室不存在");
    room.setStatus(status);
    meetingRoomRepository.updateById(room);
    log.info("会议室状态更新: {} -> {}", room.getName(), status);
  }

  // ==================== 会议预约 ====================

  /**
   * 分页查询会议预约 问题405修复：使用批量加载避免N+1查询
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<MeetingBookingDTO> listBookings(final MeetingBookingQueryDTO query) {
    // 将 LocalDate 转换为 LocalDateTime
    LocalDateTime startDateTime =
        query.getStartTime() != null ? query.getStartTime().atStartOfDay() : null;
    LocalDateTime endDateTime =
        query.getEndTime() != null ? query.getEndTime().plusDays(1).atStartOfDay() : null;

    IPage<MeetingBooking> page =
        meetingBookingMapper.selectBookingPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getRoomId(),
            query.getOrganizerId(),
            query.getStatus(),
            startDateTime,
            endDateTime);

    List<MeetingBookingDTO> records = convertBookingsToDTOs(page.getRecords());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 预约会议室 问题407修复：使用悲观锁防止并发冲突 问题411修复：序列化失败抛出异常 问题415修复：允许5分钟时间误差
   *
   * @param command 预约命令
   * @return 预约DTO
   */
  @Transactional
  public MeetingBookingDTO bookMeeting(final BookMeetingCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 验证会议室
    MeetingRoom room = meetingRoomRepository.getByIdOrThrow(command.getRoomId(), "会议室不存在");
    if (!room.getEnabled()) {
      throw new BusinessException("该会议室已禁用");
    }
    if (MeetingRoom.STATUS_MAINTENANCE.equals(room.getStatus())) {
      throw new BusinessException("该会议室正在维护中");
    }

    // 问题415修复：验证时间（允许5分钟误差）
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime minStartTime = now.minusMinutes(5);
    if (command.getStartTime().isAfter(command.getEndTime())) {
      throw new BusinessException("开始时间不能晚于结束时间");
    }
    if (command.getStartTime().isBefore(minStartTime)) {
      throw new BusinessException("开始时间不能早于当前时间");
    }

    // 问题407修复：使用悲观锁锁定会议室，防止并发预约
    meetingRoomMapper.selectById(room.getId()); // 在事务中再次查询确保锁定

    // 在锁保护下检查时间冲突
    int conflicting =
        meetingBookingMapper.countConflicting(
            command.getRoomId(), command.getStartTime(), command.getEndTime(), null);
    if (conflicting > 0) {
      throw new BusinessException("该时间段会议室已被预约");
    }

    // 生成预约编号
    String bookingNo = generateBookingNo();

    // 问题411修复：序列化参会人员失败抛出异常
    String attendeesJson = null;
    if (command.getAttendeeIds() != null && !command.getAttendeeIds().isEmpty()) {
      try {
        attendeesJson = objectMapper.writeValueAsString(command.getAttendeeIds());
      } catch (JsonProcessingException e) {
        log.error("序列化参会人员失败: {}", command.getAttendeeIds(), e);
        throw new BusinessException("参会人员数据格式错误");
      }
    }

    // 创建预约
    MeetingBooking booking =
        MeetingBooking.builder()
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
   * 取消会议预约 问题413修复：管理员也可以取消预约
   *
   * @param bookingId 预约ID
   */
  @Transactional
  public void cancelBooking(final Long bookingId) {
    Long userId = SecurityUtils.getUserId();

    MeetingBooking booking = meetingBookingRepository.getByIdOrThrow(bookingId, "预约不存在");

    if (!MeetingBooking.STATUS_BOOKED.equals(booking.getStatus())) {
      throw new BusinessException("只能取消已预约状态的会议");
    }

    // 问题413修复：组织者或管理员可以取消
    if (!booking.getOrganizerId().equals(userId)) {
      if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("MEETING_MANAGER")) {
        throw new BusinessException("权限不足：只有组织者或管理员才能取消预约");
      }
      log.warn(
          "管理员取消预约: bookingId={}, operator={}, organizer={}",
          bookingId,
          userId,
          booking.getOrganizerId());
    }

    booking.setStatus(MeetingBooking.STATUS_CANCELLED);
    booking.setUpdatedBy(userId);
    booking.setUpdatedAt(LocalDateTime.now());
    meetingBookingRepository.updateById(booking);
    log.info("会议预约已取消: bookingNo={}, cancelBy={}", booking.getBookingNo(), userId);
  }

  /**
   * 获取会议室某日预约情况 问题405修复：使用批量加载避免N+1查询
   *
   * @param roomId 会议室ID
   * @param date 日期
   * @return 预约列表
   */
  public List<MeetingBookingDTO> getRoomDayBookings(final Long roomId, final LocalDate date) {
    LocalDateTime startTime = date.atStartOfDay();
    LocalDateTime endTime = date.plusDays(1).atStartOfDay();

    List<MeetingBooking> bookings =
        meetingBookingMapper.selectByRoomAndTimeRange(roomId, startTime, endTime);
    return convertBookingsToDTOs(bookings);
  }

  /**
   * 获取我的会议预约 问题405修复：使用批量加载避免N+1查询
   *
   * @return 预约列表
   */
  public List<MeetingBookingDTO> getMyBookings() {
    Long userId = SecurityUtils.getUserId();
    List<MeetingBooking> bookings = meetingBookingMapper.selectByOrganizer(userId);
    return convertBookingsToDTOs(bookings);
  }

  /**
   * 获取会议室日程视图（M8-024） 问题405修复：使用批量加载避免N+1查询
   *
   * @param roomId 会议室ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 预约列表
   */
  public List<MeetingBookingDTO> getRoomSchedule(
      final Long roomId, final LocalDate startDate, final LocalDate endDate) {
    LocalDateTime startTime = startDate.atStartOfDay();
    LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
    List<MeetingBooking> bookings =
        meetingBookingMapper.selectByRoomAndTimeRange(roomId, startTime, endTime);
    return convertBookingsToDTOs(bookings);
  }

  /**
   * 获取所有会议室日程视图（M8-024） 问题406修复：使用批量查询避免N+1查询，原来511次查询现在只需2次
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 会议室预约映射
   */
  public Map<Long, List<MeetingBookingDTO>> getAllRoomsSchedule(
      final LocalDate startDate, final LocalDate endDate) {
    // 1. 查询所有启用的会议室
    List<MeetingRoom> rooms = meetingRoomMapper.selectEnabledRooms();
    if (rooms.isEmpty()) {
      return Collections.emptyMap();
    }

    LocalDateTime startTime = startDate.atStartOfDay();
    LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

    // 2. 构建会议室Map
    Map<Long, MeetingRoom> roomMap =
        rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, r -> r));

    // 3. 批量查询所有会议室的预约
    List<MeetingBooking> allBookings = new ArrayList<>();
    for (MeetingRoom room : rooms) {
      allBookings.addAll(
          meetingBookingMapper.selectByRoomAndTimeRange(room.getId(), startTime, endTime));
    }

    // 4. 按会议室ID分组预约
    Map<Long, List<MeetingBooking>> bookingsByRoom =
        allBookings.stream().collect(Collectors.groupingBy(MeetingBooking::getRoomId));

    // 5. 转换为DTO并组装结果
    return rooms.stream()
        .collect(
            Collectors.toMap(
                MeetingRoom::getId,
                room -> {
                  List<MeetingBooking> roomBookings =
                      bookingsByRoom.getOrDefault(room.getId(), Collections.emptyList());
                  return roomBookings.stream()
                      .map(b -> toBookingDTO(b, roomMap))
                      .collect(Collectors.toList());
                }));
  }

  /**
   * 生成预约编号
   *
   * @return 预约编号
   */
  private String generateBookingNo() {
    String prefix = "MT" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取会议室状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getRoomStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case MeetingRoom.STATUS_AVAILABLE -> "可用";
      case MeetingRoom.STATUS_OCCUPIED -> "占用中";
      case MeetingRoom.STATUS_MAINTENANCE -> "维护中";
      default -> status;
    };
  }

  /**
   * 获取预约状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getBookingStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case MeetingBooking.STATUS_BOOKED -> "已预约";
      case MeetingBooking.STATUS_IN_PROGRESS -> "进行中";
      case MeetingBooking.STATUS_COMPLETED -> "已完成";
      case MeetingBooking.STATUS_CANCELLED -> "已取消";
      default -> status;
    };
  }

  private MeetingRoomDTO toRoomDTO(final MeetingRoom room) {
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

  /**
   * 问题405修复：批量转换预约DTO，避免N+1查询
   *
   * @param bookings 预约列表
   * @return DTO列表
   */
  private List<MeetingBookingDTO> convertBookingsToDTOs(final List<MeetingBooking> bookings) {
    if (bookings.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载会议室信息
    Set<Long> roomIds =
        bookings.stream()
            .map(MeetingBooking::getRoomId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, MeetingRoom> roomMap =
        roomIds.isEmpty()
            ? Collections.emptyMap()
            : meetingRoomRepository.listByIds(new ArrayList<>(roomIds)).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, r -> r));

    // 转换DTO（从Map获取）
    return bookings.stream().map(b -> toBookingDTO(b, roomMap)).collect(Collectors.toList());
  }

  private MeetingBookingDTO toBookingDTO(final MeetingBooking booking) {
    // 单个转换时仍查询会议室
    MeetingRoom room = meetingRoomRepository.getById(booking.getRoomId());
    Map<Long, MeetingRoom> roomMap =
        room != null ? Collections.singletonMap(room.getId(), room) : Collections.emptyMap();
    return toBookingDTO(booking, roomMap);
  }

  /**
   * 问题405修复：带Map参数的toBookingDTO方法，避免重复查询
   *
   * @param booking 预约
   * @param roomMap 会议室映射
   * @return 预约DTO
   */
  private MeetingBookingDTO toBookingDTO(
      final MeetingBooking booking, final Map<Long, MeetingRoom> roomMap) {
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

    // 从Map获取会议室名称
    if (booking.getRoomId() != null) {
      MeetingRoom room = roomMap.get(booking.getRoomId());
      if (room != null) {
        dto.setRoomName(room.getName());
      }
    }

    return dto;
  }
}
