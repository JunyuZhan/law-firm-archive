package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.dto.borrow.BorrowLinkAccessResponse;
import com.archivesystem.dto.borrow.BorrowLinkResponse;
import com.archivesystem.entity.BorrowLink;

import java.util.List;

/**
 * 电子借阅链接服务接口.
 * @author junyuzhan
 */
public interface BorrowLinkService {

    /**
     * 申请借阅链接（供外部系统调用）.
     *
     * @param request 申请请求
     * @return 申请响应（包含访问链接）
     */
    BorrowLinkApplyResponse applyLink(BorrowLinkApplyRequest request, String sourceCode);

    /**
     * 为借阅申请生成访问链接（内部使用）.
     *
     * @param borrowId 借阅申请ID
     * @param expireDays 有效期天数
     * @param allowDownload 是否允许下载
     * @return 生成的链接
     */
    BorrowLinkResponse generateLinkForBorrow(Long borrowId, Integer expireDays, Boolean allowDownload);

    /**
     * 验证并获取链接信息.
     *
     * @param accessToken 访问令牌
     * @return 访问响应（包含档案和文件信息）
     */
    BorrowLinkAccessResponse validateAndAccess(String accessToken, String clientIp);

    /**
     * 根据令牌获取链接.
     *
     * @param accessToken 访问令牌
     * @return 链接信息
     */
    BorrowLink getByAccessToken(String accessToken);

    /**
     * 根据ID获取链接.
     *
     * @param id 链接ID
     * @return 链接信息
     */
    BorrowLinkResponse getById(Long id);

    /**
     * 撤销链接.
     *
     * @param id 链接ID
     * @param reason 撤销原因
     */
    void revoke(Long id, String reason, String sourceCode);

    /**
     * 记录下载.
     *
     * @param accessToken 访问令牌
     * @param fileId 文件ID
     * @param clientIp 客户端IP
     */
    void recordDownload(String accessToken, Long fileId, String clientIp);

    /**
     * 获取公开借阅文件的短时访问地址.
     */
    String getFileAccessUrl(String accessToken, Long fileId, boolean download, String clientIp);

    /**
     * 分页查询链接列表.
     *
     * @param archiveId 档案ID（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<BorrowLinkResponse> getList(Long archiveId, String status, Boolean allowDownload, String sourceType, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取档案的有效链接列表.
     *
     * @param archiveId 档案ID
     * @return 链接列表
     */
    List<BorrowLinkResponse> getActiveByArchiveId(Long archiveId);

    /**
     * 更新过期链接状态.
     *
     * @return 更新数量
     */
    int updateExpiredLinks();

    /**
     * 获取链接统计信息.
     *
     * @return 统计数据
     */
    BorrowLinkStats getStats();

    /**
     * 链接统计信息.
     */
    record BorrowLinkStats(
            long totalCount,
            long activeCount,
            long expiredCount,
            long revokedCount,
            long totalAccessCount,
            long totalDownloadCount
    ) {}
}
