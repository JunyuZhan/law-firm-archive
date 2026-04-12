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
import com.archivesystem.service.FileStorageService;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 电子借阅链接服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowLinkServiceImpl implements BorrowLinkService {

    private final BorrowLinkMapper borrowLinkMapper;
    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final BorrowApplicationMapper borrowApplicationMapper;
    private final FileStorageService fileStorageService;

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
        validateArchiveIdentity(archive, request.getArchiveId(), request.getArchiveNo());

        // 检查档案是否允许借阅
        if (Archive.STATUS_DESTROYED.equals(archive.getStatus())) {
            throw new BusinessException("该档案已销毁，无法借阅");
        }

        BorrowLink reusableLink = findReusableExternalLink(archive, request);
        if (reusableLink != null) {
            log.info("复用已存在的电子借阅链接: linkId={}, archiveId={}, userId={}",
                    reusableLink.getId(), archive.getId(), request.getUserId());
            return buildApplyResponse(reusableLink, archive);
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

        return buildApplyResponse(link, archive);
    }

    @Override
    @Transactional
    public BorrowLink generateLinkForBorrow(Long borrowId, Integer expireDays, Boolean allowDownload) {
        BorrowApplication application = borrowApplicationMapper.selectById(borrowId);
        if (application == null) {
            throw new NotFoundException("借阅申请不存在");
        }
        if (!BorrowApplication.STATUS_APPROVED.equals(application.getStatus())
                && !BorrowApplication.STATUS_BORROWED.equals(application.getStatus())) {
            throw new BusinessException("仅已审批或借出中的借阅申请可以生成电子借阅链接");
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
        boolean canDownload = BorrowApplication.TYPE_DOWNLOAD.equals(application.getBorrowType())
                && !BorrowApplication.STATUS_RETURNED.equals(application.getStatus())
                && (allowDownload == null || allowDownload);

        BorrowLink link = BorrowLink.builder()
                .borrowId(borrowId)
                .archiveId(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .accessToken(accessToken)
                .sourceType(BorrowLink.SOURCE_TYPE_INTERNAL)
                .sourceUserName(application.getApplicantName())
                .borrowPurpose(application.getBorrowPurpose())
                .expireAt(LocalDateTime.now().plusDays(days))
                .allowDownload(canDownload)
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
        increaseBorrowUsage(link.getBorrowId(), 1, 0);

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
    public void recordDownload(String accessToken, Long fileId, String clientIp) {
        BorrowLink link = requireActiveLink(accessToken);
        if (!Boolean.TRUE.equals(link.getAllowDownload())) {
            throw new BusinessException("403", "该链接不允许下载");
        }

        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null || !link.getArchiveId().equals(file.getArchiveId())) {
            throw NotFoundException.of("文件", fileId);
        }

        link.incrementDownloadCount();
        link.setLastAccessAt(LocalDateTime.now());
        link.setLastAccessIp(clientIp);
        borrowLinkMapper.updateById(link);
        increaseBorrowUsage(link.getBorrowId(), 0, 1);
        log.info("记录下载: linkId={}, fileId={}, clientIp={}", link.getId(), fileId, clientIp);
    }

    @Override
    public String getFileAccessUrl(String accessToken, Long fileId, boolean download) {
        BorrowLink link = requireActiveLink(accessToken);
        if (download && !Boolean.TRUE.equals(link.getAllowDownload())) {
            throw new BusinessException("403", "该链接不允许下载");
        }

        int expirySeconds = 60;
        if (link.getExpireAt() != null) {
            long remainingSeconds = Duration.between(LocalDateTime.now(), link.getExpireAt()).getSeconds();
            if (remainingSeconds <= 0) {
                throw new BusinessException("403", "链接已过期");
            }
            expirySeconds = (int) Math.max(1, Math.min(60, remainingSeconds));
        }

        return download
                ? fileStorageService.getBorrowDownloadUrl(link.getArchiveId(), fileId, expirySeconds)
                : fileStorageService.getBorrowPreviewUrl(link.getArchiveId(), fileId, expirySeconds);
    }

    @Override
    public PageResult<BorrowLink> getList(Long archiveId, String status, Boolean allowDownload, String sourceType, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowLink> wrapper = new LambdaQueryWrapper<>();
        
        if (archiveId != null) {
            wrapper.eq(BorrowLink::getArchiveId, archiveId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowLink::getStatus, status);
        }
        if (allowDownload != null) {
            wrapper.eq(BorrowLink::getAllowDownload, allowDownload);
        }
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(BorrowLink::getSourceType, sourceType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(BorrowLink::getArchiveNo, keyword)
                    .or().like(BorrowLink::getSourceUserName, keyword)
                    .or().like(BorrowLink::getSourceSystem, keyword)
                    .or().like(BorrowLink::getBorrowPurpose, keyword));
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
        Map<String, Object> stats = borrowLinkMapper.selectAggregateStats();
        return new BorrowLinkStats(
                asLong(stats, "totalCount"),
                asLong(stats, "activeCount"),
                asLong(stats, "expiredCount"),
                asLong(stats, "revokedCount"),
                asLong(stats, "totalAccessCount"),
                asLong(stats, "totalDownloadCount")
        );
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

    private void validateArchiveIdentity(Archive archive, Long archiveId, String archiveNo) {
        if (archive == null || archiveId == null || !StringUtils.hasText(archiveNo)) {
            return;
        }
        if (!archiveId.equals(archive.getId()) || !archiveNo.equals(archive.getArchiveNo())) {
            throw new BusinessException("archiveId 与 archiveNo 不匹配");
        }
    }

    private BorrowLink findReusableExternalLink(Archive archive, BorrowLinkApplyRequest request) {
        if (archive == null || !StringUtils.hasText(request.getUserId())) {
            return null;
        }
        List<BorrowLink> existingLinks =
                borrowLinkMapper.selectBySourceUser(request.getUserId(), BorrowLink.SOURCE_TYPE_LAW_FIRM);
        return existingLinks.stream()
                .filter(BorrowLink::isValid)
                .filter(link -> archive.getId().equals(link.getArchiveId()))
                .filter(link -> request.getPurpose().equals(link.getBorrowPurpose()))
                .filter(link -> equalsNullable(request.getMaxAccessCount(), link.getMaxAccessCount()))
                .filter(link -> equalsNullable(boolOrDefault(request.getAllowDownload(), true), boolOrDefault(link.getAllowDownload(), true)))
                .findFirst()
                .orElse(null);
    }

    private BorrowLinkApplyResponse buildApplyResponse(BorrowLink link, Archive archive) {
        return BorrowLinkApplyResponse.builder()
                .linkId(link.getId())
                .accessToken(link.getAccessToken())
                .accessUrl(buildAccessUrl(link.getAccessToken()))
                .expireAt(link.getExpireAt())
                .allowDownload(link.getAllowDownload())
                .maxAccessCount(link.getMaxAccessCount())
                .archiveNo(archive.getArchiveNo())
                .archiveTitle(archive.getTitle())
                .build();
    }

    private <T> boolean equalsNullable(T left, T right) {
        return java.util.Objects.equals(left, right);
    }

    private boolean boolOrDefault(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
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

    private long asLong(Map<String, Object> values, String key) {
        if (values == null) {
            return 0L;
        }
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BorrowLink requireActiveLink(String accessToken) {
        BorrowLink link = borrowLinkMapper.selectByAccessToken(accessToken);
        if (link == null) {
            throw new BusinessException("403", "链接不存在或已失效");
        }
        if (BorrowLink.STATUS_REVOKED.equals(link.getStatus())) {
            throw new BusinessException("403", "链接已被撤销");
        }
        if (link.getExpireAt() != null && LocalDateTime.now().isAfter(link.getExpireAt())) {
            if (BorrowLink.STATUS_ACTIVE.equals(link.getStatus())) {
                link.setStatus(BorrowLink.STATUS_EXPIRED);
                borrowLinkMapper.updateById(link);
            }
            throw new BusinessException("403", "链接已过期");
        }
        if (link.getMaxAccessCount() != null && link.getAccessCount() >= link.getMaxAccessCount()) {
            throw new BusinessException("403", "访问次数已达上限");
        }
        return link;
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

        BorrowApplication application = link.getBorrowId() != null
                ? borrowApplicationMapper.selectById(link.getBorrowId())
                : null;

        // 构建借阅人信息
        BorrowLinkAccessResponse.BorrowerInfo borrowerInfo = BorrowLinkAccessResponse.BorrowerInfo.builder()
                .userId(link.getSourceUserId())
                .userName(link.getSourceUserName())
                .purpose(link.getBorrowPurpose())
                .sourceSystem(link.getSourceSystem())
                .borrowType(application != null ? application.getBorrowType() : null)
                .expectedReturnDate(application != null ? application.getExpectedReturnDate() : null)
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
        return BorrowLinkAccessResponse.FileInfo.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileExtension(file.getFileExtension())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileCategory(file.getFileCategory())
                .previewUrl(null)
                .downloadUrl(Boolean.TRUE.equals(allowDownload) ? "" : null)
                .isLongTermFormat(file.getIsLongTermFormat())
                .build();
    }

    private void increaseBorrowUsage(Long borrowId, int viewIncrement, int downloadIncrement) {
        if (borrowId == null) {
            return;
        }
        BorrowApplication application = borrowApplicationMapper.selectById(borrowId);
        if (application == null) {
            return;
        }
        application.setViewCount((application.getViewCount() == null ? 0 : application.getViewCount()) + viewIncrement);
        application.setDownloadCount((application.getDownloadCount() == null ? 0 : application.getDownloadCount()) + downloadIncrement);
        application.setLastAccessAt(LocalDateTime.now());
        borrowApplicationMapper.updateById(application);
    }
}
