package com.archivesystem.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案ES文档映射
 * 用于全文检索
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "archives")
@Setting(settingPath = "elasticsearch/archive-settings.json")
public class ArchiveDocument {

    @Id
    private Long id;

    /**
     * 档案号
     */
    @Field(type = FieldType.Keyword)
    private String archiveNo;

    /**
     * 题名（支持分词搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 全宗ID
     */
    @Field(type = FieldType.Long)
    private Long fondsId;

    /**
     * 全宗号
     */
    @Field(type = FieldType.Keyword)
    private String fondsNo;

    /**
     * 分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 分类号
     */
    @Field(type = FieldType.Keyword)
    private String categoryCode;

    /**
     * 档案门类
     */
    @Field(type = FieldType.Keyword)
    private String archiveType;

    /**
     * 责任者
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String responsibility;

    /**
     * 案件编号
     */
    @Field(type = FieldType.Keyword)
    private String caseNo;

    /**
     * 案件名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String caseName;

    /**
     * 委托人
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String clientName;

    /**
     * 主办律师
     */
    @Field(type = FieldType.Keyword)
    private String lawyerName;

    /**
     * 主办律师规范化标记列表（用于对象级权限过滤）
     */
    @Field(type = FieldType.Keyword)
    private List<String> lawyerTokens;

    /**
     * 关键词
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String keywords;

    /**
     * 摘要
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String archiveAbstract;

    /**
     * 备注
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String remarks;

    /**
     * 文件内容（OCR提取的文本内容）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String fileContent;

    /**
     * 保管期限
     */
    @Field(type = FieldType.Keyword)
    private String retentionPeriod;

    /**
     * 密级
     */
    @Field(type = FieldType.Keyword)
    private String securityLevel;

    /**
     * 来源类型
     */
    @Field(type = FieldType.Keyword)
    private String sourceType;

    /**
     * 状态
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * 归档日期
     */
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate archiveDate;

    /**
     * 文件日期
     */
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate documentDate;

    /**
     * 接收时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime receivedAt;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    /**
     * 创建人ID（用于对象级权限过滤）
     */
    @Field(type = FieldType.Long)
    private Long createdBy;

    /**
     * 接收人ID（用于对象级权限过滤）
     */
    @Field(type = FieldType.Long)
    private Long receivedBy;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    /**
     * 文件数量
     */
    @Field(type = FieldType.Integer)
    private Integer fileCount;

    /**
     * 归档年份（用于聚合统计）
     */
    @Field(type = FieldType.Integer)
    private Integer archiveYear;

    /**
     * 文件名列表（用于搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private List<String> fileNames;
}
