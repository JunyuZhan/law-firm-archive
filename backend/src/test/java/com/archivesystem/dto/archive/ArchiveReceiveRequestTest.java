package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveReceiveRequestTest {

    @Test
    void testDefaultValues() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();

        assertTrue(request.getAsync());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        LocalDate documentDate = LocalDate.of(2026, 1, 15);
        LocalDate caseCloseDate = LocalDate.of(2025, 12, 31);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        request.setSourceType("LAW_FIRM");
        request.setSourceId("SRC-001");
        request.setSourceNo("NO-001");
        request.setCallbackUrl("https://callback.example.com/notify");
        request.setAsync(false);
        request.setTitle("合同纠纷档案");
        request.setArchiveType("LITIGATION");
        request.setRetentionPeriod("PERMANENT");
        request.setResponsibility("李律师");
        request.setDocumentDate(documentDate);
        request.setSecurityLevel("NORMAL");
        request.setCaseNo("CASE-001");
        request.setCaseName("张三诉李四合同纠纷");
        request.setClientName("张三");
        request.setLawyerName("李律师");
        request.setCaseCloseDate(caseCloseDate);
        request.setMetadata(metadata);
        request.setKeywords("合同,纠纷,民事");
        request.setArchiveAbstract("这是案件摘要内容");
        request.setRemarks("备注信息");

        assertEquals("LAW_FIRM", request.getSourceType());
        assertEquals("SRC-001", request.getSourceId());
        assertEquals("NO-001", request.getSourceNo());
        assertEquals("https://callback.example.com/notify", request.getCallbackUrl());
        assertFalse(request.getAsync());
        assertEquals("合同纠纷档案", request.getTitle());
        assertEquals("LITIGATION", request.getArchiveType());
        assertEquals("PERMANENT", request.getRetentionPeriod());
        assertEquals("李律师", request.getResponsibility());
        assertEquals(documentDate, request.getDocumentDate());
        assertEquals("NORMAL", request.getSecurityLevel());
        assertEquals("CASE-001", request.getCaseNo());
        assertEquals("张三诉李四合同纠纷", request.getCaseName());
        assertEquals("张三", request.getClientName());
        assertEquals("李律师", request.getLawyerName());
        assertEquals(caseCloseDate, request.getCaseCloseDate());
        assertEquals(metadata, request.getMetadata());
        assertEquals("合同,纠纷,民事", request.getKeywords());
        assertEquals("这是案件摘要内容", request.getArchiveAbstract());
        assertEquals("备注信息", request.getRemarks());
    }

    @Test
    void testFilesList() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        List<ArchiveReceiveRequest.FileInfo> files = new ArrayList<>();

        ArchiveReceiveRequest.FileInfo file1 = new ArchiveReceiveRequest.FileInfo();
        file1.setFileName("起诉状.pdf");
        file1.setFileType("application/pdf");
        file1.setDownloadUrl("https://storage.example.com/files/1.pdf");
        file1.setFileSize(102400L);
        file1.setFileCategory("LITIGATION_DOC");
        file1.setDescription("原告起诉状");
        files.add(file1);

        ArchiveReceiveRequest.FileInfo file2 = new ArchiveReceiveRequest.FileInfo();
        file2.setFileName("证据材料.jpg");
        file2.setDownloadUrl("https://storage.example.com/files/2.jpg");
        files.add(file2);

        request.setFiles(files);

        assertEquals(2, request.getFiles().size());
        assertEquals("起诉状.pdf", request.getFiles().get(0).getFileName());
        assertEquals("application/pdf", request.getFiles().get(0).getFileType());
        assertEquals("https://storage.example.com/files/1.pdf", request.getFiles().get(0).getDownloadUrl());
        assertEquals(102400L, request.getFiles().get(0).getFileSize());
        assertEquals("LITIGATION_DOC", request.getFiles().get(0).getFileCategory());
        assertEquals("原告起诉状", request.getFiles().get(0).getDescription());
    }

    @Test
    void testFileInfo() {
        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();

        fileInfo.setFileName("document.docx");
        fileInfo.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        fileInfo.setDownloadUrl("https://example.com/download/doc.docx");
        fileInfo.setFileSize(51200L);
        fileInfo.setFileCategory("CONTRACT");
        fileInfo.setDescription("合同文档");

        assertEquals("document.docx", fileInfo.getFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", fileInfo.getFileType());
        assertEquals("https://example.com/download/doc.docx", fileInfo.getDownloadUrl());
        assertEquals(51200L, fileInfo.getFileSize());
        assertEquals("CONTRACT", fileInfo.getFileCategory());
        assertEquals("合同文档", fileInfo.getDescription());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveReceiveRequest request1 = new ArchiveReceiveRequest();
        request1.setSourceType("LAW_FIRM");
        request1.setSourceId("SRC-001");
        request1.setTitle("测试档案");

        ArchiveReceiveRequest request2 = new ArchiveReceiveRequest();
        request2.setSourceType("LAW_FIRM");
        request2.setSourceId("SRC-001");
        request2.setTitle("测试档案");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setTitle("测试");

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveReceiveRequest"));
    }

    @Test
    void testFileInfo_EqualsAndHashCode() {
        ArchiveReceiveRequest.FileInfo file1 = new ArchiveReceiveRequest.FileInfo();
        file1.setFileName("test.pdf");
        file1.setDownloadUrl("https://example.com/test.pdf");

        ArchiveReceiveRequest.FileInfo file2 = new ArchiveReceiveRequest.FileInfo();
        file2.setFileName("test.pdf");
        file2.setDownloadUrl("https://example.com/test.pdf");

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
    }
}
