package com.lawfirm.application.evidence.service;

import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceExportItemDTO;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 证据清单导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceExportService {

    private final EvidenceAppService evidenceAppService;
    private final MatterRepository matterRepository;

    /**
     * 导出证据清单为 Word 文档
     *
     * @param matterId 项目ID
     * @param items    证据清单项（可选，为空时导出全部）
     * @return Word 文档字节数组
     */
    public byte[] exportToWord(Long matterId, List<EvidenceExportItemDTO> items) {
        try {
            // 获取项目信息
            Matter matter = matterRepository.getById(matterId);
            if (matter == null) {
                throw new com.lawfirm.common.exception.BusinessException("项目不存在");
            }

            // 如果没有指定证据列表，获取全部证据
            List<EvidenceExportItemDTO> exportItems;
            if (items == null || items.isEmpty()) {
                List<EvidenceDTO> evidences = evidenceAppService.getEvidenceByMatter(matterId);
                exportItems = evidences.stream()
                        .map(e -> {
                            EvidenceExportItemDTO item = new EvidenceExportItemDTO();
                            item.setId(e.getId());
                            item.setName(e.getName());
                            item.setEvidenceTypeName(e.getEvidenceTypeName());
                            item.setProvePurpose(e.getProvePurpose());
                            item.setPageStart(e.getPageStart());
                            item.setPageEnd(e.getPageEnd());
                            item.setSource(e.getSource());
                            item.setIsOriginal(e.getIsOriginal());
                            item.setOriginalCount(e.getOriginalCount());
                            item.setCopyCount(e.getCopyCount());
                            return item;
                        })
                        .toList();
            } else {
                exportItems = items;
            }

            // 创建 Word 文档
            try (XWPFDocument document = new XWPFDocument()) {
                // 标题
                XWPFParagraph titlePara = document.createParagraph();
                titlePara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText("证 据 清 单");
                titleRun.setBold(true);
                titleRun.setFontSize(18);
                titleRun.setFontFamily("宋体");

                // 空行
                document.createParagraph();

                // 项目信息
                XWPFParagraph infoPara = document.createParagraph();
                XWPFRun infoRun = infoPara.createRun();
                infoRun.setText("案件名称：" + matter.getName());
                infoRun.setFontSize(12);
                infoRun.setFontFamily("宋体");

                XWPFParagraph casePara = document.createParagraph();
                XWPFRun caseRun = casePara.createRun();
                caseRun.setText("案件编号：" + (matter.getMatterNo() != null ? matter.getMatterNo() : ""));
                caseRun.setFontSize(12);
                caseRun.setFontFamily("宋体");

                // 空行
                document.createParagraph();

                // 创建表格
                XWPFTable table = document.createTable(exportItems.size() + 1, 6);
                table.setWidth("100%");

                // 设置表头
                XWPFTableRow headerRow = table.getRow(0);
                setCellText(headerRow.getCell(0), "序号", true);
                setCellText(headerRow.getCell(1), "证据名称", true);
                setCellText(headerRow.getCell(2), "证据类型", true);
                setCellText(headerRow.getCell(3), "证明目的", true);
                setCellText(headerRow.getCell(4), "页码", true);
                setCellText(headerRow.getCell(5), "原件/复印件", true);

                // 填充数据
                for (int i = 0; i < exportItems.size(); i++) {
                    EvidenceExportItemDTO item = exportItems.get(i);
                    XWPFTableRow row = table.getRow(i + 1);
                    
                    setCellText(row.getCell(0), String.valueOf(i + 1), false);
                    setCellText(row.getCell(1), item.getName() != null ? item.getName() : "", false);
                    setCellText(row.getCell(2), item.getEvidenceTypeName() != null ? item.getEvidenceTypeName() : "", false);
                    setCellText(row.getCell(3), item.getProvePurpose() != null ? item.getProvePurpose() : "", false);
                    
                    // 页码
                    String pageRange = "";
                    if (item.getPageStart() != null && item.getPageEnd() != null) {
                        pageRange = item.getPageStart() + "-" + item.getPageEnd();
                    } else if (item.getPageStart() != null) {
                        pageRange = String.valueOf(item.getPageStart());
                    }
                    setCellText(row.getCell(4), pageRange, false);
                    
                    // 原件/复印件
                    String originalInfo = "";
                    if (Boolean.TRUE.equals(item.getIsOriginal())) {
                        originalInfo = "原件" + (item.getOriginalCount() != null ? "(" + item.getOriginalCount() + "份)" : "");
                    } else {
                        originalInfo = "复印件" + (item.getCopyCount() != null ? "(" + item.getCopyCount() + "份)" : "");
                    }
                    setCellText(row.getCell(5), originalInfo, false);
                }

                // 空行
                document.createParagraph();
                document.createParagraph();

                // 日期
                XWPFParagraph datePara = document.createParagraph();
                datePara.setAlignment(ParagraphAlignment.RIGHT);
                XWPFRun dateRun = datePara.createRun();
                dateRun.setText("制表日期：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
                dateRun.setFontSize(11);
                dateRun.setFontFamily("宋体");

                // 输出
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("导出证据清单失败", e);
            throw new com.lawfirm.common.exception.BusinessException("导出证据清单失败: " + e.getMessage());
        }
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(10);
        run.setFontFamily("宋体");
    }

    /**
     * 获取导出文件名
     */
    public String getExportFileName(Long matterId, String format) {
        Matter matter = matterRepository.getById(matterId);
        String matterName = matter != null ? matter.getName() : "证据清单";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return matterName + "_证据清单_" + dateStr + "." + format;
    }
}

