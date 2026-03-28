package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.dto.borrow.BorrowLinkAccessResponse;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.BorrowLink;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.BorrowLinkMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.BorrowLinkService;
import com.archivesystem.service.MinioService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 电子借阅链接服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowLinkServiceImpl implements BorrowLinkService {

    private final BorrowLinkMapper borrowLinkMapper;
    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final BorrowApplicationMapper borrowApplicationMapper;
    private final MinioService minioService;

    @Value("${app.borrow-link.base-url:}")
    private String baseUrl;

    @Value("${app.borrow-link.default-expire-days:7}")
    private int defaultExpireDays;

    @Value("${app.borrow-link.max-expire-days:30}")
    private int maxExpireDays;

    @Override
    @Transactional
    public BorrowLinkApplyResponse applyLink(BorrowLinkApplyRequest request) {
        // 查找档案
        Archive archive = findArchive(request.getArchiveId(), request.getArchiveNo());
        if (archive == null) {
            throw new NotFoundException("档案不存在");
        }

        // 检查档案是否允许借阅
        if (Archive.STATUS_DESTROYED.equals(archive.getStatus())) {
            throw new BusinessException("该档案已销毁，无法借阅");
        }

        // 生成访问令牌
        String accessToken = generateAccessToken();

        // 计算过期时间
        int expireDays = request.getExpireDays() != null ? request.getExpireDays() : defaultExpireDays;
        if (expireDays > maxExpireDays) {
            expireDays = maxExpireDays;
        }
        LocalDateTime expireAt = LocalDateTime.now().plusDays(expireDays);

        // 创建借阅链接
        BorrowLink link = BorrowLink.builder()
                .archiveId(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .accessToken(accessToken)
                .sourceType(BorrowLink.SOURCE_TYPE_LAW_FIRM)
                .sourceUserId(request.getUserId())
                .sourceUserName(request.getUserName())
                .borrowPurpose(request.getPurpose())
                .expireAt(expireAt)
                .maxAccessCount(request.getMaxAccessCount())
                .allowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : true)
                .status(BorrowLink.STATUS_ACTIVE)
                .build();

        borrowLinkMapper.insert(link);
        log.info("电子借阅链接创建: linkId={}, archiveId={}, userId={}", link.getId(), archive.getId(), request.getUserId());

        // 构建响应
        return BorrowLinkApplyResponse.builder()
                .linkId(link.getId())
                .accessToken(accessToken)
                .accessUrl(buildAccessUrl(accessToken))
                .expireAt(expireAt)
                .allowDownload(link.getAllowDownload())
                .maxAccessCount(link.getMaxAccessCount())
                .archiveNo(archive.getArchiveNo())
                .archiveTitle(archive.getTitle())
                .build();
    }

    @Override
    @Transactional
    public BorrowLink generateLinkForBorrow(Long borrowId, Integer expireDays, Boolean allowDownload) {
        BorrowApplication application = borrowApplicationMapper.selectById(borrowId);
        if (application == null) {
            throw new NotFoundException("借阅申请不存在");
        }

        Archive archive = archiveMapper.selectById(application.getArchiveId());
        if (archive == null) {
            throw new NotFoundException("档案不存在");
        }

        // 检查是否已有链接
        BorrowLink existing = borrowLinkMapper.selectByBorrowId(borrowId);
        if (existing != null && existing.isValid()) {
            return existing;
        }

        String accessToken = generateAccessToken();
        int days = expireDays != null ? expireDays : defaultExpireDays;
        if (days > maxExpireDays) {
            days = maxExpireDays;
        }

        BorrowLink link = BorrowLink.builder()
                .borrowId(borrowId)
                .archiveId(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .accessToken(accessToken)
                .sourceType(BorrowLink.SOURCE_TYPE_INTERNAL)
                .sourceUserName(application.getApplicantName())
                .borrowPurpose(application.getBorrowPurpose())
                .expireAt(LocalDateTime.now().plusDays(days))
                .allowDownload(allowDownload != null ? allowDownload : true)
                .status(BorrowLink.STATUS_ACTIVE)
                .build();

        borrowLinkMapper.insert(link);
        log.info("内部借阅链接生成: linkId={}, borrowId={}", link.getId(), borrowId);

        return link;
    }

    @Override
    @Transactional
    public BorrowLinkAccessResponse validateAndAccess(String accessToken, String clientIp) {
        BorrowLink link = borrowLinkMapper.selectByAccessToken(accessToken);
        if (link == null) {
            return BorrowLinkAccessResponse.invalid("链接不存在或已失效");
        }

        // 检查状态
        if (BorrowLink.STATUS_REVOKED.equals(link.getStatus())) {
            return BorrowLinkAccessResponse.invalid("链接已被撤销");
        }

        // 检查过期
        if (link.getExpireAt() != null && LocalDateTime.now().isAfter(link.getExpireAt())) {
            if (BorrowLink.STATUS_ACTIVE.equals(link.getStatus())) {
                link.setStatus(BorrowLink.STATUS_EXPIRED);
                borrowLinkMapper.updateById(link);
            }
            return BorrowLinkAccessResponse.invalid("链接已过期");
        }

        // 检查访问次数
        if (link.getMaxAccessCount() != null && link.getAccessCount() >= link.getMaxAccessCount()) {
            return BorrowLinkAccessResponse.invalid("访问次数已达上限");
        }

        // 记录访问
        link.incrementAccessCount(clientIp);
        borrowLinkMapper.updateById(link);

        // 获取档案信息
        Archive archive = archiveMapper.selectById(link.getArchiveId());
        if (archive == null) {
            return BorrowLinkAccessResponse.invalid("档案不存在");
        }

        // 获取文件列表
        List<DigitalFile> files = digitalFileMapper.selectByArchiveId(link.getArchiveId());

        // 构建响应
        return buildAccessResponse(link, archive, files);
    }

    @Override
    public BorrowLink getByAccessToken(String accessToken) {
        return borrowLinkMapper.selectByAccessToken(accessToken);
    }

    @Override
    public BorrowLink getById(Long id) {
        BorrowLink link = borrowLinkMapper.selectById(id);
        if (link == null) {
            throw NotFoundException.of("借阅链接", id);
        }
        return link;
    }

    @Override
    @Transactional
    public void revoke(Long id, String reason) {
        BorrowLink link = getById(id);
        
        if (!BorrowLink.STATUS_ACTIVE.equals(link.getStatus())) {
            throw new BusinessException("该链接不是有效状态，无法撤销");
        }

        link.setStatus(BorrowLink.STATUS_REVOKED);
        link.setRevokeReason(reason);
        link.setRevokedAt(LocalDateTime.now());
        borrowLinkMapper.updateById(link);

        log.info("借阅链接撤销: linkId={}, reason={}", id, reason);
    }

    @Override
    @Transactional
    public void recordDownload(String accessToken, Long fileId) {
        BorrowLink link = borrowLinkMapper.selectByAccessToken(accessToken);
        if (link != null && link.isValid()) {
            link.incrementDownloadCount();
            borrowLinkMapper.updateById(link);
            log.debug("记录下载: linkId={}, fileId={}", link.getId(), fileId);
        }
    }

    @Override
    public PageResult<BorrowLink> getList(Long archiveId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowLink> wrapper = new LambdaQueryWrapper<>();
        
        if (archiveId != null) {
            wrapper.eq(BorrowLink::getArchiveId, archiveId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowLink::getStatus, status);
        }
        
        wrapper.orderByDesc(BorrowLink::getCreatedAt);

        Page<BorrowLink> page = new Page<>(pageNum, pageSize);
        Page<BorrowLink> result = borrowLinkMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public List<BorrowLink> getActiveByArchiveId(Long archiveId) {
        LambdaQueryWrapper<BorrowLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowLink::getArchiveId, archiveId)
               .eq(BorrowLink::getStatus, BorrowLink.STATUS_ACTIVE)
               .gt(BorrowLink::getExpireAt, LocalDateTime.now())
               .orderByDesc(BorrowLink::getCreatedAt);
        return borrowLinkMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public int updateExpiredLinks() {
        return borrowLinkMapper.updateExpiredStatus();
    }

    @Override
    public BorrowLinkStats getStats() {
        LambdaQueryWrapper<BorrowLink> wrapper = new LambdaQueryWrapper<>();
        long total = borrowLinkMapper.selectCount(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowLink::getStatus, BorrowLink.STATUS_ACTIVE);
        long active = borrowLinkMapper.selectCount(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowLink::getStatus, BorrowLink.STATUS_EXPIRED);
        long expired = borrowLinkMapper.selectCount(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowLink::getStatus, BorrowLink.STATUS_REVOKED);
        long revoked = borrowLinkMapper.selectCount(wrapper);

        // 统计访问和下载次数
        List<BorrowLink> allLinks = borrowLinkMapper.selectList(new LambdaQueryWrapper<>());
        long totalAccess = allLinks.stream().mapToInt(l -> l.getAccessCount() != null ? l.getAccessCount() : 0).sum();
        long totalDownload = allLinks.stream().mapToInt(l -> l.getDownloadCount() != null ? l.getDownloadCount() : 0).sum();

        return new BorrowLinkStats(total, active, expired, revoked, totalAccess, totalDownload);
    }

    private Archive findArchive(Long archiveId, String archiveNo) {
        if (archiveId != null) {
            return archiveMapper.selectById(archiveId);
        }
        if (StringUtils.hasText(archiveNo)) {
            LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Archive::getArchiveNo, archiveNo)
                   .eq(Archive::getDeleted, false);
            return archiveMapper.selectOne(wrapper);
        }
        return null;
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildAccessUrl(String accessToken) {
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl + "/open/borrow/access/" + accessToken;
        }
        return "/open/borrow/access/" + accessToken;
    }

    private BorrowLinkAccessResponse buildAccessResponse(BorrowLink link, Archive archive, List<DigitalFile> files) {
        // 构建档案信息
        BorrowLinkAccessResponse.ArchiveInfo archiveInfo = BorrowLinkAccessResponse.ArchiveInfo.builder()
                .archiveId(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .title(archive.getTitle())
                .archiveType(archive.getArchiveType())
                .retentionPeriod(archive.getRetentionPeriod())
                .securityLevel(archive.getSecurityLevel())
                .caseName(archive.getCaseName())
                .caseNo(archive.getCaseNo())
                .fileCount(files.size())
                .build();

        // 构建文件列表（生成预签名URL）
        List<BorrowLinkAccessResponse.FileInfo> fileInfos = files.stream()
                .map(f -> buildFileInfo(f, link.getAllowDownload()))
                .collect(Collectors.toList());

        // 构建链接信息
        long remainingSeconds = 0;
        if (link.getExpireAt() != null) {
            remainingSeconds = Duration.between(LocalDateTime.now(), link.getExpireAt()).getSeconds();
            if (remainingSeconds < 0) remainingSeconds = 0;
        }

        BorrowLinkAccessResponse.LinkInfo linkInfo = BorrowLinkAccessResponse.LinkInfo.builder()
                .linkId(link.getId())
                .expireAt(link.getExpireAt())
                .remainingSeconds(remainingSeconds)
                .allowDownload(link.getAllowDownload())
                .accessCount(link.getAccessCount())
                .maxAccessCount(link.getMaxAccessCount())
                .build();

        // 构建借阅人信息
        BorrowLinkAccessResponse.BorrowerInfo borrowerInfo = BorrowLinkAccessResponse.BorrowerInfo.builder()
                .userId(link.getSourceUserId())
                .userName(link.getSourceUserName())
                .purpose(link.getBorrowPurpose())
                .sourceSystem(link.getSourceSystem())
                .build();

        return BorrowLinkAccessResponse.builder()
                .valid(true)
                .archive(archiveInfo)
                .files(fileInfos)
                .linkInfo(linkInfo)
                .borrower(borrowerInfo)
                .build();
    }

    private BorrowLinkAccessResponse.FileInfo buildFileInfo(DigitalFile file, Boolean allowDownload) {
        String previewPath = StringUtils.hasText(file.getConvertedPath()) ? file.getConvertedPath() : file.getStoragePath();
        String previewUrl = minioService.getPresignedUrl(previewPath, 3600);
        
        String downloadUrl = null;
        if (Boolean.TRUE.equals(allowDownload)) {
            downloadUrl = minioService.getPresignedUrl(file.getStoragePath(), 3600);
        }

        return BorrowLinkAccessResponse.FileInfo.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileExtension(file.getFileExtension())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileCategory(file.getFileCategory())
                .previewUrl(previewUrl)
                .downloadUrl(downloadUrl)
                .isLongTermFormat(file.getIsLongTermFormat())
                .build();
    }
}
