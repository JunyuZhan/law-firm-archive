package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveCreateRequestTest {

    @Test
    void testSettersAndGetters() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        LocalDate archiveDate = LocalDate.of(2026, 1, 15);
        LocalDate caseCloseDate = LocalDate.of(2025, 12, 31);
        List<Long> fileIds = Arrays.asList(1L, 2L, 3L);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("key1", "value1");

        request.setFondsId(1L);
        request.setCategoryId(10L);
        request.setArchiveType("LITIGATION");
        request.setTitle("合同纠纷案件档案");
        request.setFileNo("FILE-001");
        request.setResponsibility("李律师");
        request.setArchiveDate(archiveDate);
        request.setDocumentDate(archiveDate);
        request.setPageCount(100);
        request.setPiecesCount(5);
        request.setRetentionPeriod("PERMANENT");
        request.setSecurityLevel("NORMAL");
        request.setCaseNo("CASE-001");
        request.setCaseName("张三诉李四");
        request.setClientName("张三");
        request.setLawyerName("李律师");
        request.setCaseCloseDate(caseCloseDate);
        request.setKeywords("合同,纠纷");
        request.setArchiveAbstract("案件摘要");
        request.setRemarks("备注");
        request.setExtraData(extraData);
        request.setFileIds(fileIds);

        assertEquals(1L, request.getFondsId());
        assertEquals(10L, request.getCategoryId());
        assertEquals("LITIGATION", request.getArchiveType());
        assertEquals("合同纠纷案件档案", request.getTitle());
        assertEquals("FILE-001", request.getFileNo());
        assertEquals("李律师", request.getResponsibility());
        assertEquals(archiveDate, request.getArchiveDate());
        assertEquals(archiveDate, request.getDocumentDate());
        assertEquals(100, request.getPageCount());
        assertEquals(5, request.getPiecesCount());
        assertEquals("PERMANENT", request.getRetentionPeriod());
        assertEquals("NORMAL", request.getSecurityLevel());
        assertEquals("CASE-001", request.getCaseNo());
        assertEquals("张三诉李四", request.getCaseName());
        assertEquals("张三", request.getClientName());
        assertEquals("李律师", request.getLawyerName());
        assertEquals(caseCloseDate, request.getCaseCloseDate());
        assertEquals("合同,纠纷", request.getKeywords());
        assertEquals("案件摘要", request.getArchiveAbstract());
        assertEquals("备注", request.getRemarks());
        assertEquals(extraData, request.getExtraData());
        assertEquals(fileIds, request.getFileIds());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveCreateRequest request1 = new ArchiveCreateRequest();
        request1.setTitle("测试档案");
        request1.setArchiveType("LITIGATION");

        ArchiveCreateRequest request2 = new ArchiveCreateRequest();
        request2.setTitle("测试档案");
        request2.setArchiveType("LITIGATION");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("测试");

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveCreateRequest"));
    }
}
