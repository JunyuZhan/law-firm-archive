package com.lawfirm.application.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** 会议记录服务（M8-023） */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRecordAppService {

  /** 会议记录仓储 */
  private final MeetingRecordRepository recordRepository;

  /** 会议记录Mapper */
  private final MeetingRecordMapper recordMapper;

  /** 会议室仓储 */
  private final MeetingRoomRepository roomRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /**
   * 创建会议记录
   *
   * @param command 创建命令
   * @return 会议记录DTO
   */
  @Transactional
  public MeetingRecordDTO createRecord(final CreateMeetingRecordCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 验证会议室存在
    roomRepository.getByIdOrThrow(command.getRoomId(), "会议室不存在");

    // 生成记录编号
    String recordNo = generateRecordNo();

    // 序列化参会人员（问题411修复：序列化失败抛出异常）
    String attendeesJson = null;
    if (command.getAttendeeIds() != null && !command.getAttendeeIds().isEmpty()) {
      try {
        attendeesJson = objectMapper.writeValueAsString(command.getAttendeeIds());
      } catch (JsonProcessingException e) {
        log.error("序列化参会人员失败: {}", command.getAttendeeIds(), e);
        throw new BusinessException("参会人员数据格式错误");
      }
    }

    MeetingRecord record =
        MeetingRecord.builder()
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
   * 根据预约ID创建会议记录 问题409修复：使用唯一约束防止并发创建重复记录
   *
   * @param bookingId 预约ID
   * @param command 创建命令
   * @return 会议记录DTO
   */
  @Transactional
  public MeetingRecordDTO createRecordFromBooking(
      final Long bookingId, final CreateMeetingRecordCommand command) {
    try {
      command.setBookingId(bookingId);
      return createRecord(command);
    } catch (org.springframework.dao.DuplicateKeyException e) {
      // 唯一约束冲突，说明已有记录
      throw new BusinessException("该预约已有会议记录");
    }
  }

  /**
   * 查询会议记录 问题412修复：添加权限验证
   *
   * @param id 记录ID
   * @return 会议记录DTO
   */
  public MeetingRecordDTO getRecordById(final Long id) {
    MeetingRecord record = recordRepository.getByIdOrThrow(id, "会议记录不存在");
    validateRecordAccess(record);
    return toDTO(record);
  }

  /**
   * 查询会议室的会议记录 问题404修复：使用批量加载避免N+1查询 问题412修复：添加权限验证（管理员或HR经理可查看所有）
   *
   * @param roomId 会议室ID
   * @return 会议记录列表
   */
  public List<MeetingRecordDTO> getRecordsByRoom(final Long roomId) {
    // 管理员或HR经理可查看所有记录
    if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR_MANAGER")) {
      throw new BusinessException("权限不足：只有管理员才能按会议室查询所有记录");
    }
    List<MeetingRecord> records = recordMapper.selectByRoomId(roomId);
    return convertToDTOs(records);
  }

  /**
   * 查询指定日期范围的会议记录 问题404修复：使用批量加载避免N+1查询 问题412修复：添加权限验证（管理员或HR经理可查看所有）
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 会议记录列表
   */
  public List<MeetingRecordDTO> getRecordsByDateRange(
      final LocalDate startDate, final LocalDate endDate) {
    // 管理员或HR经理可查看所有记录
    if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR_MANAGER")) {
      throw new BusinessException("权限不足：只有管理员才能按日期查询所有记录");
    }
    List<MeetingRecord> records = recordMapper.selectByDateRange(startDate, endDate);
    return convertToDTOs(records);
  }

  /**
   * 问题412修复：验证会议记录访问权限
   *
   * @param record 会议记录
   */
  private void validateRecordAccess(final MeetingRecord record) {
    Long currentUserId = SecurityUtils.getUserId();

    // 组织者可以查看
    if (record.getOrganizerId() != null && record.getOrganizerId().equals(currentUserId)) {
      return;
    }

    // 管理员或HR经理可以查看
    if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR_MANAGER")) {
      return;
    }

    // 参会人员可以查看
    if (record.getAttendees() != null) {
      try {
        List<Long> attendeeIds =
            objectMapper.readValue(record.getAttendees(), new TypeReference<List<Long>>() {});
        if (attendeeIds.contains(currentUserId)) {
          return;
        }
      } catch (JsonProcessingException e) {
        log.warn("解析参会人员失败", e);
      }
    }

    throw new BusinessException("权限不足：只能查看自己组织或参加的会议记录");
  }

  /**
   * 问题404修复：批量转换DTO，避免N+1查询
   *
   * @param records 会议记录列表
   * @return DTO列表
   */
  private List<MeetingRecordDTO> convertToDTOs(final List<MeetingRecord> records) {
    if (records.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载会议室信息
    Set<Long> roomIds =
        records.stream()
            .map(MeetingRecord::getRoomId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, MeetingRoom> roomMap =
        roomIds.isEmpty()
            ? Collections.emptyMap()
            : roomRepository.listByIds(new ArrayList<>(roomIds)).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, r -> r));

    // 批量加载组织者信息
    Set<Long> organizerIds =
        records.stream()
            .map(MeetingRecord::getOrganizerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        organizerIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(organizerIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO（从Map获取）
    return records.stream().map(r -> toDTO(r, roomMap, userMap)).collect(Collectors.toList());
  }

  /**
   * 问题404修复：带Map参数的toDTO方法，避免重复查询
   *
   * @param record 会议记录实体
   * @param roomMap 会议室Map
   * @param userMap 用户Map
   * @return 会议记录DTO
   */
  private MeetingRecordDTO toDTO(
      final MeetingRecord record,
      final Map<Long, MeetingRoom> roomMap,
      final Map<Long, User> userMap) {
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

    // 从Map获取会议室名称
    if (record.getRoomId() != null) {
      MeetingRoom room = roomMap.get(record.getRoomId());
      if (room != null) {
        dto.setRoomName(room.getName());
      }
    }

    // 从Map获取组织者名称
    if (record.getOrganizerId() != null) {
      User organizer = userMap.get(record.getOrganizerId());
      if (organizer != null) {
        dto.setOrganizerName(organizer.getRealName());
      }
    }

    return dto;
  }

  private String generateRecordNo() {
    String prefix = "MR" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  private MeetingRecordDTO toDTO(final MeetingRecord record) {
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
