package com.archivesystem.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveDTOTest {

    @Test
    void testArchiveDTO_BasicProperties() {
        ArchiveDTO dto = new ArchiveDTO();

        dto.setId(1L);
        dto.setArchiveNo("ARCH-2026-001");
        dto.setArchiveName("测试档案");
        dto.setArchiveType("LITIGATION");
        dto.setArchiveTypeName("诉讼案件");
        dto.setCategory("CIVIL");
        dto.setCategoryName("民事案件");
        dto.setDescription("档案描述");

        assertEquals(1L, dto.getId());
        assertEquals("ARCH-2026-001", dto.getArchiveNo());
        assertEquals("测试档案", dto.getArchiveName());
        assertEquals("LITIGATION", dto.getArchiveType());
        assertEquals("诉讼案件", dto.getArchiveTypeName());
        assertEquals("CIVIL", dto.getCategory());
        assertEquals("民事案件", dto.getCategoryName());
        assertEquals("档案描述", dto.getDescription());
    }

    @Test
    void testArchiveDTO_SourceInfo() {
        ArchiveDTO dto = new ArchiveDTO();

        dto.setSourceType("LAW_FIRM");
        dto.setSourceTypeName("律所系统");
        dto.setSourceId("SRC-001");
        dto.setSourceNo("CASE-2026-001");
        dto.setSourceName("律所管理系统");

        assertEquals("LAW_FIRM", dto.getSourceType());
        assertEquals("律所系统", dto.getSourceTypeName());
        assertEquals("SRC-001", dto.getSourceId());
        assertEquals("CASE-2026-001", dto.getSourceNo());
        assertEquals("律所管理系统", dto.getSourceName());
    }

    @Test
    void testArchiveDTO_AssociatedInfo() {
        ArchiveDTO dto = new ArchiveDTO();
        LocalDate closeDate = LocalDate.of(2026, 1, 15);

        dto.setClientName("张三");
        dto.setResponsiblePerson("李律师");
        dto.setCaseCloseDate(closeDate);

        assertEquals("张三", dto.getClientName());
        assertEquals("李律师", dto.getResponsiblePerson());
        assertEquals(closeDate, dto.getCaseCloseDate());
    }

    @Test
    void testArchiveDTO_PhysicalInfo() {
        ArchiveDTO dto = new ArchiveDTO();

        dto.setVolumeCount(3);
        dto.setPageCount(150);
        dto.setCatalog("目录内容");
        dto.setLocationId(10L);
        dto.setLocationName("档案室A区");
        dto.setBoxNo("BOX-001");
        dto.setHasElectronic(true);

        assertEquals(3, dto.getVolumeCount());
        assertEquals(150, dto.getPageCount());
        assertEquals("目录内容", dto.getCatalog());
        assertEquals(10L, dto.getLocationId());
        assertEquals("档案室A区", dto.getLocationName());
        assertEquals("BOX-001", dto.getBoxNo());
        assertTrue(dto.getHasElectronic());
    }

    @Test
    void testArchiveDTO_RetentionInfo() {
        ArchiveDTO dto = new ArchiveDTO();
        LocalDate expireDate = LocalDate.of(2036, 1, 15);

        dto.setRetentionPeriod("PERMANENT");
        dto.setRetentionPeriodName("永久");
        dto.setRetentionExpireDate(expireDate);

        assertEquals("PERMANENT", dto.getRetentionPeriod());
        assertEquals("永久", dto.getRetentionPeriodName());
        assertEquals(expireDate, dto.getRetentionExpireDate());
    }

    @Test
    void testArchiveDTO_StatusInfo() {
        ArchiveDTO dto = new ArchiveDTO();
        LocalDateTime storedAt = LocalDateTime.of(2026, 1, 10, 10, 30);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 1, 5, 9, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 12, 14, 30);

        dto.setStatus("STORED");
        dto.setStatusName("已入库");
        dto.setStoredByName("管理员");
        dto.setStoredAt(storedAt);
        dto.setReceivedAt(receivedAt);
        dto.setRemarks("备注信息");
        dto.setCreatedAt(createdAt);
        dto.setUpdatedAt(updatedAt);

        assertEquals("STORED", dto.getStatus());
        assertEquals("已入库", dto.getStatusName());
        assertEquals("管理员", dto.getStoredByName());
        assertEquals(storedAt, dto.getStoredAt());
        assertEquals(receivedAt, dto.getReceivedAt());
        assertEquals("备注信息", dto.getRemarks());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testArchiveDTO_FilesList() {
        ArchiveDTO dto = new ArchiveDTO();
        List<ArchiveDTO.ArchiveFileDTO> files = new ArrayList<>();

        ArchiveDTO.ArchiveFileDTO file1 = new ArchiveDTO.ArchiveFileDTO();
        file1.setId(1L);
        file1.setFileName("file1.pdf");
        files.add(file1);

        dto.setFiles(files);
        dto.setFileCount(1);

        assertEquals(1, dto.getFiles().size());
        assertEquals(1, dto.getFileCount());
    }

    @Test
    void testArchiveFileDTO() {
        ArchiveDTO.ArchiveFileDTO fileDTO = new ArchiveDTO.ArchiveFileDTO();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 10, 0);

        fileDTO.setId(1L);
        fileDTO.setFileName("document.pdf");
        fileDTO.setOriginalFileName("原始文件名.pdf");
        fileDTO.setFileType("application/pdf");
        fileDTO.setFileSize(1024000L);
        fileDTO.setCategory("EVIDENCE");
        fileDTO.setCategoryName("证据材料");
        fileDTO.setSortOrder(1);
        fileDTO.setDescription("文件描述");
        fileDTO.setDownloadUrl("https://example.com/file/1");
        fileDTO.setCreatedAt(createdAt);

        assertEquals(1L, fileDTO.getId());
        assertEquals("document.pdf", fileDTO.getFileName());
        assertEquals("原始文件名.pdf", fileDTO.getOriginalFileName());
        assertEquals("application/pdf", fileDTO.getFileType());
        assertEquals(1024000L, fileDTO.getFileSize());
        assertEquals("EVIDENCE", fileDTO.getCategory());
        assertEquals("证据材料", fileDTO.getCategoryName());
        assertEquals(1, fileDTO.getSortOrder());
        assertEquals("文件描述", fileDTO.getDescription());
        assertEquals("https://example.com/file/1", fileDTO.getDownloadUrl());
        assertEquals(createdAt, fileDTO.getCreatedAt());
    }

    @Test
    void testArchiveDTO_EqualsAndHashCode() {
        ArchiveDTO dto1 = new ArchiveDTO();
        dto1.setId(1L);
        dto1.setArchiveNo("ARCH-001");

        ArchiveDTO dto2 = new ArchiveDTO();
        dto2.setId(1L);
        dto2.setArchiveNo("ARCH-001");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testArchiveDTO_ToString() {
        ArchiveDTO dto = new ArchiveDTO();
        dto.setId(1L);
        dto.setArchiveName("测试档案");

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveDTO"));
        assertTrue(str.contains("测试档案"));
    }

    @Test
    void testArchiveFileDTO_EqualsAndHashCode() {
        ArchiveDTO.ArchiveFileDTO file1 = new ArchiveDTO.ArchiveFileDTO();
        file1.setId(1L);
        file1.setFileName("test.pdf");

        ArchiveDTO.ArchiveFileDTO file2 = new ArchiveDTO.ArchiveFileDTO();
        file2.setId(1L);
        file2.setFileName("test.pdf");

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
    }
}
