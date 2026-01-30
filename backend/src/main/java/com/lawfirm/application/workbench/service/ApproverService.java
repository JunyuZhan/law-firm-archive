package com.lawfirm.application.workbench.service;

import com.lawfirm.common.constant.EmployeeStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 审批人查找服务 根据业务类型和规则查找合适的审批人. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverService {

  /** User Repository. */
  private final UserRepository userRepository;

  /** User Mapper. */
  private final UserMapper userMapper;

  /** Department Repository. */
  private final DepartmentRepository departmentRepository;

  /** 合同金额阈值. */
  @Value("${law-firm.approval.contract-amount-threshold:100000}")
  private java.math.BigDecimal contractAmountThreshold;

  /**
   * 查找合同审批人.
   *
   * <p>规则： - 金额 ≥ 阈值（默认10万）：由主任审批（最高级别） - 金额 < 阈值：由团队负责人审批 - 如果找不到对应角色，降级查找
   *
   * @param contractAmount 合同金额
   * @return 审批人ID
   */
  public Long findContractApprover(final java.math.BigDecimal contractAmount) {
    // 如果合同金额超过阈值，需要主任审批（最高级别）
    if (contractAmount != null && contractAmount.compareTo(contractAmountThreshold) >= 0) {
      Long director = findApproverByRole("DIRECTOR");
      if (director != null) {
        return director;
      }
      // 如果没有主任，降级到团队负责人
      return findApproverByRole("TEAM_LEADER");
    }

    // 普通金额由团队负责人审批
    Long partner = findApproverByRole("TEAM_LEADER");
    if (partner != null) {
      return partner;
    }

    // 如果没有团队负责人，升级到主任
    return findApproverByRole("DIRECTOR");
  }

  /**
   * 查找用印申请审批人 规则：按照部门架构垂直向上查找 1. 当前部门负责人 2. 上级部门负责人（沿部门层级向上） 3. 团队负责人（TEAM_LEADER角色） 4.
   * 主任（DIRECTOR角色）
   *
   * @param applicantId 申请人ID，如果为null则使用当前登录用户
   * @return 审批人ID
   */
  public Long findSealApplicationApprover(final Long applicantId) {
    Long targetApplicantId = applicantId;
    if (targetApplicantId == null) {
      targetApplicantId = SecurityUtils.getUserId();
    }

    User applicant = userRepository.getById(targetApplicantId);
    if (applicant == null) {
      throw new BusinessException("申请人不存在");
    }

    Long currentDeptId = applicant.getDepartmentId();

    // 1. 沿部门层级向上查找部门负责人
    Long deptId = currentDeptId;
    while (deptId != null && deptId > 0) {
      Department dept = departmentRepository.getById(deptId);
      if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(applicantId)) {
        User leader = userRepository.getById(dept.getLeaderId());
        if (leader != null && EmployeeStatus.ACTIVE.equals(leader.getStatus())) {
          // 返回第一个找到的部门负责人（通常部门负责人都有审批权限）
          log.info(
              "找到用印申请审批人（部门负责人）: userId={}, deptId={}, deptName={}",
              leader.getId(),
              deptId,
              dept.getName());
          return dept.getLeaderId();
        }
        deptId = dept.getParentId();
      } else if (dept != null) {
        deptId = dept.getParentId();
      } else {
        deptId = null;
      }
    }

    // 2. 如果没有找到部门负责人，查找团队负责人
    List<Long> partnerIds = userMapper.selectUserIdsByRoleCode("TEAM_LEADER");
    if (partnerIds != null && !partnerIds.isEmpty()) {
      for (Long partnerId : partnerIds) {
        if (!partnerId.equals(applicantId)) {
          User partner = userRepository.getById(partnerId);
          if (partner != null && EmployeeStatus.ACTIVE.equals(partner.getStatus())) {
            log.info("找到用印申请审批人（团队负责人）: userId={}", partnerId);
            return partnerId;
          }
        }
      }
    }

    // 3. 如果没有团队负责人，查找主任
    List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
    if (directorIds != null && !directorIds.isEmpty()) {
      for (Long directorId : directorIds) {
        if (!directorId.equals(applicantId)) {
          User director = userRepository.getById(directorId);
          if (director != null && EmployeeStatus.ACTIVE.equals(director.getStatus())) {
            log.info("找到用印申请审批人（主任）: userId={}", directorId);
            return directorId;
          }
        }
      }
    }

    // 4. 最后降级到默认审批人
    log.warn("未找到合适的审批人，使用默认审批人: applicantId={}", applicantId);
    return findDefaultApprover();
  }

  /**
   * 查找用印申请审批人（兼容旧方法，使用当前登录用户）.
   *
   * @return 审批人ID
   */
  public Long findSealApplicationApprover() {
    return findSealApplicationApprover(null);
  }

  /**
   * 获取用印申请可选审批人列表（申请人架构垂直线上的领导） 包括：当前部门负责人、上级部门负责人、团队负责人、主任
   *
   * @param applicantId 申请人ID，如果为null则使用当前登录用户
   * @return 可选审批人列表
   */
  public List<Map<String, Object>> getSealApplicationAvailableApprovers(final Long applicantId) {
    Long targetApplicantId = applicantId;
    if (targetApplicantId == null) {
      targetApplicantId = SecurityUtils.getUserId();
    }

    User applicant = userRepository.getById(targetApplicantId);
    if (applicant == null) {
      throw new BusinessException("申请人不存在");
    }

    Long currentDeptId = applicant.getDepartmentId();
    List<Map<String, Object>> approvers = new ArrayList<>();
    Set<Long> addedUserIds = new HashSet<>();

    // 1. 沿部门层级向上查找所有部门负责人
    Long deptId = currentDeptId;
    while (deptId != null && deptId > 0) {
      Department dept = departmentRepository.getById(deptId);
      if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(applicantId)) {
        if (!addedUserIds.contains(dept.getLeaderId())) {
          User leader = userRepository.getById(dept.getLeaderId());
          if (leader != null && EmployeeStatus.ACTIVE.equals(leader.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", leader.getId());
            approver.put("realName", leader.getRealName());
            approver.put("departmentName", dept.getName());
            approver.put("position", "部门负责人");
            approvers.add(approver);
            addedUserIds.add(leader.getId());
          }
        }
        deptId = dept.getParentId();
      } else if (dept != null) {
        deptId = dept.getParentId();
      } else {
        deptId = null;
      }
    }

    // 2. 添加所有团队负责人（TEAM_LEADER角色）
    List<Long> partnerIds = userMapper.selectUserIdsByRoleCode("TEAM_LEADER");
    if (partnerIds != null) {
      for (Long partnerId : partnerIds) {
        if (!partnerId.equals(applicantId) && !addedUserIds.contains(partnerId)) {
          User partner = userRepository.getById(partnerId);
          if (partner != null && EmployeeStatus.ACTIVE.equals(partner.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", partner.getId());
            approver.put("realName", partner.getRealName());
            approver.put("departmentName", getDepartmentName(partner.getDepartmentId()));
            approver.put("position", "团队负责人");
            approvers.add(approver);
            addedUserIds.add(partner.getId());
          }
        }
      }
    }

    // 3. 添加主任（DIRECTOR角色）
    List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
    if (directorIds != null) {
      for (Long directorId : directorIds) {
        if (!directorId.equals(applicantId) && !addedUserIds.contains(directorId)) {
          User director = userRepository.getById(directorId);
          if (director != null && EmployeeStatus.ACTIVE.equals(director.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", director.getId());
            approver.put("realName", director.getRealName());
            approver.put("departmentName", getDepartmentName(director.getDepartmentId()));
            approver.put("position", "主任");
            approvers.add(approver);
            addedUserIds.add(director.getId());
          }
        }
      }
    }

    return approvers;
  }

  /**
   * 获取部门名称.
   *
   * @param deptId 部门ID
   * @return 部门名称
   */
  private String getDepartmentName(final Long deptId) {
    if (deptId == null) {
      return "";
    }
    Department dept = departmentRepository.getById(deptId);
    return dept != null ? dept.getName() : "";
  }

  /**
   * 查找利冲检查审批人.
   *
   * <p>规则：由主任审批（利冲是重要事项） 如果没有主任，降级到团队负责人
   *
   * @return 审批人ID
   */
  public Long findConflictCheckApprover() {
    Long director = findApproverByRole("DIRECTOR");
    if (director != null) {
      return director;
    }

    return findApproverByRole("TEAM_LEADER");
  }

  /**
   * 查找数据交接审批人.
   *
   * <p>规则： - RESIGNATION（离职交接）：由主任审批（人员变动需最高级别审批） - PROJECT（项目移交）：由团队负责人或主任审批 -
   * CLIENT（客户移交）：由团队负责人或主任审批 - LEAD（案源移交）：由团队负责人或主任审批
   *
   * @param handoverType 交接类型
   * @return 审批人ID
   */
  public Long findDataHandoverApprover(final String handoverType) {
    if ("RESIGNATION".equals(handoverType)) {
      // 离职交接需要主任审批
      Long director = findApproverByRole("DIRECTOR");
      if (director != null) {
        return director;
      }
      // 如果没有主任，降级到团队负责人
      return findApproverByRole("TEAM_LEADER");
    }

    // 其他类型由团队负责人审批
    Long partner = findApproverByRole("TEAM_LEADER");
    if (partner != null) {
      return partner;
    }

    // 如果没有团队负责人，升级到主任
    Long director = findApproverByRole("DIRECTOR");
    if (director != null) {
      return director;
    }

    // 最后降级到管理员
    return findApproverByRole("ADMIN");
  }

  /**
   * 根据角色查找审批人.
   *
   * @param roleCode 角色代码
   * @return 审批人ID，如果未找到返回null
   */
  private Long findApproverByRole(final String roleCode) {
    List<Long> userIds = userMapper.selectUserIdsByRoleCode(roleCode);
    if (userIds != null && !userIds.isEmpty()) {
      return userIds.get(0);
    }

    log.warn("未找到角色为 {} 的审批人", roleCode);
    return null;
  }

  /**
   * 查找默认审批人（管理员）.
   *
   * @return 审批人ID
   * @throws BusinessException 如果系统中没有可用的审批人
   */
  public Long findDefaultApprover() {
    Long admin = findApproverByRole("ADMIN");
    if (admin != null) {
      return admin;
    }

    // 如果找不到管理员，返回第一个用户（通常是超级管理员）
    List<User> users = userRepository.list();
    if (users != null && !users.isEmpty()) {
      return users.get(0).getId();
    }

    throw new BusinessException("系统中没有可用的审批人");
  }

  /**
   * 查找项目结案审批人.
   *
   * <p>规则：由团队负责人（TEAM_LEADER）审批 如果没有团队负责人，升级到主任
   *
   * @return 审批人ID
   */
  public Long findMatterCloseApprover() {
    Long partner = findApproverByRole("TEAM_LEADER");
    if (partner != null) {
      return partner;
    }

    // 如果没有团队负责人，升级到主任
    Long director = findApproverByRole("DIRECTOR");
    if (director != null) {
      return director;
    }

    // 最后降级到管理员
    return findApproverByRole("ADMIN");
  }

  /**
   * 获取项目结案可选审批人列表 包括：团队负责人（TEAM_LEADER角色）优先
   *
   * @return 可选审批人列表
   */
  public List<Map<String, Object>> getMatterCloseAvailableApprovers() {
    Long currentUserId = SecurityUtils.getUserId();
    List<Map<String, Object>> approvers = new ArrayList<>();
    Set<Long> addedUserIds = new HashSet<>();

    // 1. 添加所有团队负责人（TEAM_LEADER角色）- 优先显示
    List<Long> teamLeaderIds = userMapper.selectUserIdsByRoleCode("TEAM_LEADER");
    if (teamLeaderIds != null) {
      for (Long leaderId : teamLeaderIds) {
        if (!leaderId.equals(currentUserId) && !addedUserIds.contains(leaderId)) {
          User leader = userRepository.getById(leaderId);
          if (leader != null && EmployeeStatus.ACTIVE.equals(leader.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", leader.getId());
            approver.put("realName", leader.getRealName());
            approver.put("departmentName", getDepartmentName(leader.getDepartmentId()));
            approver.put("position", "团队负责人");
            approver.put("recommended", true); // 标记为推荐
            approvers.add(approver);
            addedUserIds.add(leader.getId());
          }
        }
      }
    }

    // 2. 添加所有主任（DIRECTOR角色）
    List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
    if (directorIds != null) {
      for (Long directorId : directorIds) {
        if (!directorId.equals(currentUserId) && !addedUserIds.contains(directorId)) {
          User director = userRepository.getById(directorId);
          if (director != null && EmployeeStatus.ACTIVE.equals(director.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", director.getId());
            approver.put("realName", director.getRealName());
            approver.put("departmentName", getDepartmentName(director.getDepartmentId()));
            approver.put("position", "主任");
            approver.put("recommended", false);
            approvers.add(approver);
            addedUserIds.add(director.getId());
          }
        }
      }
    }

    return approvers;
  }

  /**
   * 查找档案入库审批人.
   *
   * <p>规则：由主任（DIRECTOR）审批 如果没有主任，降级到团队负责人
   *
   * @return 审批人ID
   */
  public Long findArchiveStoreApprover() {
    Long director = findApproverByRole("DIRECTOR");
    if (director != null) {
      return director;
    }

    // 如果没有主任，降级到团队负责人
    Long partner = findApproverByRole("TEAM_LEADER");
    if (partner != null) {
      return partner;
    }

    // 最后降级到管理员
    return findApproverByRole("ADMIN");
  }

  /**
   * 获取档案入库可选审批人列表 包括：主任（DIRECTOR角色）优先
   *
   * @return 可选审批人列表
   */
  public List<Map<String, Object>> getArchiveStoreAvailableApprovers() {
    Long currentUserId = SecurityUtils.getUserId();
    List<Map<String, Object>> approvers = new ArrayList<>();
    Set<Long> addedUserIds = new HashSet<>();

    // 1. 添加所有主任（DIRECTOR角色）- 优先显示
    List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
    if (directorIds != null) {
      for (Long directorId : directorIds) {
        if (!directorId.equals(currentUserId) && !addedUserIds.contains(directorId)) {
          User director = userRepository.getById(directorId);
          if (director != null && EmployeeStatus.ACTIVE.equals(director.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", director.getId());
            approver.put("realName", director.getRealName());
            approver.put("departmentName", getDepartmentName(director.getDepartmentId()));
            approver.put("position", "主任");
            approver.put("recommended", true); // 标记为推荐
            approvers.add(approver);
            addedUserIds.add(director.getId());
          }
        }
      }
    }

    // 2. 添加所有团队负责人（TEAM_LEADER角色）
    List<Long> teamLeaderIds = userMapper.selectUserIdsByRoleCode("TEAM_LEADER");
    if (teamLeaderIds != null) {
      for (Long leaderId : teamLeaderIds) {
        if (!leaderId.equals(currentUserId) && !addedUserIds.contains(leaderId)) {
          User leader = userRepository.getById(leaderId);
          if (leader != null && EmployeeStatus.ACTIVE.equals(leader.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", leader.getId());
            approver.put("realName", leader.getRealName());
            approver.put("departmentName", getDepartmentName(leader.getDepartmentId()));
            approver.put("position", "团队负责人");
            approver.put("recommended", false);
            approvers.add(approver);
            addedUserIds.add(leader.getId());
          }
        }
      }
    }

    return approvers;
  }
}
