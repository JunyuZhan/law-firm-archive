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
 * 外出登记服务（M8-005）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoOutAppService {

    private final GoOutRecordRepository goOutRepository;
    private final GoOutRecordMapper goOutMapper;
    private final UserRepository userRepository;

    /**
     * 外出登记
     */
    @Transactional
    public GoOutRecordDTO registerGoOut(GoOutCommand command) {
        Long userId = SecurityUtils.getUserId();

        // 检查是否有未返回的外出记录
        List<GoOutRecord> currentOut = goOutMapper.selectCurrentOut(userId);
        if (!currentOut.isEmpty()) {
            throw new BusinessException("您还有未返回的外出记录，请先登记返回");
        }

        // 生成登记编号
        String recordNo = generateRecordNo();

        GoOutRecord record = GoOutRecord.builder()
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
    }

    /**
     * 登记返回
     */
    @Transactional
    public GoOutRecordDTO registerReturn(Long id) {
        GoOutRecord record = goOutRepository.getByIdOrThrow(id, "外出记录不存在");
        
        if (!GoOutRecord.STATUS_OUT.equals(record.getStatus())) {
            throw new BusinessException("该记录不是外出中状态");
        }

        record.setActualReturnTime(LocalDateTime.now());
        record.setStatus(GoOutRecord.STATUS_RETURNED);
        record.setUpdatedBy(SecurityUtils.getUserId());
        record.setUpdatedAt(LocalDateTime.now());
        goOutRepository.updateById(record);

        log.info("外出返回登记成功: recordNo={}", record.getRecordNo());
        return toDTO(record);
    }

    /**
     * 查询我的外出记录
     */
    public List<GoOutRecordDTO> getMyRecords() {
        Long userId = SecurityUtils.getUserId();
        List<GoOutRecord> records = goOutMapper.selectByUserId(userId);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询指定日期范围的外出记录
     */
    public List<GoOutRecordDTO> getRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getUserId();
        List<GoOutRecord> records = goOutMapper.selectByDateRange(userId, startDate, endDate);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询当前外出的记录
     */
    public List<GoOutRecordDTO> getCurrentOut() {
        Long userId = SecurityUtils.getUserId();
        List<GoOutRecord> records = goOutMapper.selectCurrentOut(userId);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private String generateRecordNo() {
        String prefix = "GO" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case GoOutRecord.STATUS_OUT -> "外出中";
            case GoOutRecord.STATUS_RETURNED -> "已返回";
            default -> status;
        };
    }

    private GoOutRecordDTO toDTO(GoOutRecord record) {
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

        // 查询用户名称
        if (record.getUserId() != null) {
            User user = userRepository.findById(record.getUserId());
            if (user != null) {
                dto.setUserName(user.getRealName());
            }
        }

        return dto;
    }
}

