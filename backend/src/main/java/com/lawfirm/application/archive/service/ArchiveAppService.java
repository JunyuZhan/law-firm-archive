package com.lawfirm.application.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.dto.ArchiveQueryDTO;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveCheckResult;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveDataSnapshot;
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
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Collections;
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
    private final ArchiveDataCollectorService dataCollectorService;
    private final com.lawfirm.infrastructure.external.document.DossierCoverGenerator coverGenerator;
    private final com.lawfirm.infrastructure.external.minio.MinioService minioService;
    private final ApprovalService approvalService;
    private final ApproverService approverService;
    private com.lawfirm.application.matter.service.MatterAppService matterAppService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    public void setMatterAppService(com.lawfirm.application.matter.service.MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 分页查询档案
     */
    public PageResult<ArchiveDTO> listArchives(ArchiveQueryDTO query) {
        // 根据用户权限过滤数据
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        
        // 获取可访问的项目ID列表
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // 如果返回空列表，表示没有权限，返回空结果
        if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
        }
        
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
        
        // 应用数据权限过滤：只查询可访问项目的档案
        if (accessibleMatterIds != null) {
            wrapper.in(Archive::getMatterId, accessibleMatterIds);
        }
        // accessibleMatterIds == null 表示可以访问所有项目的档案（ALL权限）
        
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
     * 自动收集项目所有相关数据
     */
    @Transactional
    public ArchiveDTO createArchive(CreateArchiveCommand command) {
        // 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
        
        // 允许从 CLOSED 或 ARCHIVED 状态的项目创建档案
        if (!"CLOSED".equals(matter.getStatus()) && !"ARCHIVED".equals(matter.getStatus())) {
            throw new BusinessException("只有已结案或已归档的案件才能创建档案");
        }

        // 检查是否已存在档案
        if (archiveRepository.count(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getMatterId, command.getMatterId())) > 0) {
            throw new BusinessException("该案件已存在档案记录");
        }

        // 收集项目所有相关数据
        ArchiveDataSnapshot snapshot = dataCollectorService.collectMatterData(command.getMatterId());

        // 转JSON并验证数据完整性
        String snapshotJson;
        try {
            snapshotJson = dataCollectorService.snapshotToJson(snapshot);

            // 验证能否反序列化
            ArchiveDataSnapshot verified = dataCollectorService.jsonToSnapshot(snapshotJson);

            // 验证关键数据完整性
            if (verified.getMatterId() == null || !snapshot.getMatterId().equals(verified.getMatterId())) {
                throw new BusinessException("数据快照验证失败: 项目ID不一致");
            }

            if (snapshot.getStatistics() == null || snapshot.getStatistics().isEmpty()) {
                log.warn("归档快照统计信息为空: matterId={}", command.getMatterId());
            }

            // 验证快照大小
            if (snapshotJson.length() > 10 * 1024 * 1024) { // 10MB
                log.warn("归档快照过大: {}KB, matterId={}",
                        snapshotJson.length() / 1024, command.getMatterId());
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("归档数据快照生成失败: matterId={}", command.getMatterId(), e);
            throw new BusinessException("归档数据快照生成失败，请检查项目数据完整性: " + e.getMessage());
        }

        // 生成档案号
        String archiveNo = generateArchiveNo(command.getArchiveType());

        // 确定保管期限（默认10年）
        String retentionPeriod = command.getRetentionPeriod() != null 
                ? command.getRetentionPeriod() : "10_YEARS";

        // 从快照中获取统计信息生成目录
        String catalog = command.getCatalog();
        if (catalog == null || catalog.isEmpty()) {
            catalog = generateCatalogFromSnapshot(snapshot);
        }

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
                .catalog(catalog)
                .retentionPeriod(retentionPeriod)
                .retentionExpireDate(calculateRetentionExpireDate(matter.getActualClosingDate(), retentionPeriod))
                .hasElectronic(command.getHasElectronic() != null ? command.getHasElectronic() : true)
                .electronicUrl(command.getElectronicUrl())
                .status("PENDING")
                .remarks(command.getRemarks())
                .archiveSnapshot(snapshotJson)
                .filesDeleted(false)
                .build();

        archiveRepository.save(archive);

        // 生成卷宗封面
        try {
            byte[] coverPdf = coverGenerator.generateCover(matter, archiveNo);
            String coverFileName = "archive_" + archive.getId() + "_cover.pdf";
            String coverPath = "archives/" + archive.getId() + "/" + coverFileName;
            String coverUrl = minioService.uploadBytes(coverPdf, coverPath, "application/pdf");
            
            // 将封面URL保存到electronicUrl字段（如果没有电子档案）
            if (archive.getElectronicUrl() == null || archive.getElectronicUrl().isEmpty()) {
                archive.setElectronicUrl(coverUrl);
            }
            archiveRepository.updateById(archive);
            
            log.info("卷宗封面生成成功: archiveId={}, coverUrl={}", archive.getId(), coverUrl);
        } catch (Exception e) {
            log.error("生成卷宗封面失败: archiveId={}", archive.getId(), e);
            // 封面生成失败不影响归档流程，只记录日志
        }

        // 记录操作日志
        logOperation(archive.getId(), "CREATE", "创建档案，数据统计：" + snapshot.getStatistics(), SecurityUtils.getUserId());

        log.info("档案创建成功: {} ({}), 数据统计: {}", archive.getArchiveName(), archive.getArchiveNo(), snapshot.getStatistics());
        return toDTO(archive);
    }

    /**
     * 从数据快照生成档案目录
     */
    private String generateCatalogFromSnapshot(ArchiveDataSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("档案目录\n");
        sb.append("========\n\n");
        
        // 根据卷宗目录生成
        if (snapshot.getDossierItems() != null && !snapshot.getDossierItems().isEmpty()) {
            for (Map<String, Object> item : snapshot.getDossierItems()) {
                String name = (String) item.get("name");
                Integer docCount = (Integer) item.get("document_count");
                sb.append(String.format("- %s (%d 个文件)\n", name, docCount != null ? docCount : 0));
            }
        }
        
        sb.append("\n数据统计\n");
        sb.append("========\n");
        Map<String, Integer> stats = snapshot.getStatistics();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            sb.append(String.format("- %s: %d\n", entry.getKey(), entry.getValue()));
        }
        
        return sb.toString();
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
     * 根据项目ID查询档案（如果存在）
     */
    public ArchiveDTO getArchiveByMatterId(Long matterId) {
        Archive archive = archiveRepository.lambdaQuery()
                .eq(Archive::getMatterId, matterId)
                .last("LIMIT 1")
                .one();
        return archive != null ? toDTO(archive) : null;
    }

    /**
     * 从项目创建档案（用于项目归档时自动创建）
     */
    @Transactional
    public ArchiveDTO createArchiveFromMatter(CreateArchiveCommand command) {
        // 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
        
        // 允许从 CLOSED 或 ARCHIVED 状态的项目创建档案
        if (!"CLOSED".equals(matter.getStatus()) && !"ARCHIVED".equals(matter.getStatus())) {
            throw new BusinessException("只有已结案或已归档的案件才能创建档案");
        }

        // 检查是否已存在档案
        if (archiveRepository.count(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getMatterId, command.getMatterId())) > 0) {
            throw new BusinessException("该案件已存在档案记录");
        }

        // 生成档案号
        String archiveNo = generateArchiveNo(command.getArchiveType() != null ? command.getArchiveType() : matter.getMatterType());

        // 确定保管期限（默认10年）
        String retentionPeriod = command.getRetentionPeriod() != null 
                ? command.getRetentionPeriod() : "10_YEARS";

        // 确定结案日期
        LocalDate caseCloseDate = matter.getActualClosingDate() != null 
                ? matter.getActualClosingDate() : LocalDate.now();

        // 创建档案
        Archive archive = Archive.builder()
                .archiveNo(archiveNo)
                .matterId(command.getMatterId())
                .archiveName(command.getArchiveName() != null ? command.getArchiveName() : matter.getName() + " - 档案")
                .archiveType(command.getArchiveType() != null ? command.getArchiveType() : matter.getMatterType())
                .matterNo(matter.getMatterNo())
                .matterName(matter.getName())
                .caseCloseDate(caseCloseDate)
                .volumeCount(command.getVolumeCount() != null ? command.getVolumeCount() : 1)
                .pageCount(command.getPageCount())
                .catalog(command.getCatalog())
                .retentionPeriod(retentionPeriod)
                .retentionExpireDate(calculateRetentionExpireDate(caseCloseDate, retentionPeriod))
                .hasElectronic(command.getHasElectronic() != null ? command.getHasElectronic() : false)
                .electronicUrl(command.getElectronicUrl())
                .status("PENDING")
                .remarks(command.getRemarks())
                .build();

        archiveRepository.save(archive);

        // 生成卷宗封面
        try {
            byte[] coverPdf = coverGenerator.generateCover(matter, archiveNo);
            String coverFileName = "archive_" + archive.getId() + "_cover.pdf";
            String coverPath = "archives/" + archive.getId() + "/" + coverFileName;
            String coverUrl = minioService.uploadBytes(coverPdf, coverPath, "application/pdf");
            
            // 将封面URL保存到electronicUrl字段（如果没有电子档案）
            if (archive.getElectronicUrl() == null || archive.getElectronicUrl().isEmpty()) {
                archive.setElectronicUrl(coverUrl);
            }
            archiveRepository.updateById(archive);
            
            log.info("卷宗封面生成成功: archiveId={}, coverUrl={}", archive.getId(), coverUrl);
        } catch (Exception e) {
            log.error("生成卷宗封面失败: archiveId={}", archive.getId(), e);
            // 封面生成失败不影响归档流程，只记录日志
        }

        // 记录操作日志
        logOperation(archive.getId(), "CREATE", "创建档案", SecurityUtils.getUserId());

        log.info("档案创建成功: {} ({})", archive.getArchiveName(), archive.getArchiveNo());
        return toDTO(archive);
    }

    /**
     * 归档预检查
     * 检查项目是否满足归档条件
     */
    public ArchiveCheckResult checkArchiveRequirements(Long matterId) {
        return dataCollectorService.checkArchiveRequirements(matterId);
    }

    /**
     * 获取项目归档数据预览
     * 用于前端展示可归档的数据
     */
    public ArchiveDataSnapshot previewArchiveData(Long matterId) {
        return dataCollectorService.collectMatterData(matterId);
    }

    /**
     * 获取可用的归档数据源配置
     */
    public List<Map<String, Object>> getAvailableDataSources() {
        return dataCollectorService.getAvailableDataSources();
    }

    /**
     * 提交入库审批
     */
    @Transactional
    public void submitStoreApproval(Long archiveId) {
        Archive archive = archiveRepository.getByIdOrThrow(archiveId, "档案不存在");
        
        if (!"PENDING".equals(archive.getStatus())) {
            throw new BusinessException("只有待入库的档案才能提交入库审批");
        }

        archive.setStatus("PENDING_STORE");
        archiveRepository.updateById(archive);

        // 创建审批记录，通知主任审批
        Long approverId = approverService.findArchiveStoreApprover();
        String businessTitle = String.format("档案入库审批：%s", archive.getArchiveNo());
        approvalService.createApproval(
                "ARCHIVE_STORE",
                archive.getId(),
                archive.getArchiveNo(),
                businessTitle,
                approverId
        );
        
        logOperation(archive.getId(), "SUBMIT_STORE", "提交入库审批", SecurityUtils.getUserId());
        log.info("档案入库审批已提交: {}", archive.getArchiveNo());
    }

    /**
     * 审批入库（手动调用）
     */
    @Transactional
    public void approveStore(Long archiveId, boolean approved, String comment) {
        if (approved) {
            onStoreApprovalApproved(archiveId, comment);
        } else {
            onStoreApprovalRejected(archiveId, comment);
        }
    }

    /**
     * 档案入库审批通过回调（由审批事件监听器调用）
     */
    @Transactional
    public void onStoreApprovalApproved(Long archiveId, String comment) {
        Archive archive = archiveRepository.getByIdOrThrow(archiveId, "档案不存在");
        
        if (!"PENDING_STORE".equals(archive.getStatus())) {
            log.warn("档案不在待入库审批状态，可能已从其他地方处理: archiveId={}, status={}", 
                    archiveId, archive.getStatus());
            return;
        }

        // 审批通过后，档案状态变为待入库（等待行政人员实际入库操作）
        archive.setStatus("PENDING");
        archiveRepository.updateById(archive);
        
        logOperation(archive.getId(), "APPROVE_STORE", "入库审批通过：" + (comment != null ? comment : ""), SecurityUtils.getUserId());
        log.info("档案入库审批通过: {}", archive.getArchiveNo());
    }

    /**
     * 档案入库审批拒绝回调（由审批事件监听器调用）
     */
    @Transactional
    public void onStoreApprovalRejected(Long archiveId, String comment) {
        Archive archive = archiveRepository.getByIdOrThrow(archiveId, "档案不存在");
        
        if (!"PENDING_STORE".equals(archive.getStatus())) {
            log.warn("档案不在待入库审批状态，可能已从其他地方处理: archiveId={}, status={}", 
                    archiveId, archive.getStatus());
            return;
        }

        // 审批拒绝，退回待入库状态
        archive.setStatus("PENDING");
        archiveRepository.updateById(archive);
        
        logOperation(archive.getId(), "REJECT_STORE", "入库审批拒绝：" + (comment != null ? comment : ""), SecurityUtils.getUserId());
        log.info("档案入库审批拒绝: {}", archive.getArchiveNo());
    }

    /**
     * 申请迁移档案（原销毁功能改为迁移）
     */
    @Transactional
    public void applyMigrate(Long id, String reason, String migrateTarget) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"STORED".equals(archive.getStatus())) {
            throw new BusinessException("只有已入库的档案才能申请迁移");
        }

        archive.setStatus("PENDING_MIGRATE");
        archive.setMigrateReason(reason);
        archive.setMigrateTarget(migrateTarget);
        archiveRepository.updateById(archive);

        logOperation(archive.getId(), "APPLY_MIGRATE", "申请迁移档案：" + reason + "，目标：" + migrateTarget, SecurityUtils.getUserId());
        log.info("档案迁移申请已提交: {}", archive.getArchiveNo());
    }

    /**
     * 审批迁移档案
     */
    @Transactional
    public void approveMigrate(Long id, boolean approved, String comment, boolean deleteFiles) {
        Archive archive = archiveRepository.getByIdOrThrow(id, "档案不存在");
        
        if (!"PENDING_MIGRATE".equals(archive.getStatus())) {
            throw new BusinessException("档案不在待迁移审批状态");
        }

        if (approved) {
            archive.setStatus("MIGRATED");
            archive.setMigrateDate(LocalDate.now());
            archive.setMigrateApproverId(SecurityUtils.getUserId());
            
            // 释放库位
            if (archive.getLocationId() != null) {
                ArchiveLocation location = locationRepository.findById(archive.getLocationId());
                if (location != null && location.getUsedCapacity() > 0) {
                    location.setUsedCapacity(location.getUsedCapacity() - 1);
                    locationRepository.updateById(location);
                }
            }
            
            // 如果选择删除文件
            if (deleteFiles) {
                archive.setFilesDeleted(true);
                // TODO: 实际删除 MinIO 中的文件
                logOperation(archive.getId(), "DELETE_FILES", "档案文件已删除", SecurityUtils.getUserId());
            }
            
            logOperation(archive.getId(), "MIGRATE", "档案已迁移：" + comment, SecurityUtils.getUserId());
            log.info("档案已迁移: {}, 目标: {}", archive.getArchiveNo(), archive.getMigrateTarget());
        } else {
            archive.setStatus("STORED");
            archive.setMigrateReason(null);
            archive.setMigrateTarget(null);
            logOperation(archive.getId(), "REJECT_MIGRATE", "迁移申请被拒绝：" + comment, SecurityUtils.getUserId());
            log.info("档案迁移申请被拒绝: {}", archive.getArchiveNo());
        }
        
        archiveRepository.updateById(archive);
    }

    /**
     * 申请销毁档案（保留原接口，内部调用迁移）
     * @deprecated 请使用 applyMigrate
     */
    @Transactional
    @Deprecated
    public void applyDestroy(Long id, String reason) {
        applyMigrate(id, reason, "档案销毁");
    }

    /**
     * 审批销毁档案（保留原接口，内部调用迁移审批）
     * @deprecated 请使用 approveMigrate
     */
    @Transactional
    @Deprecated
    public void approveDestroy(Long id, boolean approved, String comment) {
        approveMigrate(id, approved, comment, true);
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
            case "PENDING_STORE" -> "待入库审批";
            case "STORED" -> "已入库";
            case "BORROWED" -> "借出";
            case "PENDING_MIGRATE" -> "待迁移审批";
            case "MIGRATED" -> "已迁移";
            case "DESTROYED" -> "已销毁"; // 兼容旧数据
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
     * 重新生成卷宗封面
     */
    @Transactional
    public ArchiveDTO regenerateCover(Long archiveId) {
        Archive archive = archiveRepository.getByIdOrThrow(archiveId, "档案不存在");
        Matter matter = matterRepository.getByIdOrThrow(archive.getMatterId(), "项目不存在");
        
        try {
            // 生成新的封面
            byte[] coverPdf = coverGenerator.generateCover(matter, archive.getArchiveNo());
            String coverFileName = "archive_" + archive.getId() + "_cover.pdf";
            String coverPath = "archives/" + archive.getId() + "/" + coverFileName;
            String coverUrl = minioService.uploadBytes(coverPdf, coverPath, "application/pdf");
            
            // 更新封面URL
            archive.setElectronicUrl(coverUrl);
            archiveRepository.updateById(archive);
            
            log.info("卷宗封面重新生成成功: archiveId={}, coverUrl={}", archive.getId(), coverUrl);
        } catch (Exception e) {
            log.error("重新生成卷宗封面失败: archiveId={}", archiveId, e);
            throw new BusinessException("重新生成卷宗封面失败: " + e.getMessage());
        }
        
        return toDTO(archive);
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

