package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.ArchiveDTO;
import com.archivesystem.dto.ArchiveQueryDTO;
import com.archivesystem.dto.ArchiveReceiveDTO;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.ArchiveFile;
import com.archivesystem.repository.ArchiveFileMapper;
import com.archivesystem.repository.ArchiveMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 档案服务.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final MinioService minioService;

    /**
     * 接收外部系统的档案.
     * 这是核心接口，用于接收律所系统或其他系统推送的档案。
     */
    @Transactional
    public ArchiveDTO receiveArchive(ArchiveReceiveDTO dto) {
        log.info("接收档案: sourceType={}, sourceId={}, archiveName={}", 
                dto.getSourceType(), dto.getSourceId(), dto.getArchiveName());

        // 检查是否已存在（根据来源ID去重）
        if (StringUtils.hasText(dto.getSourceId())) {
            Archive existing = archiveMapper.selectBySourceIdAndType(dto.getSourceId(), dto.getSourceType());
            if (existing != null) {
                log.warn("档案已存在: sourceId={}, archiveNo={}", dto.getSourceId(), existing.getArchiveNo());
                throw new RuntimeException("档案已存在，编号：" + existing.getArchiveNo());
            }
        }

        // 生成档案编号
        String archiveNo = generateArchiveNo(dto.getSourceType());

        // 创建档案记录
        Archive archive = Archive.builder()
                .archiveNo(archiveNo)
                .archiveName(dto.getArchiveName())
                .archiveType(dto.getArchiveType() != null ? dto.getArchiveType() : Archive.TYPE_OTHER)
                .category(dto.getCategory() != null ? dto.getCategory() : Archive.CATEGORY_CASE)
                .description(dto.getDescription())
                .sourceType(dto.getSourceType())
                .sourceId(dto.getSourceId())
                .sourceNo(dto.getSourceNo())
                .sourceSnapshot(dto.getSourceSnapshot())
                .clientName(dto.getClientName())
                .responsiblePerson(dto.getResponsiblePerson())
                .caseCloseDate(dto.getCaseCloseDate())
                .volumeCount(dto.getVolumeCount() != null ? dto.getVolumeCount() : 1)
                .pageCount(dto.getPageCount())
                .catalog(dto.getCatalog())
                .retentionPeriod(dto.getRetentionPeriod() != null ? dto.getRetentionPeriod() : "10_YEARS")
                .retentionExpireDate(calculateRetentionExpireDate(dto.getCaseCloseDate(), dto.getRetentionPeriod()))
                .hasElectronic(dto.getFiles() != null && !dto.getFiles().isEmpty())
                .status(Archive.STATUS_RECEIVED)
                .receivedAt(LocalDateTime.now())
                .remarks(dto.getRemarks())
                .build();

        archiveMapper.insert(archive);

        // 处理档案文件
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (int i = 0; i < dto.getFiles().size(); i++) {
                ArchiveReceiveDTO.ArchiveFileDTO fileDto = dto.getFiles().get(i);
                processArchiveFile(archive.getId(), fileDto, i);
            }
        }

        log.info("档案接收成功: archiveNo={}, id={}", archiveNo, archive.getId());
        return toDTO(archive);
    }

    /**
     * 处理档案文件.
     */
    private void processArchiveFile(Long archiveId, ArchiveReceiveDTO.ArchiveFileDTO fileDto, int index) {
        try {
            String storagePath = null;
            
            // 如果有下载URL，下载并存储到MinIO
            if (StringUtils.hasText(fileDto.getDownloadUrl())) {
                storagePath = minioService.downloadAndStore(
                        fileDto.getDownloadUrl(), 
                        "archives/" + archiveId + "/" + fileDto.getFileName()
                );
            }

            ArchiveFile archiveFile = ArchiveFile.builder()
                    .archiveId(archiveId)
                    .fileName(fileDto.getFileName())
                    .originalFileName(fileDto.getFileName())
                    .fileType(fileDto.getFileType())
                    .fileSize(fileDto.getFileSize())
                    .storagePath(storagePath)
                    .category(fileDto.getCategory() != null ? fileDto.getCategory() : ArchiveFile.CATEGORY_DOCUMENT)
                    .sortOrder(fileDto.getSortOrder() != null ? fileDto.getSortOrder() : index)
                    .description(fileDto.getDescription())
                    .sourceUrl(fileDto.getDownloadUrl())
                    .build();

            archiveFileMapper.insert(archiveFile);
            log.debug("档案文件保存成功: archiveId={}, fileName={}", archiveId, fileDto.getFileName());

        } catch (Exception e) {
            log.error("处理档案文件失败: archiveId={}, fileName={}", archiveId, fileDto.getFileName(), e);
            // 文件处理失败不影响档案接收
        }
    }

    /**
     * 分页查询档案.
     */
    public PageResult<ArchiveDTO> listArchives(ArchiveQueryDTO query) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getArchiveNo())) {
            wrapper.like(Archive::getArchiveNo, query.getArchiveNo());
        }
        if (StringUtils.hasText(query.getArchiveName())) {
            wrapper.like(Archive::getArchiveName, query.getArchiveName());
        }
        if (StringUtils.hasText(query.getArchiveType())) {
            wrapper.eq(Archive::getArchiveType, query.getArchiveType());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Archive::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getSourceType())) {
            wrapper.eq(Archive::getSourceType, query.getSourceType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Archive::getStatus, query.getStatus());
        }
        if (query.getLocationId() != null) {
            wrapper.eq(Archive::getLocationId, query.getLocationId());
        }
        if (StringUtils.hasText(query.getClientName())) {
            wrapper.like(Archive::getClientName, query.getClientName());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(Archive::getArchiveNo, query.getKeyword())
                    .or().like(Archive::getArchiveName, query.getKeyword())
                    .or().like(Archive::getClientName, query.getKeyword())
                    .or().like(Archive::getSourceNo, query.getKeyword())
            );
        }
        
        wrapper.orderByDesc(Archive::getCreatedAt);

        IPage<Archive> page = archiveMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<ArchiveDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取档案详情.
     */
    public ArchiveDTO getArchiveById(Long id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null || archive.getDeleted()) {
            throw new RuntimeException("档案不存在");
        }
        
        ArchiveDTO dto = toDTO(archive);
        
        // 加载文件列表
        List<ArchiveFile> files = archiveFileMapper.selectByArchiveId(id);
        dto.setFiles(files.stream().map(this::toFileDTO).collect(Collectors.toList()));
        dto.setFileCount(files.size());
        
        return dto;
    }

    /**
     * 档案入库.
     */
    @Transactional
    public void storeArchive(Long id, Long locationId, String boxNo) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null || archive.getDeleted()) {
            throw new RuntimeException("档案不存在");
        }
        
        if (!Archive.STATUS_RECEIVED.equals(archive.getStatus()) 
                && !Archive.STATUS_PENDING.equals(archive.getStatus())) {
            throw new RuntimeException("当前状态不允许入库");
        }

        archive.setLocationId(locationId);
        archive.setBoxNo(boxNo);
        archive.setStatus(Archive.STATUS_STORED);
        archive.setStoredAt(LocalDateTime.now());
        // archive.setStoredBy(SecurityUtils.getUserId()); // TODO: 需要实现安全上下文
        
        archiveMapper.updateById(archive);
        log.info("档案入库成功: archiveNo={}, locationId={}", archive.getArchiveNo(), locationId);
    }

    /**
     * 获取统计数据.
     */
    public Map<String, Object> getStatistics() {
        List<Map<String, Object>> statusStats = archiveMapper.countByStatus();
        List<Map<String, Object>> sourceStats = archiveMapper.countBySourceType();
        
        return Map.of(
                "byStatus", statusStats,
                "bySourceType", sourceStats,
                "totalCount", archiveMapper.selectCount(
                        new LambdaQueryWrapper<Archive>().eq(Archive::getDeleted, false)
                )
        );
    }

    /**
     * 生成档案编号.
     */
    private String generateArchiveNo(String sourceType) {
        String prefix = "ARC";
        if (Archive.SOURCE_LAW_FIRM.equals(sourceType)) {
            prefix = "LF";
        } else if (Archive.SOURCE_IMPORT.equals(sourceType)) {
            prefix = "IMP";
        } else if (Archive.SOURCE_EXTERNAL.equals(sourceType)) {
            prefix = "EXT";
        }
        
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + datePart + random;
    }

    /**
     * 计算保管到期日期.
     */
    private LocalDate calculateRetentionExpireDate(LocalDate baseDate, String retentionPeriod) {
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }
        if (retentionPeriod == null) {
            retentionPeriod = "10_YEARS";
        }
        
        return switch (retentionPeriod) {
            case "PERMANENT" -> LocalDate.of(9999, 12, 31);
            case "30_YEARS" -> baseDate.plusYears(30);
            case "15_YEARS" -> baseDate.plusYears(15);
            case "10_YEARS" -> baseDate.plusYears(10);
            case "5_YEARS" -> baseDate.plusYears(5);
            default -> baseDate.plusYears(10);
        };
    }

    /**
     * 转换为DTO.
     */
    private ArchiveDTO toDTO(Archive archive) {
        ArchiveDTO dto = new ArchiveDTO();
        dto.setId(archive.getId());
        dto.setArchiveNo(archive.getArchiveNo());
        dto.setArchiveName(archive.getArchiveName());
        dto.setArchiveType(archive.getArchiveType());
        dto.setArchiveTypeName(getArchiveTypeName(archive.getArchiveType()));
        dto.setCategory(archive.getCategory());
        dto.setCategoryName(getCategoryName(archive.getCategory()));
        dto.setDescription(archive.getDescription());
        dto.setSourceType(archive.getSourceType());
        dto.setSourceTypeName(getSourceTypeName(archive.getSourceType()));
        dto.setSourceId(archive.getSourceId());
        dto.setSourceNo(archive.getSourceNo());
        dto.setSourceName(archive.getSourceName());
        dto.setClientName(archive.getClientName());
        dto.setResponsiblePerson(archive.getResponsiblePerson());
        dto.setCaseCloseDate(archive.getCaseCloseDate());
        dto.setVolumeCount(archive.getVolumeCount());
        dto.setPageCount(archive.getPageCount());
        dto.setCatalog(archive.getCatalog());
        dto.setLocationId(archive.getLocationId());
        dto.setBoxNo(archive.getBoxNo());
        dto.setHasElectronic(archive.getHasElectronic());
        dto.setRetentionPeriod(archive.getRetentionPeriod());
        dto.setRetentionPeriodName(getRetentionPeriodName(archive.getRetentionPeriod()));
        dto.setRetentionExpireDate(archive.getRetentionExpireDate());
        dto.setStatus(archive.getStatus());
        dto.setStatusName(getStatusName(archive.getStatus()));
        dto.setStoredAt(archive.getStoredAt());
        dto.setReceivedAt(archive.getReceivedAt());
        dto.setRemarks(archive.getRemarks());
        dto.setCreatedAt(archive.getCreatedAt());
        dto.setUpdatedAt(archive.getUpdatedAt());
        return dto;
    }

    private ArchiveDTO.ArchiveFileDTO toFileDTO(ArchiveFile file) {
        ArchiveDTO.ArchiveFileDTO dto = new ArchiveDTO.ArchiveFileDTO();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setFileType(file.getFileType());
        dto.setFileSize(file.getFileSize());
        dto.setCategory(file.getCategory());
        dto.setCategoryName(getFileCategoryName(file.getCategory()));
        dto.setSortOrder(file.getSortOrder());
        dto.setDescription(file.getDescription());
        dto.setCreatedAt(file.getCreatedAt());
        // dto.setDownloadUrl(minioService.getPresignedUrl(file.getStoragePath()));
        return dto;
    }

    private String getArchiveTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "LITIGATION" -> "诉讼案件";
            case "NON_LITIGATION" -> "非诉项目";
            case "CONSULTATION" -> "咨询";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    private String getCategoryName(String category) {
        if (category == null) return null;
        return switch (category) {
            case "CASE" -> "案件档案";
            case "CONTRACT" -> "合同档案";
            case "PERSONNEL" -> "人事档案";
            case "FINANCE" -> "财务档案";
            case "OTHER" -> "其他";
            default -> category;
        };
    }

    private String getSourceTypeName(String sourceType) {
        if (sourceType == null) return null;
        return switch (sourceType) {
            case "LAW_FIRM" -> "律所系统";
            case "MANUAL" -> "手动录入";
            case "IMPORT" -> "批量导入";
            case "EXTERNAL" -> "外部系统";
            default -> sourceType;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "RECEIVED" -> "已接收";
            case "PENDING" -> "待入库";
            case "STORED" -> "已入库";
            case "BORROWED" -> "借出中";
            case "PENDING_DESTROY" -> "待销毁";
            case "DESTROYED" -> "已销毁";
            default -> status;
        };
    }

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

    private String getFileCategoryName(String category) {
        if (category == null) return null;
        return switch (category) {
            case "COVER" -> "封面";
            case "CATALOG" -> "目录";
            case "DOCUMENT" -> "文档";
            case "ATTACHMENT" -> "附件";
            default -> category;
        };
    }
}
