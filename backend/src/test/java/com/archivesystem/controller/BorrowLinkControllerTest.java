package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.borrow.BorrowLinkResponse;
import com.archivesystem.entity.BorrowLink;
import com.archivesystem.service.BorrowLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BorrowLinkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BorrowLinkService borrowLinkService;

    @InjectMocks
    private BorrowLinkController borrowLinkController;

    private BorrowLinkResponse testLink;
    private BorrowLinkResponse detailLink;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(borrowLinkController).build();

        testLink = BorrowLinkResponse.builder()
                .id(1L)
                .archiveId(100L)
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .expireAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        detailLink = BorrowLinkResponse.builder()
                .id(1L)
                .archiveId(100L)
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .expireAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .accessUrl("/borrow/access/test-token-123")
                .build();
    }

    @Test
    void testGetList() throws Exception {
        when(borrowLinkService.getList(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 20, 1, Collections.singletonList(testLink)));

        mockMvc.perform(get("/borrow-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].borrowId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].revokeReason").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].revokedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].accessUrl").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].sourceSystem").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].sourceUserId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].revokedBy").doesNotExist());
    }

    @Test
    void testGetListWithFilters() throws Exception {
        when(borrowLinkService.getList(eq(100L), eq("ACTIVE"), eq(true), eq(null), eq(null), eq(1), eq(10)))
                .thenReturn(PageResult.of(1, 10, 1, Collections.singletonList(testLink)));

        mockMvc.perform(get("/borrow-links")
                        .param("archiveId", "100")
                        .param("status", "ACTIVE")
                        .param("allowDownload", "true")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById() throws Exception {
        when(borrowLinkService.getById(1L)).thenReturn(detailLink);

        mockMvc.perform(get("/borrow-links/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.accessUrl").value("/borrow/access/test-token-123"))
                .andExpect(jsonPath("$.data.sourceSystem").doesNotExist())
                .andExpect(jsonPath("$.data.sourceUserId").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.revokedBy").doesNotExist());
    }

    @Test
    void testGetActiveByArchive() throws Exception {
        List<BorrowLinkResponse> links = Arrays.asList(testLink);
        when(borrowLinkService.getActiveByArchiveId(100L)).thenReturn(links);

        mockMvc.perform(get("/borrow-links/archive/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].archiveId").value(100))
                .andExpect(jsonPath("$.data[0].borrowId").doesNotExist())
                .andExpect(jsonPath("$.data[0].revokeReason").doesNotExist())
                .andExpect(jsonPath("$.data[0].revokedAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].accessUrl").doesNotExist())
                .andExpect(jsonPath("$.data[0].sourceSystem").doesNotExist())
                .andExpect(jsonPath("$.data[0].sourceUserId").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].revokedBy").doesNotExist());
    }

    @Test
    void testGenerateLink() throws Exception {
        when(borrowLinkService.generateLinkForBorrow(eq(1L), eq(7), eq(true)))
                .thenReturn(detailLink);

        mockMvc.perform(post("/borrow-links/generate")
                        .param("borrowId", "1")
                        .param("expireDays", "7")
                        .param("allowDownload", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("链接生成成功"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.accessUrl").value("/borrow/access/test-token-123"))
                .andExpect(jsonPath("$.data.sourceSystem").doesNotExist())
                .andExpect(jsonPath("$.data.sourceUserId").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.revokedBy").doesNotExist());
    }

    @Test
    void testRevoke() throws Exception {
        doNothing().when(borrowLinkService).revoke(1L, "测试撤销", null);

        mockMvc.perform(post("/borrow-links/1/revoke")
                        .param("reason", "测试撤销"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value("链接已撤销"));

        verify(borrowLinkService).revoke(1L, "测试撤销", null);
    }

    @Test
    void testGetStats() throws Exception {
        BorrowLinkService.BorrowLinkStats stats = new BorrowLinkService.BorrowLinkStats(
                100L, 80L, 15L, 5L, 500L, 200L);
        when(borrowLinkService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/borrow-links/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalCount").value(100));
    }

    @Test
    void testUpdateExpired() throws Exception {
        when(borrowLinkService.updateExpiredLinks()).thenReturn(5);

        mockMvc.perform(post("/borrow-links/update-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.updatedCount").value(5))
                .andExpect(jsonPath("$.data.count").doesNotExist());
    }
}
