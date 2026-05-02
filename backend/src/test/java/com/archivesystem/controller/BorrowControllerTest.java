package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.archive.ArchiveDTO;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.BorrowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class BorrowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BorrowService borrowService;

    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private BorrowController borrowController;

    private ObjectMapper objectMapper;
    private BorrowApplication testApplication;
    private ArchiveDTO testArchive;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(borrowController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testApplication = new BorrowApplication();
        testApplication.setId(1L);
        testApplication.setArchiveId(1L);
        testApplication.setApplicantId(1L);
        testApplication.setApplicantName("测试用户");
        testApplication.setApplicantDept("诉讼部");
        testApplication.setBorrowPurpose("测试借阅");
        testApplication.setBorrowType(BorrowApplication.TYPE_ONLINE);
        testApplication.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testApplication.setStatus(BorrowApplication.STATUS_PENDING);
        testApplication.setCreatedAt(LocalDateTime.now());
        testApplication.setApplicantPhone("13800138000");
        testApplication.setApplicantId(101L);
        testApplication.setApproverId(202L);
        testApplication.setDeleted(false);

        testArchive = ArchiveDTO.builder()
                .id(1L)
                .status("STORED")
                .archiveForm("HYBRID")
                .hasElectronic(true)
                .hasPhysical(true)
                .securityLevel("INTERNAL")
                .build();
    }

    @Test
    void testApply_Success() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("archiveId", 1L);
        params.put("borrowPurpose", "测试借阅");
        params.put("borrowType", "ONLINE");
        params.put("expectedReturnDate", LocalDate.now().plusDays(7).toString());
        params.put("remarks", "测试备注");

        when(borrowService.apply(anyLong(), anyString(), anyString(), any(LocalDate.class), anyString()))
                .thenReturn(testApplication);

        mockMvc.perform(post("/borrows/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("申请提交成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.borrowType").value(BorrowApplication.TYPE_ONLINE))
                .andExpect(jsonPath("$.data.status").value(BorrowApplication.STATUS_PENDING))
                .andExpect(jsonPath("$.data.borrowPurpose").doesNotExist())
                .andExpect(jsonPath("$.data.approveRemarks").doesNotExist())
                .andExpect(jsonPath("$.data.remarks").doesNotExist())
                .andExpect(jsonPath("$.data.applicantPhone").doesNotExist());
    }

    @Test
    void testGetById_Success() throws Exception {
        when(borrowService.getById(1L)).thenReturn(testApplication);

        mockMvc.perform(get("/borrows/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.borrowPurpose").value("测试借阅"))
                .andExpect(jsonPath("$.data.applicantPhone").doesNotExist())
                .andExpect(jsonPath("$.data.applicantId").doesNotExist())
                .andExpect(jsonPath("$.data.approverId").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    @Test
    void testGetMyApplications_Success() throws Exception {
        PageResult<BorrowApplication> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testApplication));

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(borrowService.getMyApplications(1L, null, null, null, 1, 20)).thenReturn(pageResult);

            mockMvc.perform(get("/borrows/my")
                            .param("pageNum", "1")
                            .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].borrowPurpose").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].approveRemarks").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].applicantPhone").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].deleted").doesNotExist());
        }
    }

    @Test
    void testGetMyApplications_WithStatus() throws Exception {
        PageResult<BorrowApplication> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testApplication));

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(borrowService.getMyApplications(1L, "PENDING", null, null, 1, 20)).thenReturn(pageResult);

            mockMvc.perform(get("/borrows/my")
                            .param("status", "PENDING")
                            .param("pageNum", "1")
                            .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }
    }

    @Test
    void testCancel_Success() throws Exception {
        doNothing().when(borrowService).cancel(1L);

        mockMvc.perform(put("/borrows/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("取消成功"));

        verify(borrowService).cancel(1L);
    }

    @Test
    void testGetPendingList_Success() throws Exception {
        PageResult<BorrowApplication> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testApplication));

        when(borrowService.getPendingList(null, null, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/borrows/pending")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].borrowPurpose").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].approveRemarks").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].applicantPhone").doesNotExist());
    }

    @Test
    void testApprove_Success() throws Exception {
        doNothing().when(borrowService).approve(1L, "同意");

        mockMvc.perform(put("/borrows/1/approve")
                        .param("remarks", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("审批通过"));

        verify(borrowService).approve(1L, "同意");
    }

    @Test
    void testApprove_WithoutRemarks() throws Exception {
        doNothing().when(borrowService).approve(1L, null);

        mockMvc.perform(put("/borrows/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(borrowService).approve(1L, null);
    }

    @Test
    void testReject_Success() throws Exception {
        doNothing().when(borrowService).reject(1L, "档案暂不可借");

        mockMvc.perform(put("/borrows/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"档案暂不可借\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已拒绝"));

        verify(borrowService).reject(1L, "档案暂不可借");
    }

    @Test
    void testLend_Success() throws Exception {
        doNothing().when(borrowService).lend(1L);

        mockMvc.perform(put("/borrows/1/lend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("借出成功"));

        verify(borrowService).lend(1L);
    }

    @Test
    void testReturnArchive_Success() throws Exception {
        doNothing().when(borrowService).returnArchive(1L, "归还完好");

        mockMvc.perform(put("/borrows/1/return")
                        .param("remarks", "归还完好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("归还成功"));

        verify(borrowService).returnArchive(1L, "归还完好");
    }

    @Test
    void testReturnArchive_WithoutRemarks() throws Exception {
        doNothing().when(borrowService).returnArchive(1L, null);

        mockMvc.perform(put("/borrows/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(borrowService).returnArchive(1L, null);
    }

    @Test
    void testRenew_Success() throws Exception {
        LocalDate newReturnDate = LocalDate.now().plusDays(14);
        doNothing().when(borrowService).renew(1L, newReturnDate);

        mockMvc.perform(put("/borrows/1/renew")
                        .param("newReturnDate", newReturnDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("续借成功"));

        verify(borrowService).renew(1L, newReturnDate);
    }

    @Test
    void testCheckAvailable_WithCurrentApplication_ShouldNotExposeApplicationDetails() throws Exception {
        BorrowApplication current = new BorrowApplication();
        current.setId(9L);
        current.setApplicationNo("BR-20260429-001");
        current.setStatus(BorrowApplication.STATUS_PENDING);
        current.setBorrowType(BorrowApplication.TYPE_ONLINE);
        current.setExpectedReturnDate(LocalDate.now().plusDays(7));
        current.setApplicantPhone("13800138000");
        current.setApproveRemarks("内部审批意见");
        current.setRejectReason("内部拒绝原因");
        current.setRemarks("申请备注");

        when(borrowService.getCurrentByArchiveId(1L)).thenReturn(current);
        when(archiveService.getById(1L)).thenReturn(testArchive);

        mockMvc.perform(get("/borrows/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.hasCurrentApplication").value(true))
                .andExpect(jsonPath("$.data.currentApplication").doesNotExist())
                .andExpect(jsonPath("$.data.unavailableReason").value("该档案已有进行中的借阅申请"))
                .andExpect(jsonPath("$.data.borrowRules.ruleSummary").exists());
    }

    @Test
    void testGetOverdueList_Success() throws Exception {
        testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
        testApplication.setExpectedReturnDate(LocalDate.now().minusDays(1)); // 已逾期

        when(borrowService.getOverdueList()).thenReturn(Arrays.asList(testApplication));

        mockMvc.perform(get("/borrows/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].borrowPurpose").doesNotExist())
                .andExpect(jsonPath("$.data[0].approveRemarks").doesNotExist())
                .andExpect(jsonPath("$.data[0].applicantPhone").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());
    }

    @Test
    void testGetOverdueList_Empty() throws Exception {
        when(borrowService.getOverdueList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/borrows/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testCheckAvailable_Available() throws Exception {
        when(borrowService.getCurrentByArchiveId(1L)).thenReturn(null);
        when(archiveService.getById(1L)).thenReturn(testArchive);

        mockMvc.perform(get("/borrows/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.allowedBorrowTypes[0]").value("ONLINE"))
                .andExpect(jsonPath("$.data.borrowRules.maxBorrowDays.ONLINE").value(30))
                .andExpect(jsonPath("$.data.hasCurrentApplication").value(false));
    }

    @Test
    void testCheckAvailable_NotAvailable() throws Exception {
        when(borrowService.getCurrentByArchiveId(1L)).thenReturn(testApplication);
        when(archiveService.getById(1L)).thenReturn(testArchive);

        mockMvc.perform(get("/borrows/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.hasCurrentApplication").value(true))
                .andExpect(jsonPath("$.data.currentApplication").doesNotExist())
                .andExpect(jsonPath("$.data.unavailableReason").value("该档案已有进行中的借阅申请"));
    }

    @Test
    void testCheckAvailable_ConfidentialArchiveDisablesDownload() throws Exception {
        testArchive.setSecurityLevel("CONFIDENTIAL");
        testArchive.setHasPhysical(false);
        testArchive.setArchiveForm("ELECTRONIC");
        when(borrowService.getCurrentByArchiveId(1L)).thenReturn(null);
        when(archiveService.getById(1L)).thenReturn(testArchive);

        mockMvc.perform(get("/borrows/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.allowedBorrowTypes.length()").value(1))
                .andExpect(jsonPath("$.data.allowedBorrowTypes[0]").value("ONLINE"))
                .andExpect(jsonPath("$.data.borrowRules.maxBorrowDays.ONLINE").value(7));
    }
}
