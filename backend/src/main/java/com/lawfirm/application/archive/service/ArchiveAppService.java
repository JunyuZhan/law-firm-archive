package com.lawfirm.application.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.dto.ArchiveQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.archive.entity.Archive;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import com.lawfirm.domain.archive.entity.ArchiveOperationLog;
import com.lawfirm.domain.archive.repository.ArchiveRepository;
import com.lawfirm.domain.archive.repository.ArchiveLocationRepository;
import com.lawfirm.domain.archive.repository.ArchiveOperationLogRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 档案管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveAppService {

    private final ArchiveRepository archiveRepository;
    private final ArchiveMapper archiveMapper;
    private final ArchiveLocationRepository locationRepository;
    private final ArchiveOperationLogRepository operationLogRepository;
    private final MatterRepository matterRepository;

    /**
     * 分页查询档案
     */
    public PageResult<ArchiveDTO> listArchives(ArchiveQueryDTO query) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getArchiveNo())) {
            wrapper.like(Archive::getArchiveNo, query.getArchiveNo());
        }
        if (StringUtils.hasText(query.getArchiveName())) {
            wrapper.like(Archive::getArchiveName, query.getArchiveName());
        }
        if (StringUtils.hasText(query.getMatterNo())) {
            wrapper.like(Archive::getMatterNo, query.getMatterNo());
        }
        if (StringUtils.hasText(query.getMatterName())) {
            wrapper.like(Archive::getMatterName, query.getMatterName());
        }
        if (StringUtils.hasText(query.getClientName())) {
            wrapper.like(Archive::getClientName, query.getClientName());
        }
        if (StringUtils.hasText(query.getArchiveType())) {
            wrapper.eq(Archive::getArchiveType, query.getArchiveType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Archive::getStatus, query.getStatus());
        }
        if (query.getLocationId() != null) {
            wrapper.eq(Archive::getLocationId, query.getLocationId());
        }
        if (query.getCaseCloseDateFrom() != null) {
            wrapper.ge(Archive::getCaseCloseDate, query.getCaseCloseDateFrom());
        }
        if (query.getCaseCloseDateTo() != null) {
            wrapper.le(Archive::getCaseCloseDate, query.getCaseCloseDateTo());
        }
        
        wrapper.orderByDesc(Archive::getCreatedAt);

        IPage<Archive> page = archiveRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<ArchiveDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建档案（从案件）
     */
    @Transactional
    public ArchiveDTO createArchive(CreateArchiveCommand command) {
        // 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
        
        if (!"CLOSED".equals(matter.getStatus())) {
            throw new BusinessException("只有已结案的案件才能创建档案");
        }

        // 检查是否已存在档案
        if (archiveRepository.count(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getMatterId, command.getMatterId())) > 0) {
            throw new BusinessException("该案件已存在档案记录");
        }

        // 生成档案号
        String archiveNo = generateArchiveNo(command.getArchiveType());

        // 确定保管期限（默认10年）
        String retentionPeriod = command.getRetentionPeriod() != null 
                ? command.getRetentionPeriod() : "10_YEARS";

        // 创建档案
        Archive archive = Archive.builder()
                .archiveNo(archiveNo)
                .matterId(command.getMatterId())
                .archiveName(command.getArchiveName() != null ? command.getArchiveName() : matter.getName() + " - 档案")
                .archiveType(command.getArchiveType() != null ? command.getArchiveType() : matter.getMatterType())
                .matterNo(matter.getMatterNo())
                .matterName(matter.getName())
                .caseCloseDate(matter.getActualClosingDate())
                .volumeCount(command.getVolumeCount() != null ? command.getVolumeCount() : 1)
                .pageCount(command.getPageCount())
                .catalog(command.getCatalog())
                .retentionPeriod(retentionPeriod)
                .retentionExpireDate(calculateRetentionExpireDate(matter.getActualClosingDate(), retentionPeriod))
                .hasElectronic(command.getHasElectronic() != null ? command.getHasElectronic() : false)
                .electronicUrl(command.getElectronicUrl())
                .status("PENDING")
                .remarks(command.getRemarks())
                .build();

        archiveRepository.save(archive);

        // 记录操作日志
        logOperation(archive.getId(), "CREATE", "创建档案", SecurityUtils.getUserId());

        log.info("档案创建成功: {} ({})", archive.getArchiveName(), archive.getArchiveNo());
        return toDTO(archive);
    }

    /**
     * 档案入库
     */
    @Transactional
    public void storeArchive(StoreArchiveCommand command) {
        Archive archive = archiveRepository.getByIdOrThrow(command.getArchiveId(), "档案不存在");
        
        if (!"PENDING".equals(archive.getStatus())) {
            throw new BusinessException("只有待入库的档案才能入库");
        }

        // 验证库位
        ArchiveLocation location = locationRepository.getByIdOrThrow(command.getLocationId(), "库位不存在");
        
        if (!"AVAILABLE".equals(location.getStatus())) {
            throw new BusinessException("库位不可用");
        }

        if (location.getUsedCapacity() >= location.getTotalCapacity()) {
            throw new BusinessException("库位已满，请选择其他库位");
        }

        // 入库
        archive.setLocationId(command.getLocationId());
        archive.setBoxNo(command.getBoxNo());
        archive.setStatus("STORED");
        archive.setStoredBy(SecurityUtils.getUserId());
        archive.setStoredAt(LocalDateTime.now());
        archiveRepository.updateById(archive);

        // 更新库位容量
        location.setUsedCapacity(location.getUsedCapacity() + 1);
        locationRepository.updateById(location);

        // 记录操作日志
        logOperation(archive.getId(), "STORE", "档案入库，库位：" + location.getLocationCode(), SecurityUtils.getUserId());

        log.info("档案入库成功: {}, 库位: {}", archive.getArchiveNo(), location.getLocationCode());
    }

    /**
     * 获取档案详情
     */
    public ArchiveDTO getArchiveById(Long id) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        return toDTO(archive);
    }

    /**
     * 获取待归档案件列表
     */
    public List<Object> getPendingMatters() {
        return archiveMapper.selectPendingArchives();
    }

    /**
     * 申请销毁档案
     */
    @Transactional
    public void applyDestroy(Long id, String reason) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"STORED".equals(archive.getStatus())) {
            throw new BusinessException("只有已入库的档案才能申请销毁");
        }

        // 检查是否到期
        if (archive.getRetentionExpireDate() != null 
                && archive.getRetentionExpireDate().isAfter(LocalDate.now())) {
            throw new BusinessException("档案保管期限未到，不能销毁");
        }

        archive.setStatus("PENDING_DESTROY");
        archive.setDestroyReason(reason);
        archiveRepository.updateById(archive);

        logOperation(archive.getId(), "APPLY_DESTROY", "申请销毁档案：" + reason, SecurityUtils.getUserId());
        log.info("档案销毁申请已提交: {}", archive.getArchiveNo());
    }

    /**
     * 审批销毁档案
     */
    @Transactional
    public void approveDestroy(Long id, boolean approved, String comment) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"PENDING_DESTROY".equals(archive.getStatus())) {
            throw new BusinessException("档案不在待销毁审批状态");
        }

        if (approved) {
            archive.setStatus("DESTROYED");
            archive.setDestroyDate(LocalDate.now());
            archive.setDestroyApproverId(SecurityUtils.getUserId());
            
            // 释放库位
            if (archive.getLocationId() != null) {
                ArchiveLocation location = locationRepository.findById(archive.getLocationId());
                if (location != null && location.getUsedCapacity() > 0) {
                    location.setUsedCapacity(location.getUsedCapacity() - 1);
                    locationRepository.updateById(location);
                }
            }
            
            logOperation(archive.getId(), "DESTROY", "档案已销毁：" + comment, SecurityUtils.getUserId());
            log.info("档案已销毁: {}", archive.getArchiveNo());
        } else {
            archive.setStatus("STORED");
            archive.setDestroyReason(null);
            logOperation(archive.getId(), "REJECT_DESTROY", "销毁申请被拒绝：" + comment, SecurityUtils.getUserId());
            log.info("档案销毁申请被拒绝: {}", archive.getArchiveNo());
        }
        
        archiveRepository.updateById(archive);
    }

    /**
     * 获取即将到期的档案（M7-041）
     */
    public List<ArchiveDTO> getExpiringArchives(int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        List<Archive> archives = archiveMapper.selectExpiringArchives(deadline);
        return archives.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按库位查看档案（M7-022）
     */
    public List<ArchiveDTO> getArchivesByLocation(Long locationId) {
        List<Archive> archives = archiveMapper.selectByLocationId(locationId);
        return archives.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 设置档案保管期限（M7-040）
     */
    @Transactional
    public ArchiveDTO setRetentionPeriod(Long id, String retentionPeriod) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"STORED".equals(archive.getStatus())) {
            throw new BusinessException("只有已入库的档案才能设置保管期限");
        }

        archive.setRetentionPeriod(retentionPeriod);
        archive.setRetentionExpireDate(calculateRetentionExpireDate(archive.getCaseCloseDate(), retentionPeriod));
        archiveRepository.updateById(archive);

        logOperation(archive.getId(), "SET_RETENTION", "设置保管期限：" + retentionPeriod, SecurityUtils.getUserId());
        log.info("档案保管期限设置成功: {}, 期限: {}", archive.getArchiveNo(), retentionPeriod);
        return toDTO(archive);
    }

    /**
     * 销毁登记（M7-044）
     */
    @Transactional
    public ArchiveDTO registerDestroy(Long id, String destroyMethod, String destroyLocation, String witness) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"DESTROYED".equals(archive.getStatus())) {
            throw new BusinessException("档案状态不正确，无法登记销毁信息");
        }

        // 更新销毁登记信息（可以扩展Archive实体添加这些字段，或使用remarks字段）
        String destroyInfo = String.format("销毁方式: %s, 销毁地点: %s, 见证人: %s", 
                destroyMethod, destroyLocation, witness);
        archive.setRemarks((archive.getRemarks() != null ? archive.getRemarks() + "\n" : "") + destroyInfo);
        archiveRepository.updateById(archive);

        logOperation(archive.getId(), "REGISTER_DESTROY", "销毁登记：" + destroyInfo, SecurityUtils.getUserId());
        log.info("档案销毁登记完成: {}", archive.getArchiveNo());
        return toDTO(archive);
    }

    /**
     * 生成档案号
     */
    private String generateArchiveNo(String archiveType) {
        String prefix = "DA"; // 档案
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + datePart + random;
    }

    /**
     * 计算保管到期日
     */
    private LocalDate calculateRetentionExpireDate(LocalDate caseCloseDate, String retentionPeriod) {
        if (caseCloseDate == null) {
            return null;
        }
        return switch (retentionPeriod) {
            case "PERMANENT" -> LocalDate.of(9999, 12, 31);
            case "30_YEARS" -> caseCloseDate.plusYears(30);
            case "15_YEARS" -> caseCloseDate.plusYears(15);
            case "10_YEARS" -> caseCloseDate.plusYears(10);
            case "5_YEARS" -> caseCloseDate.plusYears(5);
            default -> caseCloseDate.plusYears(10);
        };
    }

    /**
     * 记录操作日志
     */
    private void logOperation(Long archiveId, String operationType, String description, Long operatorId) {
        ArchiveOperationLog log = ArchiveOperationLog.builder()
                .archiveId(archiveId)
                .operationType(operationType)
                .operationDescription(description)
                .operatorId(operatorId)
                .operatedAt(LocalDateTime.now())
                .build();
        operationLogRepository.save(log);
    }

    /**
     * 获取档案类型名称
     */
    private String getArchiveTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "LITIGATION" -> "诉讼";
            case "NON_LITIGATION" -> "非诉";
            case "CONSULTATION" -> "咨询";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待入库";
            case "STORED" -> "已入库";
            case "BORROWED" -> "借出";
            case "DESTROYED" -> "已销毁";
            default -> status;
        };
    }

    /**
     * 获取保管期限名称
     */
    private String getRetentionPeriodName(String period) {
        if (period == null) return null;
        return switch (period) {
            case "PERMANENT" -> "永久";
            case "30_YEARS" -> "30年";
            case "15_YEARS" -> "15年";
            case "10_YEARS" -> "10年";
            case "5_YEARS" -> "5年";
            default -> period;
        };
    }

    /**
     * Entity 转 DTO
     */
    private ArchiveDTO toDTO(Archive archive) {
        ArchiveDTO dto = new ArchiveDTO();
        dto.setId(archive.getId());
        dto.setArchiveNo(archive.getArchiveNo());
        dto.setMatterId(archive.getMatterId());
        dto.setMatterNo(archive.getMatterNo());
        dto.setMatterName(archive.getMatterName());
        dto.setArchiveName(archive.getArchiveName());
        dto.setArchiveType(archive.getArchiveType());
        dto.setArchiveTypeName(getArchiveTypeName(archive.getArchiveType()));
        dto.setClientName(archive.getClientName());
        dto.setMainLawyerName(archive.getMainLawyerName());
        dto.setCaseCloseDate(archive.getCaseCloseDate());
        dto.setVolumeCount(archive.getVolumeCount());
        dto.setPageCount(archive.getPageCount());
        dto.setCatalog(archive.getCatalog());
        dto.setLocationId(archive.getLocationId());
        dto.setBoxNo(archive.getBoxNo());
        dto.setRetentionPeriod(archive.getRetentionPeriod());
        dto.setRetentionPeriodName(getRetentionPeriodName(archive.getRetentionPeriod()));
        dto.setRetentionExpireDate(archive.getRetentionExpireDate());
        dto.setHasElectronic(archive.getHasElectronic());
        dto.setElectronicUrl(archive.getElectronicUrl());
        dto.setStatus(archive.getStatus());
        dto.setStatusName(getStatusName(archive.getStatus()));
        dto.setStoredBy(archive.getStoredBy());
        dto.setStoredAt(archive.getStoredAt());
        dto.setDestroyDate(archive.getDestroyDate());
        dto.setDestroyReason(archive.getDestroyReason());
        dto.setDestroyApproverId(archive.getDestroyApproverId());
        dto.setRemarks(archive.getRemarks());
        dto.setCreatedAt(archive.getCreatedAt());
        dto.setUpdatedAt(archive.getUpdatedAt());
        return dto;
    }
}

