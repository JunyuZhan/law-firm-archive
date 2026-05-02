package com.archivesystem.controller;

import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import com.archivesystem.dto.archive.ArchiveReceiveResponse;
import com.archivesystem.dto.borrow.BorrowLinkAccessResponse;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.BorrowLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class OpenApiControllerTest {

    private static final ExternalSource TEST_EXTERNAL_SOURCE = ExternalSource.builder()
            .sourceCode("LAW_FIRM_A")
            .sourceName("律所A")
            .sourceType(ExternalSource.TYPE_LAW_FIRM)
            .enabled(true)
            .build();

    private MockMvc mockMvc;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private BorrowLinkService borrowLinkService;

    @InjectMocks
    private OpenApiController openApiController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(openApiController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testReceive() throws Exception {
        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();
        fileInfo.setFileName("contract.pdf");
        fileInfo.setDownloadUrl("http://example.com/files/contract.pdf");
        fileInfo.setFileType("application/pdf");
        fileInfo.setFileCategory("CONTRACT");

        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("CASE-001");
        request.setArchiveType("CASE_FILE");
        request.setTitle("合同档案-2026");
        request.setRetentionPeriod("PERMANENT");
        request.setFiles(Arrays.asList(fileInfo));

        ArchiveReceiveResponse response = new ArchiveReceiveResponse();
        response.setArchiveId(1L);
        response.setArchiveNo("ARC-20260213-0001");
        response.setStatus("PROCESSING");
        response.setMessage("档案接收成功，正在处理中");
        response.setReceivedAt(LocalDateTime.now());

        when(archiveService.receive(any(ArchiveReceiveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/open/archive/receive")
                        .requestAttr("externalSource", TEST_EXTERNAL_SOURCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("档案接收成功"))
                .andExpect(jsonPath("$.data.archiveId").value(1))
                .andExpect(jsonPath("$.data.archiveNo").value("ARC-20260213-0001"));

        verify(archiveService).receive(argThat(arg ->
                "LAW_FIRM_A".equals(arg.getSourceType())
                        && "CASE-001".equals(arg.getSourceId())));
    }

    @Test
    void testReceive_WithoutFiles() throws Exception {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("CASE-002");
        request.setArchiveType("CASE_FILE");
        request.setTitle("无附件档案");
        request.setRetentionPeriod("30_YEARS");

        ArchiveReceiveResponse response = new ArchiveReceiveResponse();
        response.setArchiveId(2L);
        response.setArchiveNo("ARC-20260213-0002");
        response.setStatus("RECEIVED");
        response.setReceivedAt(LocalDateTime.now());

        when(archiveService.receive(any(ArchiveReceiveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/open/archive/receive")
                        .requestAttr("externalSource", TEST_EXTERNAL_SOURCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveId").value(2));
    }

    @Test
    void testReceive_MissingExternalSourceContext() throws Exception {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("CASE-003");
        request.setArchiveType("CASE_FILE");
        request.setTitle("缺少认证上下文");
        request.setRetentionPeriod("30_YEARS");

        mockMvc.perform(post("/open/archive/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("401"));

        verify(archiveService, never()).receive(any(ArchiveReceiveRequest.class));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/open/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void testApplyBorrow() throws Exception {
        BorrowLinkApplyRequest request = new BorrowLinkApplyRequest();
        request.setArchiveId(1L);
        request.setUserId("u001");
        request.setUserName("测试用户");
        request.setPurpose("查阅");

        BorrowLinkApplyResponse response = new BorrowLinkApplyResponse();
        response.setLinkId(10L);
        response.setAccessUrl("https://example.com/open/borrow/access/token1234");
        response.setExpireAt(LocalDateTime.now().plusDays(7));

        when(borrowLinkService.applyLink(any(BorrowLinkApplyRequest.class), eq("LAW_FIRM_A"))).thenReturn(response);

        mockMvc.perform(post("/open/borrow/apply")
                        .requestAttr("externalSource", TEST_EXTERNAL_SOURCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("借阅链接生成成功"))
                .andExpect(jsonPath("$.data.linkId").value(10))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    void testRevokeBorrow() throws Exception {
        mockMvc.perform(post("/open/borrow/revoke/10")
                        .requestAttr("externalSource", TEST_EXTERNAL_SOURCE)
                        .param("reason", "管理系统撤销"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("借阅链接已撤销"));

        verify(borrowLinkService).revoke(10L, "管理系统撤销", "LAW_FIRM_A");
    }

    @Test
    void testAccessArchive_InvalidLink() throws Exception {
        when(borrowLinkService.validateAndAccess(eq("token12345678"), eq("10.0.0.1")))
                .thenReturn(BorrowLinkAccessResponse.invalid("链接已过期"));

        mockMvc.perform(get("/open/borrow/access/token12345678")
                        .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("链接已过期"));
    }

    @Test
    void testAccessArchive_ValidLink() throws Exception {
        BorrowLinkAccessResponse response = BorrowLinkAccessResponse.builder()
                .valid(true)
                .archive(BorrowLinkAccessResponse.ArchiveInfo.builder()
                        .archiveNo("ARC-20260213-0001")
                        .title("测试档案")
                        .build())
                .files(java.util.List.of(BorrowLinkAccessResponse.FileInfo.builder()
                        .fileId(9L)
                        .fileName("卷宗.pdf")
                        .fileExtension("pdf")
                        .fileSize(1024L)
                        .isLongTermFormat(true)
                        .build()))
                .linkInfo(BorrowLinkAccessResponse.LinkInfo.builder()
                        .accessCount(1)
                        .allowDownload(true)
                        .build())
                .borrower(BorrowLinkAccessResponse.BorrowerInfo.builder()
                        .userName("张三")
                        .purpose("阅卷")
                        .build())
                .build();

        when(borrowLinkService.validateAndAccess(eq("token12345678"), eq("127.0.0.1")))
                .thenReturn(response);

        mockMvc.perform(get("/open/borrow/access/token12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archive.archiveNo").value("ARC-20260213-0001"))
                .andExpect(jsonPath("$.data.archive.archiveId").doesNotExist())
                .andExpect(jsonPath("$.data.archive.retentionPeriod").doesNotExist())
                .andExpect(jsonPath("$.data.linkInfo.linkId").doesNotExist())
                .andExpect(jsonPath("$.data.files[0].fileName").value("卷宗.pdf"))
                .andExpect(jsonPath("$.data.files[0].mimeType").doesNotExist())
                .andExpect(jsonPath("$.data.files[0].fileCategory").doesNotExist())
                .andExpect(jsonPath("$.data.files[0].previewUrl").doesNotExist())
                .andExpect(jsonPath("$.data.files[0].downloadUrl").doesNotExist())
                .andExpect(jsonPath("$.data.borrower.userName").value("张三"))
                .andExpect(jsonPath("$.data.borrower.userId").doesNotExist())
                .andExpect(jsonPath("$.data.borrower.sourceSystem").doesNotExist());
    }

    @Test
    void testGetPreviewUrl() throws Exception {
        when(borrowLinkService.getFileAccessUrl("token12345678", 9L, false, "127.0.0.1"))
                .thenReturn("preview-url");

        mockMvc.perform(get("/open/borrow/access/token12345678/preview/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").value("preview-url"));
    }

    @Test
    void testGetPreviewUrl_ShouldHideInternalFailureDetails() throws Exception {
        when(borrowLinkService.getFileAccessUrl("token12345678", 9L, false, "127.0.0.1"))
                .thenThrow(NotFoundException.of("文件", 9L));

        mockMvc.perform(get("/open/borrow/access/token12345678/preview/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("访问链接无效、已过期或无权访问该文件"));
    }

    @Test
    void testGetDownloadUrl() throws Exception {
        when(borrowLinkService.getFileAccessUrl("token12345678", 9L, true, "127.0.0.1"))
                .thenReturn("download-url");

        mockMvc.perform(get("/open/borrow/access/token12345678/download-url/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").value("download-url"));
    }

    @Test
    void testGetDownloadUrl_ShouldHideInternalFailureDetails() throws Exception {
        when(borrowLinkService.getFileAccessUrl("token12345678", 9L, true, "127.0.0.1"))
                .thenThrow(new BusinessException("403", "该链接不允许下载"));

        mockMvc.perform(get("/open/borrow/access/token12345678/download-url/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("访问链接无效、已过期或无权访问该文件"));
    }

    @Test
    void testRecordDownload() throws Exception {
        mockMvc.perform(post("/open/borrow/access/token12345678/download/9")
                        .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(borrowLinkService).recordDownload("token12345678", 9L, "10.0.0.1");
    }

    @Test
    void testRecordDownload_ShouldHideInternalFailureDetails() throws Exception {
        doThrow(NotFoundException.of("文件", 9L))
                .when(borrowLinkService).recordDownload("token12345678", 9L, "127.0.0.1");

        mockMvc.perform(post("/open/borrow/access/token12345678/download/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("访问链接无效、已过期或无权访问该文件"));
    }

    @Test
    void testApplyBorrow_MissingExternalSourceContext() throws Exception {
        BorrowLinkApplyRequest request = new BorrowLinkApplyRequest();
        request.setArchiveId(1L);
        request.setUserId("u001");
        request.setUserName("测试用户");
        request.setPurpose("查阅");

        mockMvc.perform(post("/open/borrow/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("401"));

        verify(borrowLinkService, never()).applyLink(any(), any());
    }
}
