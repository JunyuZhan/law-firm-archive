package com.lawfirm.application.archive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 档案数据收集服务. 负责在项目归档时收集所有相关数据 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveDataCollectorService {

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** JDBC模板 */
  private final JdbcTemplate jdbcTemplate;

  /** JSON映射器 */
  private final ObjectMapper objectMapper;

  /**
   * 收集项目的所有相关数据.
   *
   * @param matterId 项目ID
   * @return 归档数据快照
   */
  public ArchiveDataSnapshot collectMatterData(final Long matterId) {
    Matter matter = matterRepository.getByIdOrThrow(matterId, "项目不存在");

    ArchiveDataSnapshot snapshot = new ArchiveDataSnapshot();
    snapshot.setMatterId(matterId);
    snapshot.setMatterNo(matter.getMatterNo());
    snapshot.setMatterName(matter.getName());
    snapshot.setCollectedAt(LocalDateTime.now());

    // 收集各类数据
    snapshot.setMatterInfo(collectMatterInfo(matterId));
    snapshot.setClientInfo(collectClientInfo(matterId));
    snapshot.setParticipants(collectParticipants(matterId));
    snapshot.setContractInfo(collectContractInfo(matterId));
    snapshot.setFeeRecords(collectFeeRecords(matterId));
    snapshot.setPaymentRecords(collectPaymentRecords(matterId));
    snapshot.setExpenseRecords(collectExpenseRecords(matterId));
    snapshot.setTimesheets(collectTimesheets(matterId));
    snapshot.setDocuments(collectDocuments(matterId));
    snapshot.setDossierItems(collectDossierItems(matterId));
    snapshot.setEvidences(collectEvidences(matterId));
    snapshot.setApprovals(collectApprovals(matterId));
    snapshot.setSealApplications(collectSealApplications(matterId));
    snapshot.setLetterApplications(collectLetterApplications(matterId));
    snapshot.setConflictChecks(collectConflictChecks(matterId));
    snapshot.setDeadlines(collectDeadlines(matterId));
    snapshot.setTasks(collectTasks(matterId));
    snapshot.setSchedules(collectSchedules(matterId));
    snapshot.setQualityChecks(collectQualityChecks(matterId));
    snapshot.setRiskWarnings(collectRiskWarnings(matterId));

    log.info("项目数据收集完成: matterId={}, matterNo={}", matterId, matter.getMatterNo());
    return snapshot;
  }

  /**
   * 将快照转换为JSON字符串.
   *
   * @param snapshot 数据快照
   * @return JSON字符串
   */
  public String snapshotToJson(final ArchiveDataSnapshot snapshot) {
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (Exception e) {
      log.error("快照序列化失败", e);
      throw new BusinessException("数据序列化失败");
    }
  }

  /**
   * 将JSON字符串转换为快照对象. 用于验证序列化/反序列化的完整性
   *
   * @param json JSON字符串
   * @return 数据快照
   */
  public ArchiveDataSnapshot jsonToSnapshot(final String json) {
    try {
      return objectMapper.readValue(json, ArchiveDataSnapshot.class);
    } catch (Exception e) {
      log.error("快照反序列化失败", e);
      throw new BusinessException("数据反序列化失败: " + e.getMessage());
    }
  }

  /**
   * 检查归档必填项是否完整.
   *
   * @param matterId 项目ID
   * @return 检查结果
   */
  public ArchiveCheckResult checkArchiveRequirements(final Long matterId) {
    ArchiveCheckResult result = new ArchiveCheckResult();
    result.setMatterId(matterId);
    result.setPassed(true);
    List<String> missingItems = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // 1. 检查项目状态
    Matter matter = matterRepository.getByIdOrThrow(matterId, "项目不存在");
    if (!"CLOSED".equals(matter.getStatus()) && !"ARCHIVED".equals(matter.getStatus())) {
      result.setPassed(false);
      missingItems.add("项目状态必须为已结案或已归档");
    }

    // 2. 检查必填的卷宗目录项是否有文件
    List<Map<String, Object>> requiredItems =
        jdbcTemplate.queryForList(
            "SELECT mdi.id, mdi.name, mdi.document_count "
                + "FROM matter_dossier_item mdi "
                + "JOIN dossier_template_item dti ON mdi.name = dti.name "
                + "JOIN dossier_template dt ON dti.template_id = dt.id "
                + "WHERE mdi.matter_id = ? AND dti.required = true",
            matterId);

    for (Map<String, Object> item : requiredItems) {
      Integer docCount = (Integer) item.get("document_count");
      if (docCount == null || docCount == 0) {
        result.setPassed(false);
        missingItems.add("必填目录缺少文件: " + item.get("name"));
      }
    }

    // 3. 检查是否有委托合同
    Integer contractCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM finance_contract WHERE matter_id = ?", Integer.class, matterId);
    if (contractCount == null || contractCount == 0) {
      result.setPassed(false);
      missingItems.add("缺少委托合同");
    }

    // 4. 检查是否有结案审批记录
    Integer closeApprovalCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workbench_approval "
                + "WHERE business_type = 'MATTER_CLOSE' AND business_id = ? AND status = 'APPROVED'",
            Integer.class,
            matterId);
    if (closeApprovalCount == null || closeApprovalCount == 0) {
      warnings.add("未找到结案审批记录（可能是旧项目）");
    }

    // 5. 检查工时记录
    Integer timesheetCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM timesheet WHERE matter_id = ?", Integer.class, matterId);
    if (timesheetCount == null || timesheetCount == 0) {
      warnings.add("没有工时记录");
    }

    result.setMissingItems(missingItems);
    result.setWarnings(warnings);
    return result;
  }

  /**
   * 获取可用的数据源配置.
   *
   * @return 数据源列表
   */
  public List<Map<String, Object>> getAvailableDataSources() {
    return jdbcTemplate.queryForList(
        "SELECT id, source_name, source_table, source_type, dossier_folder, "
            + "is_required, sort_order, description "
            + "FROM archive_data_source WHERE is_enabled = true AND deleted = false "
            + "ORDER BY sort_order");
  }

  // ==================== 私有方法：收集各类数据 ====================

  /**
   * 收集项目信息.
   *
   * @param matterId 项目ID
   * @return 项目信息
   */
  private Map<String, Object> collectMatterInfo(final Long matterId) {
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            "SELECT m.*, "
                + "u.real_name as lead_lawyer_name, "
                + "d.name as department_name "
                + "FROM matter m "
                + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
                + "LEFT JOIN sys_dept d ON m.department_id = d.id "
                + "WHERE m.id = ?",
            matterId);
    return list.isEmpty() ? Collections.emptyMap() : list.get(0);
  }

  /**
   * 收集客户信息.
   *
   * @param matterId 项目ID
   * @return 客户信息列表
   */
  private List<Map<String, Object>> collectClientInfo(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT mc.*, c.name as client_name, c.contact_person, c.phone, c.email, c.address "
            + "FROM matter_client mc "
            + "JOIN crm_client c ON mc.client_id = c.id "
            + "WHERE mc.matter_id = ?",
        matterId);
  }

  /**
   * 收集团队成员信息.
   *
   * @param matterId 项目ID
   * @return 团队成员列表
   */
  private List<Map<String, Object>> collectParticipants(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT mp.*, u.real_name, u.phone, u.email "
            + "FROM matter_participant mp "
            + "JOIN sys_user u ON mp.user_id = u.id "
            + "WHERE mp.matter_id = ?",
        matterId);
  }

  /**
   * 收集合同信息.
   *
   * @param matterId 项目ID
   * @return 合同信息列表
   */
  private List<Map<String, Object>> collectContractInfo(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM finance_contract WHERE matter_id = ?", matterId);
  }

  /**
   * 收费记录信息.
   *
   * @param matterId 项目ID
   * @return 收费记录列表
   */
  private List<Map<String, Object>> collectFeeRecords(final Long matterId) {
    return jdbcTemplate.queryForList("SELECT * FROM finance_fee WHERE matter_id = ?", matterId);
  }

  /**
   * 收集付款记录信息.
   *
   * @param matterId 项目ID
   * @return 付款记录列表
   */
  private List<Map<String, Object>> collectPaymentRecords(final Long matterId) {
    return jdbcTemplate.queryForList("SELECT * FROM finance_payment WHERE matter_id = ?", matterId);
  }

  /**
   * 收集费用记录信息.
   *
   * @param matterId 项目ID
   * @return 费用记录列表
   */
  private List<Map<String, Object>> collectExpenseRecords(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM finance_expense WHERE matter_id = ? OR allocated_to_matter_id = ?",
        matterId,
        matterId);
  }

  /**
   * 收集工时记录信息.
   *
   * @param matterId 项目ID
   * @return 工时记录列表
   */
  private List<Map<String, Object>> collectTimesheets(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT t.*, u.real_name as user_name "
            + "FROM timesheet t "
            + "JOIN sys_user u ON t.user_id = u.id "
            + "WHERE t.matter_id = ? "
            + "ORDER BY t.work_date",
        matterId);
  }

  /**
   * 收集文档信息.
   *
   * @param matterId 项目ID
   * @return 文档列表
   */
  private List<Map<String, Object>> collectDocuments(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT d.*, mdi.name as dossier_folder_name "
            + "FROM doc_document d "
            + "LEFT JOIN matter_dossier_item mdi ON d.dossier_item_id = mdi.id "
            + "WHERE d.matter_id = ? AND d.deleted = false "
            + "ORDER BY d.dossier_item_id, d.created_at",
        matterId);
  }

  /**
   * 收集卷宗目录信息.
   *
   * @param matterId 项目ID
   * @return 卷宗目录列表
   */
  private List<Map<String, Object>> collectDossierItems(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM matter_dossier_item WHERE matter_id = ? ORDER BY sort_order", matterId);
  }

  /**
   * 收集证据信息.
   *
   * @param matterId 项目ID
   * @return 证据列表
   */
  private List<Map<String, Object>> collectEvidences(final Long matterId) {
    return jdbcTemplate.queryForList("SELECT * FROM evidence WHERE matter_id = ?", matterId);
  }

  /**
   * 收集审批记录信息.
   *
   * @param matterId 项目ID
   * @return 审批记录列表
   */
  private List<Map<String, Object>> collectApprovals(final Long matterId) {
    // 收集与项目相关的所有审批记录
    return jdbcTemplate.queryForList(
        "SELECT * FROM workbench_approval "
            + "WHERE business_id = ? OR business_id IN "
            + "(SELECT id FROM finance_contract WHERE matter_id = ?) OR "
            + "business_id IN (SELECT id FROM seal_application WHERE matter_id = ?) "
            + "ORDER BY created_at",
        matterId,
        matterId,
        matterId);
  }

  /**
   * 收集用印申请信息.
   *
   * @param matterId 项目ID
   * @return 用印申请列表
   */
  private List<Map<String, Object>> collectSealApplications(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM seal_application WHERE matter_id = ?", matterId);
  }

  /**
   * 收集函件申请信息.
   *
   * @param matterId 项目ID
   * @return 函件申请列表
   */
  private List<Map<String, Object>> collectLetterApplications(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM letter_application WHERE matter_id = ?", matterId);
  }

  /**
   * 收集利益冲突检查信息.
   *
   * @param matterId 项目ID
   * @return 利益冲突检查列表
   */
  private List<Map<String, Object>> collectConflictChecks(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM crm_conflict_check WHERE matter_id = ?", matterId);
  }

  /**
   * 收集期限信息.
   *
   * @param matterId 项目ID
   * @return 期限列表
   */
  private List<Map<String, Object>> collectDeadlines(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM matter_deadline WHERE matter_id = ? ORDER BY deadline_date", matterId);
  }

  /**
   * 收集任务信息.
   *
   * @param matterId 项目ID
   * @return 任务列表
   */
  private List<Map<String, Object>> collectTasks(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT t.*, u.real_name as assignee_name "
            + "FROM task t "
            + "LEFT JOIN sys_user u ON t.assignee_id = u.id "
            + "WHERE t.matter_id = ?",
        matterId);
  }

  /**
   * 收集日程安排信息.
   *
   * @param matterId 项目ID
   * @return 日程安排列表
   */
  private List<Map<String, Object>> collectSchedules(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM schedule WHERE matter_id = ? ORDER BY start_time", matterId);
  }

  /**
   * 收集质检信息.
   *
   * @param matterId 项目ID
   * @return 质检记录列表
   */
  private List<Map<String, Object>> collectQualityChecks(final Long matterId) {
    return jdbcTemplate.queryForList("SELECT * FROM quality_check WHERE matter_id = ?", matterId);
  }

  /**
   * 收集风险预警信息.
   *
   * @param matterId 项目ID
   * @return 风险预警列表
   */
  private List<Map<String, Object>> collectRiskWarnings(final Long matterId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM risk_warning WHERE matter_id = ? ORDER BY created_at", matterId);
  }

  // ==================== 内部数据类 ====================

  /** 归档数据快照. */
  @Data
  public static class ArchiveDataSnapshot {
    /** 项目ID */
    private Long matterId;

    /** 项目编号 */
    private String matterNo;

    /** 项目名称 */
    private String matterName;

    /** 收集时间 */
    private LocalDateTime collectedAt;

    // 核心信息
    /** 项目信息 */
    private Map<String, Object> matterInfo;

    /** 客户信息 */
    private List<Map<String, Object>> clientInfo;

    /** 团队成员 */
    private List<Map<String, Object>> participants;

    // 合同与费用
    /** 合同信息 */
    private List<Map<String, Object>> contractInfo;

    /** 收费记录 */
    private List<Map<String, Object>> feeRecords;

    /** 付款记录 */
    private List<Map<String, Object>> paymentRecords;

    /** 费用记录 */
    private List<Map<String, Object>> expenseRecords;

    // 工时
    /** 工时记录 */
    private List<Map<String, Object>> timesheets;

    // 文档
    /** 文档列表 */
    private List<Map<String, Object>> documents;

    /** 卷宗目录 */
    private List<Map<String, Object>> dossierItems;

    /** 证据列表 */
    private List<Map<String, Object>> evidences;

    // 审批与用印
    /** 审批记录 */
    private List<Map<String, Object>> approvals;

    /** 用印申请 */
    private List<Map<String, Object>> sealApplications;

    /** 函件申请 */
    private List<Map<String, Object>> letterApplications;

    // 其他
    /** 利益冲突检查 */
    private List<Map<String, Object>> conflictChecks;

    /** 期限列表 */
    private List<Map<String, Object>> deadlines;

    /** 任务列表 */
    private List<Map<String, Object>> tasks;

    /** 日程安排 */
    private List<Map<String, Object>> schedules;

    /** 质检记录 */
    private List<Map<String, Object>> qualityChecks;

    /** 风险预警 */
    private List<Map<String, Object>> riskWarnings;

    /**
     * 获取数据统计.
     *
     * @return 统计信息
     */
    public Map<String, Integer> getStatistics() {
      Map<String, Integer> stats = new LinkedHashMap<>();
      stats.put("客户数", clientInfo != null ? clientInfo.size() : 0);
      stats.put("团队成员数", participants != null ? participants.size() : 0);
      stats.put("合同数", contractInfo != null ? contractInfo.size() : 0);
      stats.put("收费记录数", feeRecords != null ? feeRecords.size() : 0);
      stats.put("工时记录数", timesheets != null ? timesheets.size() : 0);
      stats.put("文档数", documents != null ? documents.size() : 0);
      stats.put("证据数", evidences != null ? evidences.size() : 0);
      stats.put("审批记录数", approvals != null ? approvals.size() : 0);
      stats.put("任务数", tasks != null ? tasks.size() : 0);
      return stats;
    }
  }

  /** 归档检查结果. */
  @Data
  public static class ArchiveCheckResult {
    /** 项目ID */
    private Long matterId;

    /** 是否通过 */
    private boolean passed;

    /** 缺失项列表 */
    private List<String> missingItems;

    /** 警告列表 */
    private List<String> warnings;
  }
}
