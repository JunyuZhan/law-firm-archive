package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.config.MetricsConfig;
import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.dto.archive.*;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.Category;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.Fonds;
import com.archivesystem.mq.ArchiveReceiveMessage;
import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.repository.*;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 档案服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final FondsMapper fondsMapper;
    private final CategoryMapper categoryMapper;
    private final RetentionPeriodMapper retentionPeriodMapper;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MetricsConfig metricsConfig;
    private final ConfigService configService;

    // 档案号计数器（仅作为Redis不可用时的备用）
    private static final AtomicInteger archiveNoCounter = new AtomicInteger(1);
    
    // Redis档案号序列键前缀
    private static final String ARCHIVE_NO_SEQ_PREFIX = "archive:seq:";

    @Override
    @Transactional
    public ArchiveReceiveResponse receive(ArchiveReceiveRequest request) {
        log.info("接收档案: sourceType={}, sourceId={}, async={}", 
                request.getSourceType(), request.getSourceId(), request.getAsync());

        // 检查是否已存在（幂等）
        Archive existingArchive = archiveMapper.selectBySourceId(request.getSourceType(), request.getSourceId());
        if (existingArchive != null) {
            log.info("档案已存在，返回已有档案号: {}", existingArchive.getArchiveNo());
            return ArchiveReceiveResponse.builder()
                    .archiveId(existingArchive.getId())
                    .archiveNo(existingArchive.getArchiveNo())
                    .status(existingArchive.getStatus())
                    .receivedAt(existingArchive.getReceivedAt())
                    .fileCount(existingArchive.getFileCount())
                    .message("档案已存在")
                    .build();
        }

        // 生成档案号
        String archiveNo = generateArchiveNo(request.getArchiveType());

        // 获取默认全宗
        Fonds defaultFonds = fondsMapper.selectByFondsNo("QZ001");

        // 确定初始状态：异步处理时为PENDING，同步处理时直接RECEIVED
        boolean isAsync = request.getAsync() == null || request.getAsync();
        String initialStatus = isAsync && hasFiles(request) ? Archive.STATUS_PROCESSING : Archive.STATUS_RECEIVED;

        // 创建档案记录
        Archive archive = Archive.builder()
                .archiveNo(archiveNo)
                .fondsId(defaultFonds != null ? defaultFonds.getId() : null)
                .fondsNo(defaultFonds != null ? defaultFonds.getFondsNo() : null)
                .archiveType(request.getArchiveType())
                .title(request.getTitle())
                .responsibility(request.getResponsibility())
                .documentDate(request.getDocumentDate())
                .retentionPeriod(request.getRetentionPeriod())
                .securityLevel(request.getSecurityLevel() != null ? request.getSecurityLevel() : Archive.SECURITY_INTERNAL)
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .sourceNo(request.getSourceNo())
                .callbackUrl(request.getCallbackUrl())
                .caseNo(request.getCaseNo())
                .caseName(request.getCaseName())
                .clientName(request.getClientName())
                .lawyerName(request.getLawyerName())
                .caseCloseDate(request.getCaseCloseDate())
                .keywords(request.getKeywords())
                .archiveAbstract(request.getArchiveAbstract())
                .remarks(request.getRemarks())
                .status(initialStatus)
                .receivedAt(LocalDateTime.now())
                .hasElectronic(hasFiles(request))
                .build();

        // 计算保管到期日期
        if (StringUtils.hasText(request.getRetentionPeriod())) {
            var retentionPeriod = retentionPeriodMapper.selectByPeriodCode(request.getRetentionPeriod());
            if (retentionPeriod != null && retentionPeriod.getPeriodYears() != null) {
                archive.setRetentionExpireDate(LocalDate.now().plusYears(retentionPeriod.getPeriodYears()));
            }
        }

        archiveMapper.insert(archive);
        log.info("档案创建成功: id={}, archiveNo={}", archive.getId(), archiveNo);

        // 处理文件
        int fileCount = 0;
        long totalSize = 0;
        
        if (hasFiles(request)) {
            if (isAsync) {
                // 异步处理：发送消息到RabbitMQ
                ArchiveReceiveMessage message = ArchiveReceiveMessage.fromRequest(
                        archive.getId(), archiveNo, request, request.getCallbackUrl());
                
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ARCHIVE_EXCHANGE,
                        RabbitMQConfig.ARCHIVE_RECEIVE_ROUTING_KEY,
                        message
                );
                
                log.info("已发送异步处理消息: archiveId={}, messageId={}", archive.getId(), message.getMessageId());
                
                return ArchiveReceiveResponse.builder()
                        .archiveId(archive.getId())
                        .archiveNo(archiveNo)
                        .status(Archive.STATUS_PROCESSING)
                        .receivedAt(archive.getReceivedAt())
                        .fileCount(request.getFiles().size())
                        .message("接收成功，文件正在异步处理中")
                        .build();
            } else {
                // 同步处理（向后兼容）
                for (int i = 0; i < request.getFiles().size(); i++) {
                    ArchiveReceiveRequest.FileInfo fileInfo = request.getFiles().get(i);
                    try {
                        DigitalFile digitalFile = fileStorageService.downloadAndStore(
                                archive.getId(),
                                fileInfo.getDownloadUrl(),
                                fileInfo.getFileName(),
                                fileInfo.getFileCategory(),
                                i + 1
                        );
                        if (digitalFile != null) {
                            fileCount++;
                            totalSize += digitalFile.getFileSize() != null ? digitalFile.getFileSize() : 0;
                        }
                    } catch (Exception e) {
                        log.error("文件下载失败: {}", fileInfo.getFileName(), e);
                    }
                }

                // 更新档案的文件统计
                archive.setFileCount(fileCount);
                archive.setTotalFileSize(totalSize);
                archive.setStatus(Archive.STATUS_RECEIVED);
                archiveMapper.updateById(archive);

                // 同步路径：文件转移完成后发送回调（与异步路径一致，便于律所系统开始90天清理倒计时）
                if (request.getCallbackUrl() != null && !request.getCallbackUrl().isEmpty()) {
                    int totalCount = request.getFiles().size();
                    int failedCount = totalCount - fileCount;
                    String status = failedCount == 0 ? CallbackMessage.STATUS_SUCCESS
                            : (fileCount > 0 ? CallbackMessage.STATUS_PARTIAL : CallbackMessage.STATUS_FAILED);
                    CallbackMessage callback = CallbackMessage.builder()
                            .messageId(java.util.UUID.randomUUID().toString())
                            .archiveId(archive.getId())
                            .archiveNo(archiveNo)
                            .sourceType(request.getSourceType())
                            .sourceId(request.getSourceId())
                            .callbackUrl(request.getCallbackUrl())
                            .status(status)
                            .successCount(fileCount)
                            .failedCount(failedCount)
                            .totalCount(totalCount)
                            .completedAt(LocalDateTime.now())
                            .retryCount(0)
                            .maxRetries(3)
                            .build();
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ARCHIVE_EXCHANGE,
                            RabbitMQConfig.ARCHIVE_CALLBACK_ROUTING_KEY,
                            callback
                    );
                    log.info("同步接收已发送文件转移完成回调: archiveId={}, status={}", archive.getId(), status);
                }
            }
        }

        return ArchiveReceiveResponse.builder()
                .archiveId(archive.getId())
                .archiveNo(archiveNo)
                .status(Archive.STATUS_RECEIVED)
                .receivedAt(archive.getReceivedAt())
                .fileCount(fileCount)
                .message("接收成功")
                .build();
    }
    
    private boolean hasFiles(ArchiveReceiveRequest request) {
        return request.getFiles() != null && !request.getFiles().isEmpty();
    }

    @Override
    @Transactional
    public ArchiveDTO create(ArchiveCreateRequest request) {
        String archiveNo = generateArchiveNo(request.getArchiveType());

        Archive archive = Archive.builder()
                .archiveNo(archiveNo)
                .fondsId(request.getFondsId())
                .categoryId(request.getCategoryId())
                .archiveType(request.getArchiveType())
                .title(request.getTitle())
                .fileNo(request.getFileNo())
                .responsibility(request.getResponsibility())
                .archiveDate(request.getArchiveDate() != null ? request.getArchiveDate() : LocalDate.now())
                .documentDate(request.getDocumentDate())
                .pageCount(request.getPageCount())
                .piecesCount(request.getPiecesCount() != null ? request.getPiecesCount() : 1)
                .retentionPeriod(request.getRetentionPeriod())
                .securityLevel(request.getSecurityLevel() != null ? request.getSecurityLevel() : Archive.SECURITY_INTERNAL)
                .sourceType(Archive.SOURCE_MANUAL)
                .caseNo(request.getCaseNo())
                .caseName(request.getCaseName())
                .clientName(request.getClientName())
                .lawyerName(request.getLawyerName())
                .caseCloseDate(request.getCaseCloseDate())
                .keywords(request.getKeywords())
                .archiveAbstract(request.getArchiveAbstract())
                .remarks(request.getRemarks())
                .extraData(request.getExtraData())
                .status(Archive.STATUS_RECEIVED)
                .receivedAt(LocalDateTime.now())
                .receivedBy(SecurityUtils.getCurrentUserId())
                .build();

        // 填充全宗和分类信息
        fillFondsAndCategory(archive);

        // 计算保管到期日期
        calculateRetentionExpireDate(archive);

        archiveMapper.insert(archive);

        // 关联已上传的文件
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            associateFiles(archive.getId(), request.getFileIds());
        }

        return getById(archive.getId());
    }

    @Override
    @Transactional
    public ArchiveDTO update(Long id, ArchiveCreateRequest request) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw NotFoundException.of("档案", id);
        }

        archive.setFondsId(request.getFondsId());
        archive.setCategoryId(request.getCategoryId());
        archive.setArchiveType(request.getArchiveType());
        archive.setTitle(request.getTitle());
        archive.setFileNo(request.getFileNo());
        archive.setResponsibility(request.getResponsibility());
        archive.setArchiveDate(request.getArchiveDate());
        archive.setDocumentDate(request.getDocumentDate());
        archive.setPageCount(request.getPageCount());
        archive.setPiecesCount(request.getPiecesCount());
        archive.setRetentionPeriod(request.getRetentionPeriod());
        archive.setSecurityLevel(request.getSecurityLevel());
        archive.setCaseNo(request.getCaseNo());
        archive.setCaseName(request.getCaseName());
        archive.setClientName(request.getClientName());
        archive.setLawyerName(request.getLawyerName());
        archive.setCaseCloseDate(request.getCaseCloseDate());
        archive.setKeywords(request.getKeywords());
        archive.setArchiveAbstract(request.getArchiveAbstract());
        archive.setRemarks(request.getRemarks());
        archive.setExtraData(request.getExtraData());

        fillFondsAndCategory(archive);
        calculateRetentionExpireDate(archive);

        archiveMapper.updateById(archive);

        return getById(id);
    }

    @Override
    public ArchiveDTO getById(Long id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw NotFoundException.of("档案", id);
        }
        return convertToDTO(archive, true);
    }

    @Override
    public ArchiveDTO getByArchiveNo(String archiveNo) {
        Archive archive = archiveMapper.selectByArchiveNo(archiveNo);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveNo);
        }
        return convertToDTO(archive, true);
    }

    @Override
    public PageResult<ArchiveDTO> query(ArchiveQueryRequest request) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getDeleted, false);

        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(Archive::getTitle, request.getKeyword())
                    .or().like(Archive::getArchiveNo, request.getKeyword())
                    .or().like(Archive::getCaseNo, request.getKeyword())
                    .or().like(Archive::getCaseName, request.getKeyword())
            );
        }

        // 精确条件
        if (StringUtils.hasText(request.getArchiveNo())) {
            wrapper.eq(Archive::getArchiveNo, request.getArchiveNo());
        }
        if (request.getFondsId() != null) {
            wrapper.eq(Archive::getFondsId, request.getFondsId());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Archive::getCategoryId, request.getCategoryId());
        }
        if (StringUtils.hasText(request.getArchiveType())) {
            wrapper.eq(Archive::getArchiveType, request.getArchiveType());
        }
        if (StringUtils.hasText(request.getRetentionPeriod())) {
            wrapper.eq(Archive::getRetentionPeriod, request.getRetentionPeriod());
        }
        if (StringUtils.hasText(request.getSecurityLevel())) {
            wrapper.eq(Archive::getSecurityLevel, request.getSecurityLevel());
        }
        if (StringUtils.hasText(request.getSourceType())) {
            wrapper.eq(Archive::getSourceType, request.getSourceType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Archive::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getCaseNo())) {
            wrapper.eq(Archive::getCaseNo, request.getCaseNo());
        }

        // 日期范围
        if (request.getArchiveDateStart() != null) {
            wrapper.ge(Archive::getArchiveDate, request.getArchiveDateStart());
        }
        if (request.getArchiveDateEnd() != null) {
            wrapper.le(Archive::getArchiveDate, request.getArchiveDateEnd());
        }
        if (request.getCreatedAtStart() != null) {
            wrapper.ge(Archive::getCreatedAt, request.getCreatedAtStart().atStartOfDay());
        }
        if (request.getCreatedAtEnd() != null) {
            wrapper.le(Archive::getCreatedAt, request.getCreatedAtEnd().plusDays(1).atStartOfDay());
        }

        // 排序
        if ("asc".equalsIgnoreCase(request.getSortOrder())) {
            wrapper.orderByAsc(Archive::getCreatedAt);
        } else {
            wrapper.orderByDesc(Archive::getCreatedAt);
        }

        // 分页查询
        Page<Archive> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<Archive> result = archiveMapper.selectPage(page, wrapper);

        List<ArchiveDTO> records = result.getRecords().stream()
                .map(a -> convertToDTO(a, false))
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw NotFoundException.of("档案", id);
        }
        archiveMapper.deleteById(id);
        log.info("档案删除: id={}, archiveNo={}", id, archive.getArchiveNo());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw NotFoundException.of("档案", id);
        }
        archive.setStatus(status);
        archiveMapper.updateById(archive);
    }

    @Override
    public String generateArchiveNo(String archiveType) {
        // 从配置服务获取前缀
        String prefix = configService.getArchiveNoPrefix(archiveType);
        
        // 从配置获取日期格式和序号位数
        String dateFormat = configService.getValue("archive.no.date.format", "yyyyMMdd");
        int seqDigits = configService.getIntValue("archive.no.seq.digits", 4);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        
        // 使用Redis生成序号（保证高并发下的唯一性）
        long seq;
        try {
            String seqKey = ARCHIVE_NO_SEQ_PREFIX + prefix + ":" + date;
            Long increment = stringRedisTemplate.opsForValue().increment(seqKey);
            seq = increment != null ? increment : 1;
            
            // 设置过期时间（次日凌晨过期）
            if (seq == 1) {
                stringRedisTemplate.expireAt(seqKey, 
                        java.util.Date.from(LocalDate.now().plusDays(1)
                                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            }
        } catch (Exception e) {
            // Redis不可用时，降级使用内存计数器
            log.warn("Redis不可用，降级使用内存计数器: {}", e.getMessage());
            seq = archiveNoCounter.getAndIncrement();
        }
        
        // 动态格式化序号位数
        String format = "%s-%s-%0" + seqDigits + "d";
        return String.format(format, prefix, date, seq);
    }

    private void fillFondsAndCategory(Archive archive) {
        if (archive.getFondsId() != null) {
            Fonds fonds = fondsMapper.selectById(archive.getFondsId());
            if (fonds != null) {
                archive.setFondsNo(fonds.getFondsNo());
            }
        }
        if (archive.getCategoryId() != null) {
            Category category = categoryMapper.selectById(archive.getCategoryId());
            if (category != null) {
                archive.setCategoryCode(category.getCategoryCode());
            }
        }
    }

    private void calculateRetentionExpireDate(Archive archive) {
        if (StringUtils.hasText(archive.getRetentionPeriod())) {
            var retentionPeriod = retentionPeriodMapper.selectByPeriodCode(archive.getRetentionPeriod());
            if (retentionPeriod != null && retentionPeriod.getPeriodYears() != null) {
                LocalDate baseDate = archive.getArchiveDate() != null ? archive.getArchiveDate() : LocalDate.now();
                archive.setRetentionExpireDate(baseDate.plusYears(retentionPeriod.getPeriodYears()));
            }
        }
    }

    private void associateFiles(Long archiveId, List<Long> fileIds) {
        long totalSize = 0;
        int sortOrder = 1;
        for (Long fileId : fileIds) {
            DigitalFile file = digitalFileMapper.selectById(fileId);
            if (file != null && file.getArchiveId() == null) {
                file.setArchiveId(archiveId);
                file.setSortOrder(sortOrder++);
                digitalFileMapper.updateById(file);
                totalSize += file.getFileSize() != null ? file.getFileSize() : 0;
            }
        }

        Archive archive = archiveMapper.selectById(archiveId);
        archive.setFileCount(fileIds.size());
        archive.setTotalFileSize(totalSize);
        archive.setHasElectronic(true);
        archiveMapper.updateById(archive);
    }

    private ArchiveDTO convertToDTO(Archive archive, boolean includeFiles) {
        ArchiveDTO dto = ArchiveDTO.builder()
                .id(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .fondsId(archive.getFondsId())
                .fondsNo(archive.getFondsNo())
                .categoryId(archive.getCategoryId())
                .categoryCode(archive.getCategoryCode())
                .archiveType(archive.getArchiveType())
                .title(archive.getTitle())
                .fileNo(archive.getFileNo())
                .responsibility(archive.getResponsibility())
                .archiveDate(archive.getArchiveDate())
                .documentDate(archive.getDocumentDate())
                .pageCount(archive.getPageCount())
                .piecesCount(archive.getPiecesCount())
                .retentionPeriod(archive.getRetentionPeriod())
                .retentionExpireDate(archive.getRetentionExpireDate())
                .securityLevel(archive.getSecurityLevel())
                .securityExpireDate(archive.getSecurityExpireDate())
                .sourceType(archive.getSourceType())
                .sourceSystem(archive.getSourceSystem())
                .sourceId(archive.getSourceId())
                .sourceNo(archive.getSourceNo())
                .caseNo(archive.getCaseNo())
                .caseName(archive.getCaseName())
                .clientName(archive.getClientName())
                .lawyerName(archive.getLawyerName())
                .caseCloseDate(archive.getCaseCloseDate())
                .hasElectronic(archive.getHasElectronic())
                .storageLocation(archive.getStorageLocation())
                .totalFileSize(archive.getTotalFileSize())
                .fileCount(archive.getFileCount())
                .status(archive.getStatus())
                .receivedAt(archive.getReceivedAt())
                .catalogedAt(archive.getCatalogedAt())
                .archivedAt(archive.getArchivedAt())
                .keywords(archive.getKeywords())
                .archiveAbstract(archive.getArchiveAbstract())
                .remarks(archive.getRemarks())
                .extraData(archive.getExtraData())
                .createdAt(archive.getCreatedAt())
                .updatedAt(archive.getUpdatedAt())
                .build();

        // 加载文件列表
        if (includeFiles && Boolean.TRUE.equals(archive.getHasElectronic())) {
            List<DigitalFile> files = digitalFileMapper.selectByArchiveId(archive.getId());
            dto.setFiles(files.stream().map(this::convertFileToDTO).collect(Collectors.toList()));
        }

        return dto;
    }

    private DigitalFileDTO convertFileToDTO(DigitalFile file) {
        return DigitalFileDTO.builder()
                .id(file.getId())
                .archiveId(file.getArchiveId())
                .fileNo(file.getFileNo())
                .fileName(file.getFileName())
                .originalName(file.getOriginalName())
                .fileExtension(file.getFileExtension())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .fileSizeFormatted(formatFileSize(file.getFileSize()))
                .formatName(file.getFormatName())
                .isLongTermFormat(file.getIsLongTermFormat())
                .hashAlgorithm(file.getHashAlgorithm())
                .hashValue(file.getHashValue())
                .hasPreview(file.getHasPreview())
                .fileCategory(file.getFileCategory())
                .sortOrder(file.getSortOrder())
                .description(file.getDescription())
                .ocrStatus(file.getOcrStatus())
                .uploadAt(file.getUploadAt())
                .createdAt(file.getCreatedAt())
                .build();
    }

    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size;
        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }
}
