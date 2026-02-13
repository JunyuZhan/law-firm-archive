package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateAnnouncementCommand;
import com.lawfirm.application.system.dto.AnnouncementDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Announcement;
import com.lawfirm.domain.system.repository.AnnouncementRepository;
import com.lawfirm.infrastructure.persistence.mapper.AnnouncementMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 系统公告应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementAppService {

  /** 公告仓储 */
  private final AnnouncementRepository announcementRepository;

  /** 公告Mapper */
  private final AnnouncementMapper announcementMapper;

  /**
   * 分页查询公告
   *
   * @param query 分页查询参数
   * @param status 状态
   * @param type 类型
   * @return 分页结果
   */
  public PageResult<AnnouncementDTO> listAnnouncements(
      final PageQuery query, final String status, final String type) {
    Integer pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
    Integer pageSize = query.getPageSize() != null ? query.getPageSize() : 10;

    IPage<Announcement> page =
        announcementMapper.selectAnnouncementPage(new Page<>(pageNum, pageSize), status, type);

    List<AnnouncementDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), pageNum, pageSize);
  }

  /** 获取有效公告 问题499修复：验证limit参数最大值 */
  private static final int MAX_ANNOUNCEMENT_LIMIT = 100;

  /**
   * 获取有效公告
   *
   * @param limit 限制数量
   * @return 公告列表
   */
  public List<AnnouncementDTO> getValidAnnouncements(final int limit) {
    // 问题499修复：验证并限制最大值
    int safeLimit = Math.min(Math.max(limit, 1), MAX_ANNOUNCEMENT_LIMIT);

    return announcementMapper.selectValidAnnouncements(safeLimit).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取公告详情
   *
   * @param id 公告ID
   * @return 公告DTO
   */
  public AnnouncementDTO getAnnouncementById(final Long id) {
    Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
    return toDTO(announcement);
  }

  /**
   * 创建公告 问题503修复：验证过期时间不能在当前时间之前
   *
   * @param command 创建命令
   * @return 公告DTO
   */
  @Transactional
  public AnnouncementDTO createAnnouncement(final CreateAnnouncementCommand command) {
    if (!StringUtils.hasText(command.getTitle())) {
      throw new BusinessException("公告标题不能为空");
    }

    // 问题503修复：验证过期时间
    LocalDateTime expireTime = command.getExpireTime();
    if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
      throw new BusinessException("公告过期时间不能早于当前时间");
    }

    Announcement announcement =
        Announcement.builder()
            .title(command.getTitle())
            .content(command.getContent())
            .type(command.getType() != null ? command.getType() : Announcement.TYPE_NOTICE)
            .priority(command.getPriority() != null ? command.getPriority() : 0)
            .status(Announcement.STATUS_DRAFT)
            .expireTime(expireTime)
            .isTop(command.getIsTop() != null ? command.getIsTop() : false)
            .build();

    announcementRepository.save(announcement);
    log.info("公告创建成功: {}", announcement.getTitle());
    return toDTO(announcement);
  }

  /**
   * 更新公告 问题500修复：限制已发布公告修改
   *
   * @param id 公告ID
   * @param command 更新命令
   * @return 公告DTO
   */
  @Transactional
  public AnnouncementDTO updateAnnouncement(
      final Long id, final CreateAnnouncementCommand command) {
    Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");

    // 问题500修复：已发布的公告只允许修改过期时间和置顶状态
    if (Announcement.STATUS_PUBLISHED.equals(announcement.getStatus())) {
      if (command.getExpireTime() != null) {
        // 问题503修复：验证过期时间
        if (command.getExpireTime().isBefore(LocalDateTime.now())) {
          throw new BusinessException("公告过期时间不能早于当前时间");
        }
        announcement.setExpireTime(command.getExpireTime());
      }
      if (command.getIsTop() != null) {
        announcement.setIsTop(command.getIsTop());
      }
      log.warn("修改已发布公告: id={}, 仅允许修改过期时间和置顶状态", id);
    } else {
      // 草稿状态可以修改所有字段
      if (StringUtils.hasText(command.getTitle())) {
        announcement.setTitle(command.getTitle());
      }
      if (command.getContent() != null) {
        announcement.setContent(command.getContent());
      }
      if (command.getType() != null) {
        announcement.setType(command.getType());
      }
      if (command.getPriority() != null) {
        announcement.setPriority(command.getPriority());
      }
      if (command.getExpireTime() != null) {
        // 问题503修复：验证过期时间
        if (command.getExpireTime().isBefore(LocalDateTime.now())) {
          throw new BusinessException("公告过期时间不能早于当前时间");
        }
        announcement.setExpireTime(command.getExpireTime());
      }
      if (command.getIsTop() != null) {
        announcement.setIsTop(command.getIsTop());
      }
    }

    announcementRepository.updateById(announcement);
    log.info("公告更新成功: {}", announcement.getTitle());
    return toDTO(announcement);
  }

  /**
   * 发布公告 问题492修复：添加权限验证和过期时间检查
   *
   * @param id 公告ID
   * @return 公告DTO
   */
  @Transactional
  public AnnouncementDTO publishAnnouncement(final Long id) {
    // 问题492修复：验证权限
    if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("ANNOUNCEMENT_MANAGER")) {
      throw new BusinessException("权限不足：只有管理员才能发布公告");
    }

    Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");

    if (Announcement.STATUS_PUBLISHED.equals(announcement.getStatus())) {
      throw new BusinessException("公告已发布");
    }

    // 问题492修复：验证过期时间
    if (announcement.getExpireTime() != null
        && announcement.getExpireTime().isBefore(LocalDateTime.now())) {
      throw new BusinessException("公告已过期，无法发布");
    }

    announcement.setStatus(Announcement.STATUS_PUBLISHED);
    announcement.setPublishTime(LocalDateTime.now());
    announcementRepository.updateById(announcement);

    log.info(
        "公告发布成功: id={}, title={}, publisher={}",
        id,
        announcement.getTitle(),
        SecurityUtils.getUserId());
    return toDTO(announcement);
  }

  /**
   * 撤回公告 问题493修复：添加权限验证
   *
   * @param id 公告ID
   */
  @Transactional
  public void withdrawAnnouncement(final Long id) {
    // 问题493修复：验证权限
    if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("ANNOUNCEMENT_MANAGER")) {
      throw new BusinessException("权限不足：只有管理员才能撤回公告");
    }

    Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
    announcement.setStatus(Announcement.STATUS_DRAFT);
    announcementRepository.updateById(announcement);

    log.info(
        "公告撤回成功: id={}, title={}, operator={}",
        id,
        announcement.getTitle(),
        SecurityUtils.getUserId());
  }

  /**
   * 删除公告 问题494修复：使用软删除替代物理删除
   *
   * @param id 公告ID
   */
  @Transactional
  public void deleteAnnouncement(final Long id) {
    // 问题494修复：验证权限
    if (!SecurityUtils.hasRole("ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能删除公告");
    }

    Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");

    // 问题494修复：软删除（改为过期状态）
    announcement.setStatus(Announcement.STATUS_EXPIRED);
    announcementRepository.updateById(announcement);

    log.info(
        "公告已归档: id={}, title={}, operator={}",
        id,
        announcement.getTitle(),
        SecurityUtils.getUserId());
  }

  /**
   * 获取类型名称
   *
   * @param type 类型代码
   * @return 类型名称
   */
  private String getTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case Announcement.TYPE_NOTICE -> "通知";
      case Announcement.TYPE_ANNOUNCEMENT -> "公告";
      case Announcement.TYPE_WARNING -> "警告";
      default -> type;
    };
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case Announcement.STATUS_DRAFT -> "草稿";
      case Announcement.STATUS_PUBLISHED -> "已发布";
      case Announcement.STATUS_EXPIRED -> "已过期";
      default -> status;
    };
  }

  /**
   * 转换为DTO
   *
   * @param announcement 公告实体
   * @return 公告DTO
   */
  private AnnouncementDTO toDTO(final Announcement announcement) {
    AnnouncementDTO dto = new AnnouncementDTO();
    dto.setId(announcement.getId());
    dto.setTitle(announcement.getTitle());
    dto.setContent(announcement.getContent());
    dto.setType(announcement.getType());
    dto.setTypeName(getTypeName(announcement.getType()));
    dto.setPriority(announcement.getPriority());
    dto.setStatus(announcement.getStatus());
    dto.setStatusName(getStatusName(announcement.getStatus()));
    dto.setPublishTime(announcement.getPublishTime());
    dto.setExpireTime(announcement.getExpireTime());
    dto.setIsTop(announcement.getIsTop());
    dto.setCreatedAt(announcement.getCreatedAt());
    return dto;
  }
}
