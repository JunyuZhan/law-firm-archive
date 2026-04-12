package com.archivesystem.dto;

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

class ArchiveReceiveDTOTest {

    @Test
    void testSourceInfo() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();

        dto.setSourceType("LAW_FIRM");
        dto.setSourceCode("LF-001");
        dto.setSourceId("CASE-123");
        dto.setSourceNo("SRC-2026-001");

        assertEquals("LAW_FIRM", dto.getSourceType());
        assertEquals("LF-001", dto.getSourceCode());
        assertEquals("CASE-123", dto.getSourceId());
        assertEquals("SRC-2026-001", dto.getSourceNo());
    }

    @Test
    void testBasicInfo() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();

        dto.setArchiveName("合同纠纷案件档案");
        dto.setArchiveType("LITIGATION");
        dto.setCategory("CIVIL");
        dto.setDescription("案件档案描述");

        assertEquals("合同纠纷案件档案", dto.getArchiveName());
        assertEquals("LITIGATION", dto.getArchiveType());
        assertEquals("CIVIL", dto.getCategory());
        assertEquals("案件档案描述", dto.getDescription());
    }

    @Test
    void testAssociatedInfo() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();
        LocalDate closeDate = LocalDate.of(2026, 2, 10);

        dto.setClientName("王五公司");
        dto.setResponsiblePerson("李四律师");
        dto.setCaseCloseDate(closeDate);

        assertEquals("王五公司", dto.getClientName());
        assertEquals("李四律师", dto.getResponsiblePerson());
        assertEquals(closeDate, dto.getCaseCloseDate());
    }

    @Test
    void testPhysicalInfo() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();

        dto.setVolumeCount(5);
        dto.setPageCount(200);
        dto.setCatalog("第一章 起诉状\n第二章 证据材料");

        assertEquals(5, dto.getVolumeCount());
        assertEquals(200, dto.getPageCount());
        assertTrue(dto.getCatalog().contains("起诉状"));
    }

    @Test
    void testRetentionAndRemarks() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();

        dto.setRetentionPeriod("PERMANENT");
        dto.setRemarks("重要案件，永久保存");

        assertEquals("PERMANENT", dto.getRetentionPeriod());
        assertEquals("重要案件，永久保存", dto.getRemarks());
    }

    @Test
    void testFilesList() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();
        List<ArchiveReceiveDTO.ArchiveFileDTO> files = new ArrayList<>();

        ArchiveReceiveDTO.ArchiveFileDTO file1 = new ArchiveReceiveDTO.ArchiveFileDTO();
        file1.setFileName("起诉状.pdf");
        file1.setFileType("application/pdf");
        file1.setFileSize(102400L);
        file1.setDownloadUrl("https://example.com/files/1.pdf");
        file1.setCategory("LITIGATION_DOC");
        file1.setSortOrder(1);
        file1.setDescription("原告起诉状");
        files.add(file1);

        ArchiveReceiveDTO.ArchiveFileDTO file2 = new ArchiveReceiveDTO.ArchiveFileDTO();
        file2.setFileName("证据材料.pdf");
        files.add(file2);

        dto.setFiles(files);

        assertEquals(2, dto.getFiles().size());
        assertEquals("起诉状.pdf", dto.getFiles().get(0).getFileName());
        assertEquals("application/pdf", dto.getFiles().get(0).getFileType());
        assertEquals(102400L, dto.getFiles().get(0).getFileSize());
    }

    @Test
    void testSourceSnapshot() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("caseId", "C-2026-001");
        snapshot.put("courtLevel", "中级人民法院");
        snapshot.put("filingDate", "2025-10-01");

        dto.setSourceSnapshot(snapshot);

        assertNotNull(dto.getSourceSnapshot());
        assertEquals("C-2026-001", dto.getSourceSnapshot().get("caseId"));
        assertEquals("中级人民法院", dto.getSourceSnapshot().get("courtLevel"));
    }

    @Test
    void testArchiveFileDTO() {
        ArchiveReceiveDTO.ArchiveFileDTO fileDTO = new ArchiveReceiveDTO.ArchiveFileDTO();

        fileDTO.setFileName("合同.docx");
        fileDTO.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        fileDTO.setFileSize(51200L);
        fileDTO.setDownloadUrl("https://storage.example.com/files/contract.docx");
        fileDTO.setCategory("CONTRACT");
        fileDTO.setSortOrder(2);
        fileDTO.setDescription("买卖合同原件扫描件");

        assertEquals("合同.docx", fileDTO.getFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", fileDTO.getFileType());
        assertEquals(51200L, fileDTO.getFileSize());
        assertEquals("https://storage.example.com/files/contract.docx", fileDTO.getDownloadUrl());
        assertEquals("CONTRACT", fileDTO.getCategory());
        assertEquals(2, fileDTO.getSortOrder());
        assertEquals("买卖合同原件扫描件", fileDTO.getDescription());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveReceiveDTO dto1 = new ArchiveReceiveDTO();
        dto1.setSourceType("LAW_FIRM");
        dto1.setArchiveName("测试档案");

        ArchiveReceiveDTO dto2 = new ArchiveReceiveDTO();
        dto2.setSourceType("LAW_FIRM");
        dto2.setArchiveName("测试档案");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveReceiveDTO dto = new ArchiveReceiveDTO();
        dto.setArchiveName("测试档案");
        dto.setSourceType("LAW_FIRM");

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveReceiveDTO"));
    }

    @Test
    void testArchiveFileDTO_EqualsAndHashCode() {
        ArchiveReceiveDTO.ArchiveFileDTO file1 = new ArchiveReceiveDTO.ArchiveFileDTO();
        file1.setFileName("test.pdf");
        file1.setFileSize(1000L);

        ArchiveReceiveDTO.ArchiveFileDTO file2 = new ArchiveReceiveDTO.ArchiveFileDTO();
        file2.setFileName("test.pdf");
        file2.setFileSize(1000L);

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
    }
}
