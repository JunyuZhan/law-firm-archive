package com.archivesystem.dto.archive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 档案搜索结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveSearchResult {

    /**
     * 搜索结果列表
     */
    private List<ArchiveSearchHit> hits;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int pageNum;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 搜索耗时（毫秒）
     */
    private long took;

    /**
     * 聚合统计结果
     */
    private Aggregations aggregations;

    /**
     * 单条搜索结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveSearchHit {
        /**
         * 档案ID
         */
        private Long id;

        /**
         * 档案号
         */
        private String archiveNo;

        /**
         * 题名
         */
        private String title;

        /**
         * 全宗号
         */
        private String fondsNo;

        /**
         * 分类号
         */
        private String categoryCode;

        /**
         * 档案类型
         */
        private String archiveType;

        /**
         * 案件编号
         */
        private String caseNo;

        /**
         * 案件名称
         */
        private String caseName;

        /**
         * 委托人
         */
        private String clientName;

        /**
         * 主办律师
         */
        private String lawyerName;

        /**
         * 保管期限
         */
        private String retentionPeriod;

        /**
         * 密级
         */
        private String securityLevel;

        /**
         * 状态
         */
        private String status;

        /**
         * 文件数量
         */
        private Integer fileCount;

        /**
         * 接收时间
         */
        private String receivedAt;

        /**
         * 搜索得分
         */
        private Float score;

        /**
         * 高亮内容
         */
        private Map<String, List<String>> highlights;
    }

    /**
     * 聚合统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aggregations {
        /**
         * 按档案类型统计
         */
        private List<BucketItem> byArchiveType;

        /**
         * 按年份统计
         */
        private List<BucketItem> byYear;

        /**
         * 按全宗统计
         */
        private List<BucketItem> byFonds;

        /**
         * 按状态统计
         */
        private List<BucketItem> byStatus;

        /**
         * 按保管期限统计
         */
        private List<BucketItem> byRetentionPeriod;
    }

    /**
     * 聚合桶项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BucketItem {
        private String key;
        private long count;
    }
}
