package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateTrainingCommand;
import com.lawfirm.application.hr.dto.TrainingDTO;
import com.lawfirm.application.hr.dto.TrainingRecordDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Training;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import com.lawfirm.domain.hr.repository.TrainingRecordRepository;
import com.lawfirm.domain.hr.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 培训管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingAppService {

    private final TrainingRepository trainingRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    /**
     * 分页查询培训列表
     */
    public PageResult<TrainingDTO> listTrainings(PageQuery query, String keyword, String status) {
        Page<Training> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Training> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Training::getTitle, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Training::getStatus, status);
        }
        wrapper.orderByDesc(Training::getStartTime);
        
        Page<Training> result = trainingRepository.page(page, wrapper);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<TrainingDTO> items = result.getRecords().stream()
                .map(t -> toDTO(t, currentUserId))
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取可报名的培训列表
     */
    public List<TrainingDTO> getAvailableTrainings() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return trainingRepository.findAvailableTrainings().stream()
                .map(t -> toDTO(t, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取培训详情
     */
    public TrainingDTO getTrainingById(Long id) {
        Training training = trainingRepository.getById(id);
        if (training == null) {
            throw new BusinessException("培训不存在");
        }
        return toDTO(training, SecurityUtils.getCurrentUserId());
    }

    /**
     * 创建培训计划
     */
    @Transactional
    public TrainingDTO createTraining(CreateTrainingCommand command) {
        Training training = Training.builder()
                .title(command.getTitle())
                .trainingType(command.getTrainingType())
                .category(command.getCategory())
                .description(command.getDescription())
                .trainer(command.getTrainer())
                .location(command.getLocation())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .duration(command.getDuration())
                .credits(command.getCredits())
                .maxParticipants(command.getMaxParticipants())
                .currentParticipants(0)
                .enrollDeadline(command.getEnrollDeadline())
                .status("DRAFT")
                .materialsUrl(command.getMaterialsUrl())
                .remarks(command.getRemarks())
                .build();
        
        trainingRepository.save(training);
        log.info("创建培训计划: {}", training.getTitle());
        return toDTO(training, null);
    }

    /**
     * 发布培训
     */
    @Transactional
    public void publishTraining(Long id) {
        Training training = trainingRepository.getById(id);
        if (training == null) {
            throw new BusinessException("培训不存在");
        }
        if (!"DRAFT".equals(training.getStatus())) {
            throw new BusinessException("只有草稿状态的培训可以发布");
        }
        
        training.setStatus("PUBLISHED");
        trainingRepository.updateById(training);
        log.info("发布培训: {}", training.getTitle());
    }

    /**
     * 取消培训
     */
    @Transactional
    public void cancelTraining(Long id) {
        Training training = trainingRepository.getById(id);
        if (training == null) {
            throw new BusinessException("培训不存在");
        }
        
        training.setStatus("CANCELLED");
        trainingRepository.updateById(training);
        log.info("取消培训: {}", training.getTitle());
    }

    /**
     * 报名培训
     */
    @Transactional
    public void enrollTraining(Long trainingId) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        
        Training training = trainingRepository.getById(trainingId);
        if (training == null) {
            throw new BusinessException("培训不存在");
        }
        if (!"PUBLISHED".equals(training.getStatus())) {
            throw new BusinessException("该培训不可报名");
        }
        if (training.getEnrollDeadline() != null && training.getEnrollDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("报名已截止");
        }
        if (training.getMaxParticipants() != null && training.getCurrentParticipants() >= training.getMaxParticipants()) {
            throw new BusinessException("报名人数已满");
        }
        if (trainingRecordRepository.hasEnrolled(trainingId, employeeId)) {
            throw new BusinessException("您已报名该培训");
        }
        
        TrainingRecord record = TrainingRecord.builder()
                .trainingId(trainingId)
                .employeeId(employeeId)
                .enrollTime(LocalDateTime.now())
                .status("ENROLLED")
                .build();
        
        trainingRecordRepository.save(record);
        trainingRepository.incrementParticipants(trainingId);
        
        log.info("报名培训: trainingId={}, employeeId={}", trainingId, employeeId);
    }

    /**
     * 取消报名
     */
    @Transactional
    public void cancelEnrollment(Long trainingId) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getTrainingId, trainingId)
               .eq(TrainingRecord::getEmployeeId, employeeId)
               .eq(TrainingRecord::getStatus, "ENROLLED");
        
        TrainingRecord record = trainingRecordRepository.getOne(wrapper);
        if (record == null) {
            throw new BusinessException("未找到报名记录");
        }
        
        record.setStatus("CANCELLED");
        trainingRecordRepository.updateById(record);
        trainingRepository.decrementParticipants(trainingId);
        
        log.info("取消报名: trainingId={}, employeeId={}", trainingId, employeeId);
    }

    /**
     * 获取我的培训记录
     */
    public List<TrainingRecordDTO> getMyTrainingRecords() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return trainingRecordRepository.findByEmployeeId(employeeId).stream()
                .map(this::toRecordDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我的学分统计
     */
    public int getMyTotalCredits() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return trainingRecordRepository.sumCreditsByEmployeeId(employeeId);
    }

    /**
     * 获取培训参与者列表
     */
    public List<TrainingRecordDTO> getTrainingParticipants(Long trainingId) {
        return trainingRecordRepository.findByTrainingId(trainingId).stream()
                .map(this::toRecordDTO)
                .collect(Collectors.toList());
    }

    private TrainingDTO toDTO(Training training, Long currentUserId) {
        TrainingDTO dto = TrainingDTO.builder()
                .id(training.getId())
                .title(training.getTitle())
                .trainingType(training.getTrainingType())
                .category(training.getCategory())
                .description(training.getDescription())
                .trainer(training.getTrainer())
                .location(training.getLocation())
                .startTime(training.getStartTime())
                .endTime(training.getEndTime())
                .duration(training.getDuration())
                .credits(training.getCredits())
                .maxParticipants(training.getMaxParticipants())
                .currentParticipants(training.getCurrentParticipants())
                .enrollDeadline(training.getEnrollDeadline())
                .status(training.getStatus())
                .materialsUrl(training.getMaterialsUrl())
                .remarks(training.getRemarks())
                .createdAt(training.getCreatedAt())
                .updatedAt(training.getUpdatedAt())
                .build();
        
        // 计算是否可报名
        boolean canEnroll = "PUBLISHED".equals(training.getStatus())
                && (training.getEnrollDeadline() == null || !training.getEnrollDeadline().isBefore(LocalDate.now()))
                && (training.getMaxParticipants() == null || training.getCurrentParticipants() < training.getMaxParticipants());
        dto.setCanEnroll(canEnroll);
        
        // 检查当前用户是否已报名
        if (currentUserId != null) {
            dto.setEnrolled(trainingRecordRepository.hasEnrolled(training.getId(), currentUserId));
        }
        
        return dto;
    }

    private TrainingRecordDTO toRecordDTO(TrainingRecord record) {
        TrainingRecordDTO dto = TrainingRecordDTO.builder()
                .id(record.getId())
                .trainingId(record.getTrainingId())
                .employeeId(record.getEmployeeId())
                .enrollTime(record.getEnrollTime())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .actualDuration(record.getActualDuration())
                .status(record.getStatus())
                .score(record.getScore())
                .passed(record.getPassed())
                .earnedCredits(record.getEarnedCredits())
                .feedback(record.getFeedback())
                .rating(record.getRating())
                .certificateUrl(record.getCertificateUrl())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .build();
        
        // 查询培训标题
        Training training = trainingRepository.getById(record.getTrainingId());
        if (training != null) {
            dto.setTrainingTitle(training.getTitle());
        }
        
        return dto;
    }
}
