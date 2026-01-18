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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnnouncementAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementAppService 公告服务测试")
class AnnouncementAppServiceTest {

    private static final Long TEST_ANNOUNCEMENT_ID = 100L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementAppService announcementAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("查询公告测试")
    class QueryAnnouncementTests {

        @Test
        @DisplayName("应该成功分页查询公告")
        void listAnnouncements_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .status(Announcement.STATUS_PUBLISHED)
                    .build();

            @SuppressWarnings("unchecked")
            IPage<Announcement> page = mock(IPage.class);
            when(page.getRecords()).thenReturn(List.of(announcement));
            when(page.getTotal()).thenReturn(1L);

            when(announcementMapper.selectAnnouncementPage(any(Page.class), any(), any()))
                    .thenReturn(page);

            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<AnnouncementDTO> result = announcementAppService.listAnnouncements(query, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取有效公告")
        void getValidAnnouncements_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("有效公告")
                    .status(Announcement.STATUS_PUBLISHED)
                    .expireTime(LocalDateTime.now().plusDays(7))
                    .build();

            when(announcementMapper.selectValidAnnouncements(10)).thenReturn(List.of(announcement));

            // When
            List<AnnouncementDTO> result = announcementAppService.getValidAnnouncements(10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该限制最大查询数量")
        void getValidAnnouncements_shouldLimitMaxCount() {
            // Given
            when(announcementMapper.selectValidAnnouncements(100)).thenReturn(Collections.emptyList());

            // When
            announcementAppService.getValidAnnouncements(200);

            // Then
            verify(announcementMapper).selectValidAnnouncements(100);
        }

        @Test
        @DisplayName("应该成功获取公告详情")
        void getAnnouncementById_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .content("公告内容")
                    .status(Announcement.STATUS_PUBLISHED)
                    .build();

            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);

            // When
            AnnouncementDTO result = announcementAppService.getAnnouncementById(TEST_ANNOUNCEMENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("测试公告");
        }
    }

    @Nested
    @DisplayName("创建公告测试")
    class CreateAnnouncementTests {

        @Test
        @DisplayName("应该成功创建公告")
        void createAnnouncement_shouldSuccess() {
            // Given
            CreateAnnouncementCommand command = new CreateAnnouncementCommand();
            command.setTitle("新公告");
            command.setContent("公告内容");
            command.setType(Announcement.TYPE_NOTICE);
            command.setExpireTime(LocalDateTime.now().plusDays(7));

            when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
                Announcement announcement = invocation.getArgument(0);
                announcement.setId(TEST_ANNOUNCEMENT_ID);
                return true;
            });

            // When
            AnnouncementDTO result = announcementAppService.createAnnouncement(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("新公告");
            assertThat(result.getStatus()).isEqualTo(Announcement.STATUS_DRAFT);
            verify(announcementRepository).save(any(Announcement.class));
        }

        @Test
        @DisplayName("应该失败当标题为空")
        void createAnnouncement_shouldFail_whenTitleEmpty() {
            // Given
            CreateAnnouncementCommand command = new CreateAnnouncementCommand();
            command.setTitle("");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> announcementAppService.createAnnouncement(command));
            assertThat(exception.getMessage()).contains("公告标题不能为空");
        }

        @Test
        @DisplayName("应该失败当过期时间早于当前时间")
        void createAnnouncement_shouldFail_whenExpireTimeBeforeNow() {
            // Given
            CreateAnnouncementCommand command = new CreateAnnouncementCommand();
            command.setTitle("新公告");
            command.setExpireTime(LocalDateTime.now().minusDays(1));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> announcementAppService.createAnnouncement(command));
            assertThat(exception.getMessage()).contains("过期时间不能早于当前时间");
        }
    }

    @Nested
    @DisplayName("更新公告测试")
    class UpdateAnnouncementTests {

        @Test
        @DisplayName("应该成功更新草稿公告")
        void updateAnnouncement_shouldSuccess_whenDraft() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("原标题")
                    .status(Announcement.STATUS_DRAFT)
                    .build();

            CreateAnnouncementCommand command = new CreateAnnouncementCommand();
            command.setTitle("新标题");
            command.setContent("新内容");

            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);
            when(announcementRepository.updateById(any(Announcement.class))).thenReturn(true);

            // When
            AnnouncementDTO result = announcementAppService.updateAnnouncement(TEST_ANNOUNCEMENT_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(announcement.getTitle()).isEqualTo("新标题");
            verify(announcementRepository).updateById(announcement);
        }

        @Test
        @DisplayName("已发布公告只能修改过期时间和置顶状态")
        void updateAnnouncement_shouldLimitFields_whenPublished() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("原标题")
                    .status(Announcement.STATUS_PUBLISHED)
                    .build();

            CreateAnnouncementCommand command = new CreateAnnouncementCommand();
            command.setTitle("新标题");
            command.setExpireTime(LocalDateTime.now().plusDays(7));
            command.setIsTop(true);

            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);
            when(announcementRepository.updateById(any(Announcement.class))).thenReturn(true);

            // When
            announcementAppService.updateAnnouncement(TEST_ANNOUNCEMENT_ID, command);

            // Then
            assertThat(announcement.getTitle()).isEqualTo("原标题"); // 标题未改变
            assertThat(announcement.getExpireTime()).isNotNull(); // 过期时间已更新
            verify(announcementRepository).updateById(announcement);
        }
    }

    @Nested
    @DisplayName("发布公告测试")
    class PublishAnnouncementTests {

        @Test
        @DisplayName("应该成功发布公告")
        void publishAnnouncement_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .status(Announcement.STATUS_DRAFT)
                    .expireTime(LocalDateTime.now().plusDays(7))
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);
            when(announcementRepository.updateById(any(Announcement.class))).thenReturn(true);

            // When
            AnnouncementDTO result = announcementAppService.publishAnnouncement(TEST_ANNOUNCEMENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(announcement.getStatus()).isEqualTo(Announcement.STATUS_PUBLISHED);
            assertThat(announcement.getPublishTime()).isNotNull();
            verify(announcementRepository).updateById(announcement);
        }

        @Test
        @DisplayName("应该失败当无权限")
        void publishAnnouncement_shouldFail_whenNoPermission() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasRole("ANNOUNCEMENT_MANAGER")).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> announcementAppService.publishAnnouncement(TEST_ANNOUNCEMENT_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }

        @Test
        @DisplayName("应该失败当公告已过期")
        void publishAnnouncement_shouldFail_whenExpired() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .status(Announcement.STATUS_DRAFT)
                    .expireTime(LocalDateTime.now().minusDays(1))
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> announcementAppService.publishAnnouncement(TEST_ANNOUNCEMENT_ID));
            assertThat(exception.getMessage()).contains("已过期");
        }
    }

    @Nested
    @DisplayName("撤回公告测试")
    class WithdrawAnnouncementTests {

        @Test
        @DisplayName("应该成功撤回公告")
        void withdrawAnnouncement_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .status(Announcement.STATUS_PUBLISHED)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);
            when(announcementRepository.updateById(any(Announcement.class))).thenReturn(true);

            // When
            announcementAppService.withdrawAnnouncement(TEST_ANNOUNCEMENT_ID);

            // Then
            assertThat(announcement.getStatus()).isEqualTo(Announcement.STATUS_DRAFT);
            verify(announcementRepository).updateById(announcement);
        }
    }

    @Nested
    @DisplayName("删除公告测试")
    class DeleteAnnouncementTests {

        @Test
        @DisplayName("应该成功删除公告（软删除）")
        void deleteAnnouncement_shouldSuccess() {
            // Given
            Announcement announcement = Announcement.builder()
                    .id(TEST_ANNOUNCEMENT_ID)
                    .title("测试公告")
                    .status(Announcement.STATUS_PUBLISHED)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
            when(announcementRepository.getByIdOrThrow(eq(TEST_ANNOUNCEMENT_ID), anyString()))
                    .thenReturn(announcement);
            when(announcementRepository.updateById(any(Announcement.class))).thenReturn(true);

            // When
            announcementAppService.deleteAnnouncement(TEST_ANNOUNCEMENT_ID);

            // Then
            assertThat(announcement.getStatus()).isEqualTo(Announcement.STATUS_EXPIRED);
            verify(announcementRepository).updateById(announcement);
        }
    }
}
