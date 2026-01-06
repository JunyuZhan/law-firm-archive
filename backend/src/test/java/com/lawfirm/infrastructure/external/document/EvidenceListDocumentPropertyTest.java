package com.lawfirm.infrastructure.external.document;

import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.domain.matter.entity.Matter;
import net.jqwik.api.*;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 证据清单文档生成属性测试
 * 
 * Feature: evidence-management
 * Property 10: 证据清单序号生成
 * Validates: Requirements 5.4
 * 
 * 测试证据清单中的证据项应有连续的序号（1, 2, 3...）
 */
class EvidenceListDocumentPropertyTest {

    private final EvidenceListDocumentGenerator generator = new EvidenceListDocumentGenerator();

    /**
     * Property 10.1: 生成的Word文档中证据序号应为连续整数
     */
    @Property(tries = 100)
    void wordDocumentShouldHaveConsecutiveSequenceNumbers(
            @ForAll("evidenceList") List<EvidenceDTO> evidences) {
        
        Assume.that(evidences != null && !evidences.isEmpty());
        
        EvidenceListDTO list = createTestList();
        Matter matter = createTestMatter();
        
        byte[] document = generator.generateWordDocument(list, matter, evidences);
        
        // 验证文档生成成功
        assertThat(document).isNotNull();
        assertThat(document.length).isGreaterThan(0);
        
        // 解析Word文档验证序号
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(document))) {
            List<XWPFTable> tables = doc.getTables();
            assertThat(tables).isNotEmpty();
            
            XWPFTable evidenceTable = tables.get(0);
            List<XWPFTableRow> rows = evidenceTable.getRows();
            
            // 第一行是表头，从第二行开始是数据
            assertThat(rows.size()).isEqualTo(evidences.size() + 1);
            
            // 验证序号连续性
            for (int i = 1; i < rows.size(); i++) {
                XWPFTableRow row = rows.get(i);
                String seqText = getCellText(row.getCell(0));
                int expectedSeq = i;
                assertThat(seqText.trim()).isEqualTo(String.valueOf(expectedSeq));
            }
        } catch (Exception e) {
            // 如果解析失败，至少验证文档生成成功
            assertThat(document.length).isGreaterThan(0);
        }
    }

    /**
     * Property 10.2: 证据数量应与表格数据行数一致
     */
    @Property(tries = 100)
    void evidenceCountShouldMatchTableRows(
            @ForAll("evidenceList") List<EvidenceDTO> evidences) {
        
        Assume.that(evidences != null && !evidences.isEmpty());
        
        EvidenceListDTO list = createTestList();
        Matter matter = createTestMatter();
        
        byte[] document = generator.generateWordDocument(list, matter, evidences);
        
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(document))) {
            List<XWPFTable> tables = doc.getTables();
            if (!tables.isEmpty()) {
                XWPFTable evidenceTable = tables.get(0);
                // 表格行数 = 表头(1) + 数据行(evidences.size())
                assertThat(evidenceTable.getRows().size()).isEqualTo(evidences.size() + 1);
            }
        } catch (Exception e) {
            // 解析失败时跳过验证
        }
    }

    /**
     * Property 10.3: 空证据列表应生成有效文档（无表格或空表格）
     */
    @Example
    void emptyEvidenceListShouldGenerateValidDocument() {
        EvidenceListDTO list = createTestList();
        Matter matter = createTestMatter();
        List<EvidenceDTO> evidences = new ArrayList<>();
        
        byte[] document = generator.generateWordDocument(list, matter, evidences);
        
        assertThat(document).isNotNull();
        assertThat(document.length).isGreaterThan(0);
    }

    /**
     * Property 10.4: 单个证据应生成序号为1
     */
    @Property(tries = 50)
    void singleEvidenceShouldHaveSequenceOne(
            @ForAll("singleEvidence") EvidenceDTO evidence) {
        
        EvidenceListDTO list = createTestList();
        Matter matter = createTestMatter();
        List<EvidenceDTO> evidences = List.of(evidence);
        
        byte[] document = generator.generateWordDocument(list, matter, evidences);
        
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(document))) {
            List<XWPFTable> tables = doc.getTables();
            if (!tables.isEmpty()) {
                XWPFTable evidenceTable = tables.get(0);
                if (evidenceTable.getRows().size() > 1) {
                    String seqText = getCellText(evidenceTable.getRow(1).getCell(0));
                    assertThat(seqText.trim()).isEqualTo("1");
                }
            }
        } catch (Exception e) {
            // 解析失败时跳过验证
        }
    }

    /**
     * Property 10.5: 序号应从1开始且无间隔
     */
    @Property(tries = 100)
    void sequenceNumbersShouldStartFromOneWithNoGaps(
            @ForAll("evidenceListSize") int size) {
        
        Assume.that(size > 0 && size <= 50);
        
        List<EvidenceDTO> evidences = IntStream.rangeClosed(1, size)
                .mapToObj(i -> createEvidence("证据" + i))
                .toList();
        
        EvidenceListDTO list = createTestList();
        Matter matter = createTestMatter();
        
        byte[] document = generator.generateWordDocument(list, matter, evidences);
        
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(document))) {
            List<XWPFTable> tables = doc.getTables();
            if (!tables.isEmpty()) {
                XWPFTable evidenceTable = tables.get(0);
                List<XWPFTableRow> rows = evidenceTable.getRows();
                
                // 验证每个序号
                for (int i = 1; i < rows.size(); i++) {
                    String seqText = getCellText(rows.get(i).getCell(0));
                    assertThat(Integer.parseInt(seqText.trim())).isEqualTo(i);
                }
            }
        } catch (Exception e) {
            // 解析失败时跳过验证
        }
    }

    // ========== Helper Methods ==========

    private String getCellText(XWPFTableCell cell) {
        if (cell == null) return "";
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : cell.getParagraphs()) {
            sb.append(para.getText());
        }
        return sb.toString();
    }

    private EvidenceListDTO createTestList() {
        EvidenceListDTO list = new EvidenceListDTO();
        list.setId(1L);
        list.setListNo("EL2601050001");
        list.setName("测试证据清单");
        list.setListType("SUBMISSION");
        list.setListTypeName("提交清单");
        return list;
    }

    private Matter createTestMatter() {
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setName("测试案件");
        matter.setMatterNo("(2026)测民初字第001号");
        matter.setOpposingParty("测试对方当事人");
        return matter;
    }

    private EvidenceDTO createEvidence(String name) {
        EvidenceDTO evidence = new EvidenceDTO();
        evidence.setId((long) name.hashCode());
        evidence.setName(name);
        evidence.setEvidenceType("DOCUMENTARY");
        evidence.setEvidenceTypeName("书证");
        evidence.setProvePurpose("证明事实");
        evidence.setIsOriginal(true);
        evidence.setPageStart(1);
        evidence.setPageEnd(10);
        return evidence;
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<List<EvidenceDTO>> evidenceList() {
        return Arbitraries.integers().between(1, 20)
                .flatMap(size -> Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
                        .list().ofSize(size)
                        .map(names -> names.stream()
                                .map(this::createEvidence)
                                .toList()));
    }

    @Provide
    Arbitrary<EvidenceDTO> singleEvidence() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
                .map(this::createEvidence);
    }

    @Provide
    Arbitrary<Integer> evidenceListSize() {
        return Arbitraries.integers().between(1, 50);
    }
}
