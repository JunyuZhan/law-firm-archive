package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateAnnouncementCommand;
import com.lawfirm.application.system.dto.AnnouncementDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.system.entity.Announcement;
import com.lawfirm.domain.system.repository.AnnouncementRepository;
import com.lawfirm.infrastructure.persistence.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统公告应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementAppService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    /**
     * 分页查询公告
     */
    public PageResult<AnnouncementDTO> listAnnouncements(PageQuery query, String status, String type) {
        Integer pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        Integer pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        
        IPage<Announcement> page = announcementMapper.selectAnnouncementPage(
                new Page<>(pageNum, pageSize),
                status,
                type
        );

        List<AnnouncementDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取有效公告
     */
    public List<AnnouncementDTO> getValidAnnouncements(int limit) {
        return announcementMapper.selectValidAnnouncements(limit).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取公告详情
     */
    public AnnouncementDTO getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
        return toDTO(announcement);
    }

    /**
     * 创建公告
     */
    @Transactional
    public AnnouncementDTO createAnnouncement(CreateAnnouncementCommand command) {
        if (!StringUtils.hasText(command.getTitle())) {
            throw new BusinessException("公告标题不能为空");
        }

        Announcement announcement = Announcement.builder()
                .title(command.getTitle())
                .content(command.getContent())
                .type(command.getType() != null ? command.getType() : Announcement.TYPE_NOTICE)
                .priority(command.getPriority() != null ? command.getPriority() : 0)
                .status(Announcement.STATUS_DRAFT)
                .expireTime(command.getExpireTime())
                .isTop(command.getIsTop() != null ? command.getIsTop() : false)
                .build();

        announcementRepository.save(announcement);
        log.info("公告创建成功: {}", announcement.getTitle());
        return toDTO(announcement);
    }

    /**
     * 更新公告
     */
    @Transactional
    public AnnouncementDTO updateAnnouncement(Long id, CreateAnnouncementCommand command) {
        Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");

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
            announcement.setExpireTime(command.getExpireTime());
        }
        if (command.getIsTop() != null) {
            announcement.setIsTop(command.getIsTop());
        }

        announcementRepository.updateById(announcement);
        log.info("公告更新成功: {}", announcement.getTitle());
        return toDTO(announcement);
    }

    /**
     * 发布公告
     */
    @Transactional
    public AnnouncementDTO publishAnnouncement(Long id) {
        Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
        
        if (Announcement.STATUS_PUBLISHED.equals(announcement.getStatus())) {
            throw new BusinessException("公告已发布");
        }

        announcement.setStatus(Announcement.STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcementRepository.updateById(announcement);
        log.info("公告发布成功: {}", announcement.getTitle());
        return toDTO(announcement);
    }

    /**
     * 撤回公告
     */
    @Transactional
    public void withdrawAnnouncement(Long id) {
        Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
        announcement.setStatus(Announcement.STATUS_DRAFT);
        announcementRepository.updateById(announcement);
        log.info("公告撤回成功: {}", announcement.getTitle());
    }

    /**
     * 删除公告
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.getByIdOrThrow(id, "公告不存在");
        announcementMapper.deleteById(id);
        log.info("公告删除成功: {}", announcement.getTitle());
    }

    private String getTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case Announcement.TYPE_NOTICE -> "通知";
            case Announcement.TYPE_ANNOUNCEMENT -> "公告";
            case Announcement.TYPE_WARNING -> "警告";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case Announcement.STATUS_DRAFT -> "草稿";
            case Announcement.STATUS_PUBLISHED -> "已发布";
            case Announcement.STATUS_EXPIRED -> "已过期";
            default -> status;
        };
    }

    private AnnouncementDTO toDTO(Announcement announcement) {
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
