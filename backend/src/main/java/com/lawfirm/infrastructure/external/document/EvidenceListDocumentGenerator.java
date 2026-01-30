package com.lawfirm.infrastructure.external.document;

import com.itextpdf.kernel.colors.ColorConstants;
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
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.domain.matter.entity.Matter;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

/**
 * 证据清单文档生成器.
 *
 * <p>使用 Apache POI 生成 Word 格式的证据清单
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceListDocumentGenerator {

  /** 日期格式化器. */
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy年MM月dd日");

  /** 标题字体大小. */
  private static final int TITLE_FONT_SIZE = 18;

  /** 正文字体大小. */
  private static final int BODY_FONT_SIZE = 12;

  /** 表格列宽：序号列. */
  private static final int TABLE_COL_WIDTH_NO = 800;

  /** 表格列宽：证据名称列. */
  private static final int TABLE_COL_WIDTH_NAME = 2500;

  /** 表格列宽：证据类型列. */
  private static final int TABLE_COL_WIDTH_TYPE = 1200;

  /** 表格列宽：证据来源列. */
  private static final int TABLE_COL_WIDTH_SOURCE = 3000;

  /** 表格列宽：页数列. */
  private static final int TABLE_COL_WIDTH_PAGES = 800;

  /** 表格列宽：备注列. */
  private static final int TABLE_COL_WIDTH_REMARK = 1200;

  /** PDF页边距. */
  private static final int PDF_MARGIN = 25;

  /** PDF标题字体大小. */
  private static final int PDF_TITLE_FONT_SIZE = 18;

  /** PDF正文字体大小. */
  private static final int PDF_BODY_FONT_SIZE = 12;

  /** PDF表格列宽：序号列. */
  private static final int PDF_TABLE_COL_WIDTH_NO = 30;

  /** PDF表格列宽：备注列. */
  private static final int PDF_TABLE_COL_WIDTH_REMARK = 15;

  /**
   * 生成 Word 格式的证据清单.
   *
   * @param list 证据清单DTO
   * @param matter 案件信息
   * @param evidences 证据列表
   * @return Word文档字节数组
   */
  public byte[] generateWordDocument(
      final EvidenceListDTO list, final Matter matter, final List<EvidenceDTO> evidences) {
    try (XWPFDocument document = new XWPFDocument()) {
      // 标题
      XWPFParagraph titlePara = document.createParagraph();
      titlePara.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun titleRun = titlePara.createRun();
      titleRun.setText("证 据 清 单");
      titleRun.setBold(true);
      titleRun.setFontSize(TITLE_FONT_SIZE);
      titleRun.setFontFamily("宋体");

      // 空行
      document.createParagraph();

      // 案件信息
      if (matter != null) {
        addCaseInfo(document, matter);
      }

      // 清单信息
      addListInfo(document, list);

      // 空行
      document.createParagraph();

      // 证据表格
      if (evidences != null && !evidences.isEmpty()) {
        addEvidenceTable(document, evidences);
      }

      // 空行
      document.createParagraph();

      // 落款
      addSignature(document);

      // 输出
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("生成Word文档失败", e);
      throw new RuntimeException("生成Word文档失败: " + e.getMessage());
    }
  }

  private void addCaseInfo(final XWPFDocument document, final Matter matter) {
    XWPFParagraph para = document.createParagraph();
    XWPFRun run = para.createRun();
    run.setFontFamily("宋体");
    run.setFontSize(BODY_FONT_SIZE);

    StringBuilder sb = new StringBuilder();
    sb.append("案件名称：").append(matter.getName() != null ? matter.getName() : "").append("\n");
    if (matter.getMatterNo() != null) {
      sb.append("案件编号：").append(matter.getMatterNo()).append("\n");
    }
    if (matter.getOpposingParty() != null) {
      sb.append("对方当事人：").append(matter.getOpposingParty()).append("\n");
    }
    run.setText(sb.toString());
  }

  private void addListInfo(final XWPFDocument document, final EvidenceListDTO list) {
    XWPFParagraph para = document.createParagraph();
    XWPFRun run = para.createRun();
    run.setFontFamily("宋体");
    run.setFontSize(BODY_FONT_SIZE);

    StringBuilder sb = new StringBuilder();
    sb.append("清单名称：").append(list.getName() != null ? list.getName() : "").append("\n");
    sb.append("清单类型：")
        .append(list.getListTypeName() != null ? list.getListTypeName() : "")
        .append("\n");
    sb.append("清单编号：").append(list.getListNo() != null ? list.getListNo() : "").append("\n");
    run.setText(sb.toString());
  }

  private void addEvidenceTable(final XWPFDocument document, final List<EvidenceDTO> evidences) {
    // 创建表格
    XWPFTable table = document.createTable(evidences.size() + 1, 6);

    // 设置表格宽度
    table.setWidth("100%");

    // 表头
    XWPFTableRow headerRow = table.getRow(0);
    setTableCell(headerRow.getCell(0), "序号", true, TABLE_COL_WIDTH_NO);
    setTableCell(headerRow.getCell(1), "证据名称", true, TABLE_COL_WIDTH_NAME);
    setTableCell(headerRow.getCell(2), "证据类型", true, TABLE_COL_WIDTH_TYPE);
    setTableCell(headerRow.getCell(3), "证明目的", true, TABLE_COL_WIDTH_SOURCE);
    setTableCell(headerRow.getCell(4), "页码", true, TABLE_COL_WIDTH_PAGES);
    setTableCell(headerRow.getCell(5), "备注", true, TABLE_COL_WIDTH_REMARK);

    // 数据行
    int seq = 1;
    for (EvidenceDTO evidence : evidences) {
      XWPFTableRow row = table.getRow(seq);
      setTableCell(row.getCell(0), String.valueOf(seq), false, TABLE_COL_WIDTH_NO);
      setTableCell(
          row.getCell(1),
          evidence.getName() != null ? evidence.getName() : "",
          false,
          TABLE_COL_WIDTH_NAME);
      setTableCell(
          row.getCell(2),
          evidence.getEvidenceTypeName() != null ? evidence.getEvidenceTypeName() : "",
          false,
          TABLE_COL_WIDTH_TYPE);
      setTableCell(
          row.getCell(3),
          evidence.getProvePurpose() != null ? evidence.getProvePurpose() : "",
          false,
          TABLE_COL_WIDTH_SOURCE);

      // 页码范围
      String pageRange = "";
      if (evidence.getPageStart() != null && evidence.getPageEnd() != null) {
        pageRange = evidence.getPageStart() + "-" + evidence.getPageEnd();
      } else if (evidence.getPageRange() != null) {
        pageRange = evidence.getPageRange();
      }
      setTableCell(row.getCell(4), pageRange, false, TABLE_COL_WIDTH_PAGES);

      // 备注（原件/复印件）
      String remark = "";
      if (Boolean.TRUE.equals(evidence.getIsOriginal())) {
        remark = "原件";
        if (evidence.getOriginalCount() != null && evidence.getOriginalCount() > 1) {
          remark += "(" + evidence.getOriginalCount() + "份)";
        }
      } else {
        remark = "复印件";
        if (evidence.getCopyCount() != null && evidence.getCopyCount() > 0) {
          remark += "(" + evidence.getCopyCount() + "份)";
        }
      }
      setTableCell(row.getCell(5), remark, false, TABLE_COL_WIDTH_REMARK);

      seq++;
    }
  }

  private void setTableCell(
      final XWPFTableCell cell, final String text, final boolean isHeader, final int width) {
    // 设置单元格宽度
    CTTblWidth cellWidth = cell.getCTTc().addNewTcPr().addNewTcW();
    cellWidth.setW(BigInteger.valueOf(width));
    cellWidth.setType(STTblWidth.DXA);

    // 清除默认段落
    cell.removeParagraph(0);

    // 创建新段落
    XWPFParagraph para = cell.addParagraph();
    para.setAlignment(ParagraphAlignment.CENTER);
    para.setVerticalAlignment(org.apache.poi.xwpf.usermodel.TextAlignment.CENTER);

    XWPFRun run = para.createRun();
    run.setText(text);
    run.setFontFamily("宋体");
    run.setFontSize(10);
    if (isHeader) {
      run.setBold(true);
    }
  }

  private void addSignature(final XWPFDocument document) {
    // 证据提交人
    XWPFParagraph para1 = document.createParagraph();
    para1.setAlignment(ParagraphAlignment.RIGHT);
    XWPFRun run1 = para1.createRun();
    run1.setFontFamily("宋体");
    run1.setFontSize(BODY_FONT_SIZE);
    run1.setText("证据提交人：________________");

    // 空行
    document.createParagraph();

    // 日期
    XWPFParagraph para2 = document.createParagraph();
    para2.setAlignment(ParagraphAlignment.RIGHT);
    XWPFRun run2 = para2.createRun();
    run2.setFontFamily("宋体");
    run2.setFontSize(BODY_FONT_SIZE);
    run2.setText("日    期：" + LocalDateTime.now().format(DATE_FORMATTER));
  }

  /**
   * 生成 PDF 格式的证据清单.
   *
   * @param list 证据清单DTO
   * @param matter 案件信息
   * @param evidences 证据列表
   * @return PDF文档字节数组
   */
  public byte[] generatePdfDocument(
      final EvidenceListDTO list, final Matter matter, final List<EvidenceDTO> evidences) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(out);
      PdfDocument pdf = new PdfDocument(writer);
      Document document = new Document(pdf);

      // 使用内置字体（中文支持需要额外配置）
      PdfFont font;
      try {
        // 尝试使用系统中文字体
        font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
      } catch (Exception e) {
        // 回退到默认字体
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

      // 案件信息
      if (matter != null) {
        addPdfCaseInfo(document, matter, font);
      }

      // 清单信息
      addPdfListInfo(document, list, font);

      // 空行
      document.add(new Paragraph("\n"));

      // 证据表格
      if (evidences != null && !evidences.isEmpty()) {
        addPdfEvidenceTable(document, evidences, font);
      }

      // 空行
      document.add(new Paragraph("\n"));

      // 落款
      addPdfSignature(document, font);

      document.close();
      return out.toByteArray();
    } catch (Exception e) {
      log.error("生成PDF文档失败", e);
      throw new RuntimeException("生成PDF文档失败: " + e.getMessage());
    }
  }

  private void addPdfCaseInfo(final Document document, final Matter matter, final PdfFont font) {
    StringBuilder sb = new StringBuilder();
    sb.append("案件名称：").append(matter.getName() != null ? matter.getName() : "").append("\n");
    if (matter.getMatterNo() != null) {
      sb.append("案件编号：").append(matter.getMatterNo()).append("\n");
    }
    if (matter.getOpposingParty() != null) {
      sb.append("对方当事人：").append(matter.getOpposingParty()).append("\n");
    }

    Paragraph para = new Paragraph(sb.toString()).setFont(font).setFontSize(PDF_BODY_FONT_SIZE);
    document.add(para);
  }

  private void addPdfListInfo(
      final Document document, final EvidenceListDTO list, final PdfFont font) {
    StringBuilder sb = new StringBuilder();
    sb.append("清单名称：").append(list.getName() != null ? list.getName() : "").append("\n");
    sb.append("清单类型：")
        .append(list.getListTypeName() != null ? list.getListTypeName() : "")
        .append("\n");
    sb.append("清单编号：").append(list.getListNo() != null ? list.getListNo() : "").append("\n");

    Paragraph para = new Paragraph(sb.toString()).setFont(font).setFontSize(PDF_BODY_FONT_SIZE);
    document.add(para);
  }

  private void addPdfEvidenceTable(
      final Document document, final List<EvidenceDTO> evidences, final PdfFont font) {
    // 创建表格（6列）
    final float[] columnWidths = {
      8, PDF_MARGIN, PDF_BODY_FONT_SIZE, PDF_TABLE_COL_WIDTH_NO, 10, PDF_TABLE_COL_WIDTH_REMARK
    };
    Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

    // 表头
    String[] headers = {"序号", "证据名称", "证据类型", "证明目的", "页码", "备注"};
    for (String header : headers) {
      Cell cell =
          new Cell()
              .add(new Paragraph(header).setFont(font).setFontSize(10).setBold())
              .setBackgroundColor(ColorConstants.LIGHT_GRAY)
              .setTextAlignment(TextAlignment.CENTER);
      table.addHeaderCell(cell);
    }

    // 数据行
    int seq = 1;
    for (EvidenceDTO evidence : evidences) {
      table.addCell(createPdfCell(String.valueOf(seq), font));
      table.addCell(createPdfCell(evidence.getName() != null ? evidence.getName() : "", font));
      table.addCell(
          createPdfCell(
              evidence.getEvidenceTypeName() != null ? evidence.getEvidenceTypeName() : "", font));
      table.addCell(
          createPdfCell(
              evidence.getProvePurpose() != null ? evidence.getProvePurpose() : "", font));

      // 页码范围
      String pageRange = "";
      if (evidence.getPageStart() != null && evidence.getPageEnd() != null) {
        pageRange = evidence.getPageStart() + "-" + evidence.getPageEnd();
      } else if (evidence.getPageRange() != null) {
        pageRange = evidence.getPageRange();
      }
      table.addCell(createPdfCell(pageRange, font));

      // 备注
      String remark = "";
      if (Boolean.TRUE.equals(evidence.getIsOriginal())) {
        remark = "原件";
        if (evidence.getOriginalCount() != null && evidence.getOriginalCount() > 1) {
          remark += "(" + evidence.getOriginalCount() + "份)";
        }
      } else {
        remark = "复印件";
        if (evidence.getCopyCount() != null && evidence.getCopyCount() > 0) {
          remark += "(" + evidence.getCopyCount() + "份)";
        }
      }
      table.addCell(createPdfCell(remark, font));

      seq++;
    }

    document.add(table);
  }

  private Cell createPdfCell(final String text, final PdfFont font) {
    return new Cell()
        .add(new Paragraph(text).setFont(font).setFontSize(9))
        .setTextAlignment(TextAlignment.CENTER);
  }

  private void addPdfSignature(final Document document, final PdfFont font) {
    Paragraph para1 =
        new Paragraph("证据提交人：________________")
            .setFont(font)
            .setFontSize(PDF_BODY_FONT_SIZE)
            .setTextAlignment(TextAlignment.RIGHT);
    document.add(para1);

    document.add(new Paragraph("\n"));

    Paragraph para2 =
        new Paragraph("日    期：" + LocalDateTime.now().format(DATE_FORMATTER))
            .setFont(font)
            .setFontSize(PDF_BODY_FONT_SIZE)
            .setTextAlignment(TextAlignment.RIGHT);
    document.add(para2);
  }
}
