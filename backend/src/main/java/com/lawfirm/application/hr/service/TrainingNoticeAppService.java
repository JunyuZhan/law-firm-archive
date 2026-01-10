package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateTrainingNoticeCommand;
import com.lawfirm.application.hr.dto.TrainingCompletionDTO;
import com.lawfirm.application.hr.dto.TrainingNoticeDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Training;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import com.lawfirm.domain.hr.repository.TrainingRecordRepository;
import com.lawfirm.domain.hr.repository.TrainingRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 培训通知应用服务（简化版）
 * 
 * 功能：
 * 1. 行政发布培训通知
 * 2. 律师上传合格证
 * 3. 行政查看完成情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingNoticeAppService {

    private final TrainingRepository trainingRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询培训通知列表
     */
    public PageResult<TrainingNoticeDTO> listNotices(PageQuery query) {
        Page<Training> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Training> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Training::getStatus, "PUBLISHED")
               .orderByDesc(Training::getCreatedAt);
        
        Page<Training> result = trainingRepository.page(page, wrapper);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        int totalEmployees = countActiveEmployees();
        
        List<TrainingNoticeDTO> items = result.getRecords().stream()
                .map(t -> toNoticeDTO(t, currentUserId, totalEmployees))
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取通知详情
     */
    public TrainingNoticeDTO getNoticeById(Long id) {
        Training training = trainingRepository.getById(id);
        if (training == null) {
            throw new BusinessException("培训通知不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        int totalEmployees = countActiveEmployees();
        return toNoticeDTO(training, currentUserId, totalEmployees);
    }

    /**
     * 发布培训通知
     * 问题342修复：使用Jackson序列化附件JSON
     */
    @Transactional
    public TrainingNoticeDTO createNotice(CreateTrainingNoticeCommand command) {
        // 使用Jackson序列化附件
        String attachmentsJson = serializeAttachments(command.getAttachments());
        
        LocalDateTime now = LocalDateTime.now();
        Training training = Training.builder()
                .title(command.getTitle())
                .trainingType("NOTICE")  // 使用NOTICE类型标识培训通知
                .description(command.getContent())
                .materialsUrl(attachmentsJson)  // 存储附件JSON
                .status("PUBLISHED")
                .startTime(now)  // 培训通知的开始时间设为当前时间
                .endTime(now.plusYears(10))  // 结束时间设为10年后（长期有效）
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        
        trainingRepository.save(training);
        log.info("发布培训通知: {}", training.getTitle());
        
        return toNoticeDTO(training, null, 0);
    }

    /**
     * 删除培训通知
     */
    @Transactional
    public void deleteNotice(Long id) {
        Training training = trainingRepository.getById(id);
        if (training == null) {
            throw new BusinessException("培训通知不存在");
        }
        
        // 删除相关完成记录
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getTrainingId, id);
        trainingRecordRepository.remove(wrapper);
        
        // 删除通知
        trainingRepository.removeById(id);
        log.info("删除培训通知: {}", training.getTitle());
    }

    /**
     * 完成培训（上传合格证）
     */
    @Transactional
    public void completeTraining(Long noticeId, String certificateUrl, String certificateName) {
        Long employeeId = SecurityUtils.getCurrentUserId();
        
        Training training = trainingRepository.getById(noticeId);
        if (training == null) {
            throw new BusinessException("培训通知不存在");
        }
        
        // 检查是否已完成
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getTrainingId, noticeId)
               .eq(TrainingRecord::getEmployeeId, employeeId);
        TrainingRecord existingRecord = trainingRecordRepository.getOne(wrapper);
        
        if (existingRecord != null) {
            // 更新证书
            existingRecord.setCertificateUrl(certificateUrl);
            existingRecord.setRemarks(certificateName);
            existingRecord.setStatus("COMPLETED");
            trainingRecordRepository.updateById(existingRecord);
        } else {
            // 创建新记录
            TrainingRecord record = TrainingRecord.builder()
                    .trainingId(noticeId)
                    .employeeId(employeeId)
                    .enrollTime(LocalDateTime.now())
                    .status("COMPLETED")
                    .certificateUrl(certificateUrl)
                    .remarks(certificateName)
                    .build();
            trainingRecordRepository.save(record);
        }
        
        log.info("完成培训: noticeId={}, employeeId={}", noticeId, employeeId);
    }

    /**
     * 获取完成情况列表（管理员）
     * 问题341修复：使用批量加载避免N+1查询
     */
    public PageResult<TrainingCompletionDTO> listCompletions(PageQuery query) {
        Page<TrainingRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<TrainingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingRecord::getStatus, "COMPLETED")
               .isNotNull(TrainingRecord::getCertificateUrl)
               .orderByDesc(TrainingRecord::getCreatedAt);
        
        Page<TrainingRecord> result = trainingRecordRepository.page(page, wrapper);
        List<TrainingRecord> records = result.getRecords();
        
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
        }
        
        // 批量加载培训信息
        Set<Long> trainingIds = records.stream()
                .map(TrainingRecord::getTrainingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Training> trainingMap = trainingIds.isEmpty() ? Collections.emptyMap() :
                trainingRepository.listByIds(new ArrayList<>(trainingIds)).stream()
                        .collect(Collectors.toMap(Training::getId, t -> t));
        
        // 批量加载用户信息
        Set<Long> userIds = records.stream()
                .map(TrainingRecord::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.listByIds(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        
        // 批量加载部门信息
        Set<Long> deptIds = userMap.values().stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Department> deptMap = deptIds.isEmpty() ? Collections.emptyMap() :
                departmentRepository.listByIds(new ArrayList<>(deptIds)).stream()
                        .collect(Collectors.toMap(Department::getId, d -> d));
        
        List<TrainingCompletionDTO> items = records.stream()
                .map(r -> toCompletionDTO(r, trainingMap, userMap, deptMap))
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    // ==================== 私有方法 ====================

    private TrainingNoticeDTO toNoticeDTO(Training training, Long currentUserId, int totalEmployees) {
        TrainingNoticeDTO dto = new TrainingNoticeDTO();
        dto.setId(training.getId());
        dto.setTitle(training.getTitle());
        dto.setContent(training.getDescription());
        dto.setStatus(training.getStatus());
        dto.setStatusName("PUBLISHED".equals(training.getStatus()) ? "已发布" : training.getStatus());
        dto.setPublishedAt(training.getCreatedAt());
        dto.setCreatedAt(training.getCreatedAt());
        
        // 解析附件
        if (training.getMaterialsUrl() != null && training.getMaterialsUrl().startsWith("[")) {
            dto.setAttachments(parseAttachments(training.getMaterialsUrl()));
        }
        
        // 统计完成情况
        int completedCount = trainingRecordRepository.countCompletedByTrainingId(training.getId());
        dto.setCompletedCount(completedCount);
        dto.setTotalCount(totalEmployees);
        
        // 当前用户完成状态
        if (currentUserId != null) {
            TrainingRecord myRecord = trainingRecordRepository.findByTrainingIdAndEmployeeId(
                    training.getId(), currentUserId);
            if (myRecord != null && "COMPLETED".equals(myRecord.getStatus())) {
                dto.setMyCompleted(true);
                dto.setMyCertificateUrl(myRecord.getCertificateUrl());
            } else {
                dto.setMyCompleted(false);
            }
        }
        
        return dto;
    }

    /**
     * 转换为完成记录DTO（单个，会查询）
     */
    private TrainingCompletionDTO toCompletionDTO(TrainingRecord record) {
        return toCompletionDTO(record, null, null, null);
    }

    /**
     * 转换为完成记录DTO（批量优化版本）
     * 问题341修复：从Map获取关联数据，避免N+1查询
     */
    private TrainingCompletionDTO toCompletionDTO(TrainingRecord record, 
            Map<Long, Training> trainingMap, Map<Long, User> userMap, Map<Long, Department> deptMap) {
        TrainingCompletionDTO dto = new TrainingCompletionDTO();
        dto.setId(record.getId());
        dto.setNoticeId(record.getTrainingId());
        dto.setEmployeeId(record.getEmployeeId());
        dto.setCertificateUrl(record.getCertificateUrl());
        dto.setCertificateName(record.getRemarks());
        dto.setUploadedAt(record.getCreatedAt());
        
        // 获取培训标题
        Training training = (trainingMap != null) ? trainingMap.get(record.getTrainingId())
                                                  : trainingRepository.getById(record.getTrainingId());
        if (training != null) {
            dto.setNoticeTitle(training.getTitle());
        }
        
        // 获取员工信息
        User user = (userMap != null) ? userMap.get(record.getEmployeeId())
                                      : userRepository.getById(record.getEmployeeId());
        if (user != null) {
            dto.setEmployeeName(user.getRealName());
            // 获取部门名称
            if (user.getDepartmentId() != null) {
                Department dept = (deptMap != null) ? deptMap.get(user.getDepartmentId())
                                                    : departmentRepository.getById(user.getDepartmentId());
                if (dept != null) {
                    dto.setDepartmentName(dept.getName());
                }
            }
        }
        
        return dto;
    }

    private int countActiveEmployees() {
        // 简单统计活跃员工数（状态为ENABLED的用户）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, "ENABLED");
        return (int) userRepository.count(wrapper);
    }

    /**
     * 解析附件JSON
     * 问题342修复：使用Jackson库解析JSON，更安全可靠
     */
    private List<TrainingNoticeDTO.AttachmentDTO> parseAttachments(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(json, 
                    new TypeReference<List<TrainingNoticeDTO.AttachmentDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析附件JSON失败，使用降级方案: {}", json, e);
            // 降级方案：返回空列表，不阻断主流程
            return Collections.emptyList();
        }
    }

    /**
     * 序列化附件为JSON
     * 问题342修复：使用Jackson库序列化JSON
     */
    private String serializeAttachments(List<CreateTrainingNoticeCommand.AttachmentCommand> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            log.error("序列化附件失败", e);
            throw new BusinessException("附件数据序列化失败");
        }
    }
}

