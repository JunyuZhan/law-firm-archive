package com.lawfirm.application.evidence.service;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceExportItemDTO;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

/** 证据清单导出服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceExportService {

  /** 证据应用服务 */
  private final EvidenceAppService evidenceAppService;

  /** 案件仓储 */
  private final MatterRepository matterRepository;

  /** Word文档标题字体大小 */
  private static final int WORD_TITLE_FONT_SIZE = 18;

  /** Word文档正文字体大小 */
  private static final int WORD_BODY_FONT_SIZE = 12;

  /** PDF文档标题字体大小 */
  private static final int PDF_TITLE_FONT_SIZE = 18;

  /** PDF文档正文字体大小 */
  private static final int PDF_BODY_FONT_SIZE = 12;

  /** PDF文档日期字体大小 */
  private static final int PDF_DATE_FONT_SIZE = 11;

  /** PDF表格列宽比例：序号列 */
  private static final float PDF_TABLE_COL_WIDTH_INDEX = 8.0f;

  /** PDF表格列宽比例：证据名称列 */
  private static final float PDF_TABLE_COL_WIDTH_NAME = 25.0f;

  /** PDF表格列宽比例：证据类型列 */
  private static final float PDF_TABLE_COL_WIDTH_TYPE = 12.0f;

  /** PDF表格列宽比例：证明位列 */
  private static final float PDF_TABLE_COL_WIDTH_PURPOSE = 30.0f;

  /** PDF表格列宽比例：页码列 */
  private static final float PDF_TABLE_COL_WIDTH_PAGE = 10.0f;

  /** PDF表格列宽比例：原件/复印件列 */
  private static final float PDF_TABLE_COL_WIDTH_ORIGINAL = 15.0f;

  /** PDF表格表头字体大小 */
  private static final int PDF_TABLE_HEADER_FONT_SIZE = 10;

  /**
   * 导出证据清单为 Word 文档
   *
   * @param matterId 项目ID
   * @param items 证据清单项（可选，为空时导出全部）
   * @return Word 文档字节数组
   */
  public byte[] exportToWord(final Long matterId, final List<EvidenceExportItemDTO> items) {
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
        exportItems =
            evidences.stream()
                .map(
                    e -> {
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
        titleRun.setFontSize(WORD_TITLE_FONT_SIZE);
        titleRun.setFontFamily("宋体");

        // 空行
        document.createParagraph();

        // 项目信息
        XWPFParagraph infoPara = document.createParagraph();
        XWPFRun infoRun = infoPara.createRun();
        infoRun.setText("案件名称：" + matter.getName());
        infoRun.setFontSize(WORD_BODY_FONT_SIZE);
        infoRun.setFontFamily("宋体");

        XWPFParagraph casePara = document.createParagraph();
        XWPFRun caseRun = casePara.createRun();
        caseRun.setText("案件编号：" + (matter.getMatterNo() != null ? matter.getMatterNo() : ""));
        caseRun.setFontSize(WORD_BODY_FONT_SIZE);
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
          setCellText(
              row.getCell(2),
              item.getEvidenceTypeName() != null ? item.getEvidenceTypeName() : "",
              false);
          setCellText(
              row.getCell(3), item.getProvePurpose() != null ? item.getProvePurpose() : "", false);

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
            originalInfo =
                "原件"
                    + (item.getOriginalCount() != null ? "(" + item.getOriginalCount() + "份)" : "");
          } else {
            originalInfo =
                "复印件" + (item.getCopyCount() != null ? "(" + item.getCopyCount() + "份)" : "");
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
        dateRun.setText(
            "制表日期：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        dateRun.setFontSize(PDF_DATE_FONT_SIZE);
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

  /**
   * 设置单元格文本.
   *
   * @param cell 单元格
   * @param text 文本内容
   * @param bold 是否加粗
   */
  private void setCellText(final XWPFTableCell cell, final String text, final boolean bold) {
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
   * 导出证据清单为 PDF 文档
   *
   * @param matterId 项目ID
   * @param items 证据清单项（可选，为空时导出全部）
   * @return PDF 文档字节数组
   */
  public byte[] exportToPdf(final Long matterId, final List<EvidenceExportItemDTO> items) {
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
        exportItems =
            evidences.stream()
                .map(
                    e -> {
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

      // 创建 PDF 文档
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 获取中文字体
        PdfFont font;
        try {
          font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
          font = PdfFontFactory.createFont();
          log.warn("无法加载中文字体，使用默认字体");
        }

        // 标题
        Paragraph title =
            new Paragraph("证 据 清 单")
                .setFont(font)
                .setFontSize(PDF_TITLE_FONT_SIZE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // 空行
        document.add(new Paragraph("\n"));

        // 项目信息
        Paragraph matterInfo = new Paragraph().setFont(font).setFontSize(PDF_BODY_FONT_SIZE);
        matterInfo.add("案件名称：" + matter.getName() + "\n");
        if (matter.getMatterNo() != null) {
          matterInfo.add("案件编号：" + matter.getMatterNo() + "\n");
        }
        document.add(matterInfo);

        // 空行
        document.add(new Paragraph("\n"));

        // 创建表格（6列）
        Table table =
            new Table(
                    UnitValue.createPercentArray(
                        new float[] {
                          PDF_TABLE_COL_WIDTH_INDEX,
                          PDF_TABLE_COL_WIDTH_NAME,
                          PDF_TABLE_COL_WIDTH_TYPE,
                          PDF_TABLE_COL_WIDTH_PURPOSE,
                          PDF_TABLE_COL_WIDTH_PAGE,
                          PDF_TABLE_COL_WIDTH_ORIGINAL
                        }))
                .useAllAvailableWidth();

        // 表头
        String[] headers = {"序号", "证据名称", "证据类型", "证明目的", "页码", "原件/复印件"};
        for (String header : headers) {
          Cell cell =
              new Cell()
                  .add(
                      new Paragraph(header)
                          .setFont(font)
                          .setFontSize(PDF_TABLE_HEADER_FONT_SIZE)
                          .setBold())
                  .setTextAlignment(TextAlignment.CENTER);
          table.addHeaderCell(cell);
        }

        // 填充数据
        for (int i = 0; i < exportItems.size(); i++) {
          EvidenceExportItemDTO item = exportItems.get(i);

          // 序号
          table.addCell(
              new Cell()
                  .add(new Paragraph(String.valueOf(i + 1)).setFont(font).setFontSize(9))
                  .setTextAlignment(TextAlignment.CENTER));

          // 证据名称
          table.addCell(
              new Cell()
                  .add(
                      new Paragraph(item.getName() != null ? item.getName() : "")
                          .setFont(font)
                          .setFontSize(9)));

          // 证据类型
          table.addCell(
              new Cell()
                  .add(
                      new Paragraph(
                              item.getEvidenceTypeName() != null ? item.getEvidenceTypeName() : "")
                          .setFont(font)
                          .setFontSize(9)));

          // 证明目的
          table.addCell(
              new Cell()
                  .add(
                      new Paragraph(item.getProvePurpose() != null ? item.getProvePurpose() : "")
                          .setFont(font)
                          .setFontSize(9)));

          // 页码
          String pageRange = "";
          if (item.getPageStart() != null && item.getPageEnd() != null) {
            pageRange = item.getPageStart() + "-" + item.getPageEnd();
          } else if (item.getPageStart() != null) {
            pageRange = String.valueOf(item.getPageStart());
          }
          table.addCell(
              new Cell()
                  .add(new Paragraph(pageRange).setFont(font).setFontSize(9))
                  .setTextAlignment(TextAlignment.CENTER));

          // 原件/复印件
          String originalInfo = "";
          if (Boolean.TRUE.equals(item.getIsOriginal())) {
            originalInfo =
                "原件"
                    + (item.getOriginalCount() != null ? "(" + item.getOriginalCount() + "份)" : "");
          } else {
            originalInfo =
                "复印件" + (item.getCopyCount() != null ? "(" + item.getCopyCount() + "份)" : "");
          }
          table.addCell(new Cell().add(new Paragraph(originalInfo).setFont(font).setFontSize(9)));
        }

        document.add(table);

        // 空行
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("\n"));

        // 日期
        Paragraph datePara =
            new Paragraph(
                    "制表日期：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
                .setFont(font)
                .setFontSize(PDF_DATE_FONT_SIZE)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(datePara);

        document.close();
        return out.toByteArray();
      }
    } catch (Exception e) {
      log.error("导出证据清单PDF失败", e);
      throw new com.lawfirm.common.exception.BusinessException("导出证据清单PDF失败: " + e.getMessage());
    }
  }

  /**
   * 获取导出文件名.
   *
   * @param matterId 项目ID
   * @param format 文件格式
   * @return 文件名
   */
  public String getExportFileName(final Long matterId, final String format) {
    Matter matter = matterRepository.getById(matterId);
    String matterName = matter != null ? matter.getName() : "证据清单";
    String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return matterName + "_证据清单_" + dateStr + "." + format;
  }
}
