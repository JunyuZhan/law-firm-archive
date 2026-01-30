package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.command.GoOutCommand;
import com.lawfirm.application.admin.dto.GoOutRecordDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.GoOutRecord;
import com.lawfirm.domain.admin.repository.GoOutRecordRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.GoOutRecordMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 外出登记服务（M8-005） */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoOutAppService {

  /** 外出记录仓储 */
  private final GoOutRecordRepository goOutRepository;

  /** 外出记录Mapper */
  private final GoOutRecordMapper goOutMapper;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 外出登记 ✅ 修复：使用数据库约束+异常处理解决并发问题 需要在数据库添加条件唯一索引: UNIQUE(user_id) WHERE status = 'OUT'.
   *
   * @param command 登记命令
   * @return 登记记录DTO
   */
  @Transactional
  public GoOutRecordDTO registerGoOut(final GoOutCommand command) {
    Long userId = SecurityUtils.getUserId();

    // ✅ 验证时间合理性
    if (command.getExpectedReturnTime() != null
        && command.getOutTime().isAfter(command.getExpectedReturnTime())) {
      throw new BusinessException("外出时间不能晚于预计返回时间");
    }

    // 生成登记编号
    String recordNo = generateRecordNo();

    try {
      GoOutRecord record =
          GoOutRecord.builder()
              .recordNo(recordNo)
              .userId(userId)
              .outTime(command.getOutTime())
              .expectedReturnTime(command.getExpectedReturnTime())
              .location(command.getLocation())
              .reason(command.getReason())
              .companions(command.getCompanions())
              .status(GoOutRecord.STATUS_OUT)
              .createdBy(userId)
              .createdAt(LocalDateTime.now())
              .build();

      goOutRepository.save(record);
      log.info("外出登记成功: recordNo={}, userId={}", recordNo, userId);
      return toDTO(record);
    } catch (DuplicateKeyException e) {
      // ✅ 并发时唯一约束冲突，说明已有外出记录
      throw new BusinessException("您还有未返回的外出记录，请先登记返回");
    }
  }

  /**
   * 登记返回 ✅ 修复：添加权限验证，只能登记自己的返回.
   *
   * @param id 记录ID
   * @return 登记记录DTO
   */
  @Transactional
  public GoOutRecordDTO registerReturn(final Long id) {
    GoOutRecord record = goOutRepository.getByIdOrThrow(id, "外出记录不存在");

    if (!GoOutRecord.STATUS_OUT.equals(record.getStatus())) {
      throw new BusinessException("该记录不是外出中状态");
    }

    Long currentUserId = SecurityUtils.getUserId();

    // ✅ 验证权限：只能登记自己的返回，除非是管理员
    if (!record.getUserId().equals(currentUserId)) {
      if (!SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER")) {
        throw new BusinessException("权限不足：只能登记自己的返回");
      }
      log.warn(
          "管理员代登记返回: recordNo={}, operator={}, user={}",
          record.getRecordNo(),
          currentUserId,
          record.getUserId());
    }

    record.setActualReturnTime(LocalDateTime.now());
    record.setStatus(GoOutRecord.STATUS_RETURNED);
    record.setUpdatedBy(currentUserId);
    record.setUpdatedAt(LocalDateTime.now());
    goOutRepository.updateById(record);

    log.info("外出返回登记成功: recordNo={}, user={}", record.getRecordNo(), record.getUserId());
    return toDTO(record);
  }

  /**
   * 查询我的外出记录 ✅ 优化：使用批量加载避免N+1查询.
   *
   * @return 外出记录列表
   */
  public List<GoOutRecordDTO> getMyRecords() {
    Long userId = SecurityUtils.getUserId();
    List<GoOutRecord> records = goOutMapper.selectByUserId(userId);
    return convertToDTOs(records);
  }

  /**
   * 查询指定日期范围的外出记录 ✅ 优化：使用批量加载避免N+1查询.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 外出记录列表
   */
  public List<GoOutRecordDTO> getRecordsByDateRange(
      final LocalDate startDate, final LocalDate endDate) {
    Long userId = SecurityUtils.getUserId();
    List<GoOutRecord> records = goOutMapper.selectByDateRange(userId, startDate, endDate);
    return convertToDTOs(records);
  }

  /**
   * 查询当前外出的记录 ✅ 优化：使用批量加载避免N+1查询.
   *
   * @return 外出记录列表
   */
  public List<GoOutRecordDTO> getCurrentOut() {
    Long userId = SecurityUtils.getUserId();
    List<GoOutRecord> records = goOutMapper.selectCurrentOut(userId);
    return convertToDTOs(records);
  }

  /**
   * 批量转换DTO
   *
   * @param records 外出记录列表
   * @return DTO列表
   */
  private List<GoOutRecordDTO> convertToDTOs(final List<GoOutRecord> records) {
    if (records.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载用户信息
    Set<Long> userIds =
        records.stream()
            .map(GoOutRecord::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO(从Map获取)
    return records.stream().map(record -> toDTO(record, userMap)).collect(Collectors.toList());
  }

  private String generateRecordNo() {
    String prefix = "GO" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case GoOutRecord.STATUS_OUT -> "外出中";
      case GoOutRecord.STATUS_RETURNED -> "已返回";
      default -> status;
    };
  }

  /**
   * 转换为DTO（单条查询使用，会触发数据库查询）
   *
   * @param record 外出记录
   * @return DTO
   */
  private GoOutRecordDTO toDTO(final GoOutRecord record) {
    Map<Long, User> userMap = new HashMap<>();
    if (record.getUserId() != null) {
      User user = userRepository.findById(record.getUserId());
      if (user != null) {
        userMap.put(user.getId(), user);
      }
    }
    return toDTO(record, userMap);
  }

  /**
   * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）
   *
   * @param record 外出记录实体
   * @param userMap 用户Map
   * @return 外出记录DTO
   */
  private GoOutRecordDTO toDTO(final GoOutRecord record, final Map<Long, User> userMap) {
    GoOutRecordDTO dto = new GoOutRecordDTO();
    dto.setId(record.getId());
    dto.setRecordNo(record.getRecordNo());
    dto.setUserId(record.getUserId());
    dto.setOutTime(record.getOutTime());
    dto.setExpectedReturnTime(record.getExpectedReturnTime());
    dto.setActualReturnTime(record.getActualReturnTime());
    dto.setLocation(record.getLocation());
    dto.setReason(record.getReason());
    dto.setCompanions(record.getCompanions());
    dto.setStatus(record.getStatus());
    dto.setStatusName(getStatusName(record.getStatus()));
    dto.setCreatedAt(record.getCreatedAt());
    dto.setUpdatedAt(record.getUpdatedAt());

    // 从Map获取用户名称（避免N+1）
    if (record.getUserId() != null) {
      User user = userMap.get(record.getUserId());
      if (user != null) {
        dto.setUserName(user.getRealName());
      }
    }

    return dto;
  }
}
