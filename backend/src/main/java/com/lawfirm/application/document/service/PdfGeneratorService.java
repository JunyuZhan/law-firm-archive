package com.lawfirm.application.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.lawfirm.application.finance.dto.ContractPrintDTO;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.workbench.entity.Approval;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PDF 生成服务
 *
 * <p>用于生成卷宗材料的PDF文档： - 收案审批表 - 委托合同 - 授权委托书
 */
@Slf4j
@Service
@SuppressWarnings("unused") // 保留未使用的布局常量供未来PDF模板使用
public class PdfGeneratorService {

  /** 日期格式化器 */
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy年MM月dd日");

  /** 表头背景颜色 */
  private static final DeviceRgb HEADER_BG_COLOR = new DeviceRgb(240, 240, 240);

  // 页面边距常量
  /** 页面左边距 */
  private static final float MARGIN_LEFT = 40f;

  /** 页面右边距 */
  private static final float MARGIN_RIGHT = 50f;

  /** 页面上边距 */
  private static final float MARGIN_TOP = 40f;

  /** 页面下边距 */
  private static final float MARGIN_BOTTOM = 50f;

  // 表格列宽比例常量
  /** 主表格标签列宽度比例 */
  private static final float WIDTH_RATIO_MAIN_LABEL = 15f;

  /** 主表格值列宽度比例1 */
  private static final float WIDTH_RATIO_MAIN_VALUE_1 = 28.33f;

  /** 主表格值列宽度比例2 */
  private static final float WIDTH_RATIO_MAIN_VALUE_2 = 28.33f;

  /** 主表格值列宽度比例3 */
  private static final float WIDTH_RATIO_MAIN_VALUE_3 = 28.34f;

  /** 审批表标签列宽度比例 */
  private static final float WIDTH_RATIO_APPROVAL_LABEL = 25f;

  /** 审批表内容列宽度比例 */
  private static final float WIDTH_RATIO_APPROVAL_CONTENT = 75f;

  /** 简单表格标签列宽度比例 */
  private static final float WIDTH_RATIO_SIMPLE_LABEL = 30f;

  /** 简单表格内容列宽度比例 */
  private static final float WIDTH_RATIO_SIMPLE_CONTENT = 70f;

  /** 四等分列宽度比例 */
  private static final float WIDTH_RATIO_QUARTER = 25f;

  /** 三等分列宽度比例 */
  private static final float WIDTH_RATIO_THIRD = 33.33f;

  /** 两等分列宽度比例 */
  private static final float WIDTH_RATIO_HALF = 50f;

  /** 签章表格列宽度比例 */
  private static final float WIDTH_RATIO_SIGN_LABEL = 35f;

  /** 签章表格内容列宽度比例 */
  private static final float WIDTH_RATIO_SIGN_CONTENT = 50f;

  // 单元格高度常量
  /** 摘要单元格高度 */
  private static final float HEIGHT_SUMMARY_CELL = 140f;

  /** 审批单元格高度 */
  private static final float HEIGHT_APPROVAL_CELL = 50f;

  // 字体大小常量
  /** 字体大小：标题 */
  private static final float FONT_SIZE_TITLE = 16f;

  /** 字体大小：副标题 */
  private static final float FONT_SIZE_SUBTITLE = 14f;

  /** 字体大小：正常大 */
  private static final float FONT_SIZE_NORMAL_LARGE = 12f;

  /** 字体大小：正常 */
  private static final float FONT_SIZE_NORMAL = 11f;

  /** 字体大小：小 */
  private static final float FONT_SIZE_SMALL = 10.5f;

  /** 字体大小：页脚 */
  private static final float FONT_SIZE_FOOTER = 12f;

  /** 字体大小：大标题 */
  private static final float FONT_SIZE_LARGE_TITLE = 18f;

  /** 字体大小：超大标题 */
  private static final float FONT_SIZE_XLARGE_TITLE = 22f;

  /** 字体大小：超大标题（20pt） */
  private static final float FONT_SIZE_XLARGE = 20f;

  /** 字体大小：极小（9pt） */
  private static final float FONT_SIZE_TINY = 9f;

  // 边距常量
  /** 小边距 */
  private static final float MARGIN_SMALL = 5f;

  /** 中等边距 */
  private static final float MARGIN_MEDIUM = 8f;

  /** 标准边距 */
  private static final float MARGIN_STANDARD = 10f;

  /** 章节边距 */
  private static final float MARGIN_SECTION_TOP = 20f;

  /** 签章区域上边距 */
  private static final float MARGIN_SIGN_AREA_TOP = 40f;

  /** 标题上边距（中等） */
  private static final float MARGIN_TITLE_TOP_MEDIUM = 15f;

  /** 小间距（用于段落间距） */
  private static final float MARGIN_VERY_SMALL = 4f;

  /** 小标题上边距 */
  private static final float MARGIN_SUBTITLE_TOP = 10f;

  /** 日期下边距 */
  private static final float MARGIN_DATE_BOTTOM = 12f;

  /** 首行缩进量（磅） */
  private static final float FIRST_LINE_INDENT = 24f;

  /** 首行缩进量（两个汉字宽度） */
  private static final float FIRST_LINE_INDENT_DOUBLE = 32f;

  /** 分隔线重复次数 */
  private static final int SEPARATOR_REPEAT_COUNT = 40;

  /** 单元格内边距 */
  private static final float PADDING_SMALL = 5f;

  /** 跨3列 */
  private static final int COLSPAN_3 = 3;

  /** 跨3行 */
  private static final int ROWSPAN_3 = 3;

  /** 底部边距10 */
  private static final float MARGIN_BOTTOM_10 = 10f;

  /** 案情简介上边距 */
  private static final float MARGIN_CASE_DESCRIPTION_TOP = 20f;

  /** 段落间距（12pt） */
  private static final float MARGIN_PARAGRAPH_SPACING = 12f;

  /** 字体大小：五号宋体（10.5pt） */
  private static final float FONT_SIZE_SONG_5 = 10.5f;

  /**
   * 生成收案审批表PDF（完整版，与合同管理模块预览一致） 使用 ContractPrintDTO 生成包含所有字段的完整审批表
   *
   * @param printDTO 合同打印DTO
   * @return PDF字节数组
   */
  public byte[] generateApprovalFormPdfFromPrintDTO(final ContractPrintDTO printDTO) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      document.setMargins(MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);

      PdfFont font = createChineseFont();

      addApprovalFormHeader(document, font, printDTO);
      Table mainTable = createMainTable(font, printDTO);
      document.add(mainTable);
      addApprovalSection(document, font, printDTO);

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("生成收案审批表PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 添加审批表头部（律所名称、标题、合同编号）
   *
   * @param document PDF文档对象
   * @param font 字体
   * @param printDTO 合同打印DTO
   */
  private void addApprovalFormHeader(
      final Document document, final PdfFont font, final ContractPrintDTO printDTO) {
    // 律所名称标题
    if (printDTO.getFirmName() != null && !printDTO.getFirmName().isEmpty()) {
      document.add(
          new Paragraph(printDTO.getFirmName())
              .setFont(font)
              .setFontSize(FONT_SIZE_TITLE)
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginBottom(MARGIN_SMALL));
    }

    // 主标题：收案审批表
    Paragraph title =
        new Paragraph("收案审批表")
            .setFont(font)
            .setFontSize(FONT_SIZE_TITLE)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10);
    document.add(title);

    // 合同编号（右对齐）
    if (printDTO.getContractNo() != null) {
      document.add(
          new Paragraph("合同编号：" + printDTO.getContractNo())
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setTextAlignment(TextAlignment.RIGHT)
              .setMarginBottom(MARGIN_STANDARD));
    }
  }

  /**
   * 创建主表格（包含所有字段）
   *
   * @param font 字体
   * @param printDTO 合同打印DTO
   * @return 主表格对象
   */
  private Table createMainTable(final PdfFont font, final ContractPrintDTO printDTO) {
    Table mainTable =
        new Table(
                UnitValue.createPercentArray(
                    new float[] {
                      WIDTH_RATIO_MAIN_LABEL,
                      WIDTH_RATIO_MAIN_VALUE_1,
                      WIDTH_RATIO_MAIN_VALUE_2,
                      WIDTH_RATIO_MAIN_VALUE_3
                    }))
            .setWidth(UnitValue.createPercentValue(100));

    // 委托人
    addFullWidthRow(
        mainTable, font, "委托人", printDTO.getClientName() != null ? printDTO.getClientName() : "-");

    // 案由
    String causeOfActionDisplay =
        printDTO.getCauseOfActionName() != null && !printDTO.getCauseOfActionName().isEmpty()
            ? printDTO.getCauseOfActionName()
            : (printDTO.getCaseTypeName() != null ? printDTO.getCaseTypeName() : "-");
    addFullWidthRow(mainTable, font, "案由", causeOfActionDisplay);

    // 关联当事人
    addFullWidthRow(
        mainTable,
        font,
        "关联当事人",
        printDTO.getOpposingParty() != null ? printDTO.getOpposingParty() : "-");

    // 委托程序
    addFullWidthRow(
        mainTable,
        font,
        "委托程序",
        printDTO.getTrialStageName() != null ? printDTO.getTrialStageName() : "-");

    // 有无利益冲突
    String conflictResult =
        printDTO.getConflictCheckResult() != null ? printDTO.getConflictCheckResult() : "待审查";
    addFullWidthRow(mainTable, font, "有无利益冲突", conflictResult);

    // 代理/辩护费 + 委托时间
    String feeDisplay =
        printDTO.getTotalAmount() != null
            ? "¥" + String.format("%.0f", printDTO.getTotalAmount())
            : "-";
    String signDateStr = formatDateChinese(printDTO.getSignDate());
    addTwoColumnRow(mainTable, font, "代理/辩护费", feeDisplay, "委托时间", signDateStr);

    // 接待人 + 办案单位
    String originatorName =
        printDTO.getOriginatorName() != null
            ? printDTO.getOriginatorName()
            : (printDTO.getSignerName() != null ? printDTO.getSignerName() : "-");
    String jurisdictionCourt =
        printDTO.getJurisdictionCourt() != null ? printDTO.getJurisdictionCourt() : "-";
    addTwoColumnRow(mainTable, font, "接待人", originatorName, "办案单位", jurisdictionCourt);

    // 案情摘要（高度更大的单元格）
    String description =
        printDTO.getDescription() != null && !printDTO.getDescription().isEmpty()
            ? printDTO.getDescription()
            : "暂无案情摘要";
    Cell summaryLabelCell = createLabelCell("案情摘要\n（附接待笔录）", font);
    summaryLabelCell.setHeight(HEIGHT_SUMMARY_CELL);
    Cell summaryValueCell = createValueCell(description, font);
    summaryValueCell.setHeight(HEIGHT_SUMMARY_CELL);
    summaryValueCell.setVerticalAlignment(VerticalAlignment.TOP);
    mainTable.addCell(summaryLabelCell);
    mainTable.addCell(new Cell(1, 3).add(summaryValueCell));

    return mainTable;
  }

  /**
   * 添加审查意见部分
   *
   * @param document PDF文档对象
   * @param font 字体
   * @param printDTO 合同打印DTO
   */
  private void addApprovalSection(
      final Document document, final PdfFont font, final ContractPrintDTO printDTO) {
    // 审查意见部分
    document.add(
        new Paragraph("\n审 查 意 见")
            .setFont(font)
            .setFontSize(FONT_SIZE_SUBTITLE)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(MARGIN_SUBTITLE_TOP)
            .setMarginBottom(MARGIN_MEDIUM));

    // 审批意见表格：标签列25%，意见列75%
    Table approvalTable =
        new Table(
                UnitValue.createPercentArray(
                    new float[] {WIDTH_RATIO_APPROVAL_LABEL, WIDTH_RATIO_APPROVAL_CONTENT}))
            .setWidth(UnitValue.createPercentValue(100));

    String originatorName =
        printDTO.getOriginatorName() != null
            ? printDTO.getOriginatorName()
            : (printDTO.getSignerName() != null ? printDTO.getSignerName() : "-");
    String signDateStr = formatDateChinese(printDTO.getSignDate());

    // 接待律师意见
    Cell receptionLabelCell = createLabelCell("接待律师意见", font);
    approvalTable.addCell(new Cell(3, 1).add(receptionLabelCell));
    approvalTable.addCell(
        new Cell()
            .add(new Paragraph("拟接受委托").setFont(font).setFontSize(FONT_SIZE_NORMAL_LARGE))
            .setHeight(HEIGHT_APPROVAL_CELL));

    // 接待律师签名
    approvalTable.addCell(
        new Cell()
            .add(
                new Paragraph("签名：" + originatorName)
                    .setFont(font)
                    .setFontSize(FONT_SIZE_NORMAL_LARGE))
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(null));
    approvalTable.addCell(
        new Cell()
            .add(
                new Paragraph("日期：" + signDateStr)
                    .setFont(font)
                    .setFontSize(FONT_SIZE_NORMAL_LARGE))
            .setBorder(null));

    // 律所领导意见
    if (printDTO.getApprovals() != null && !printDTO.getApprovals().isEmpty()) {
      ContractPrintDTO.ApprovalInfo leaderApproval = printDTO.getApprovals().get(0);
      String leaderComment = leaderApproval.getComment() != null ? leaderApproval.getComment() : "";
      String leaderName =
          leaderApproval.getApproverName() != null ? leaderApproval.getApproverName() : "";
      String leaderDate = formatDateTimeChinese(leaderApproval.getApprovedAt());

      Cell leaderLabelCell = createLabelCell("律所领导意见", font);
      approvalTable.addCell(new Cell(3, 1).add(leaderLabelCell));
      approvalTable.addCell(
          new Cell()
              .add(new Paragraph(leaderComment).setFont(font).setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setHeight(HEIGHT_APPROVAL_CELL));

      approvalTable.addCell(
          new Cell()
              .add(
                  new Paragraph("签名：" + leaderName)
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setTextAlignment(TextAlignment.RIGHT)
              .setBorder(null));
      approvalTable.addCell(
          new Cell()
              .add(
                  new Paragraph("日期：" + leaderDate)
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setBorder(null));
    } else {
      // 预留律所领导签字区域
      Cell leaderLabelCell = createLabelCell("律所领导意见", font);
      approvalTable.addCell(new Cell(3, 1).add(leaderLabelCell));
      approvalTable.addCell(
          new Cell()
              .add(new Paragraph("").setFont(font).setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setHeight(HEIGHT_APPROVAL_CELL));

      approvalTable.addCell(
          new Cell()
              .add(new Paragraph("签名：").setFont(font).setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setTextAlignment(TextAlignment.RIGHT)
              .setBorder(null));
      approvalTable.addCell(
          new Cell()
              .add(
                  new Paragraph("日期：____年____月____日")
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL_LARGE))
              .setBorder(null));
    }

    document.add(approvalTable);
  }

  /**
   * 生成收案审批表PDF（简化版，保持向后兼容）
   *
   * @param approval 审批信息
   * @param contract 合同信息
   * @param matter 项目信息
   * @param client 客户信息
   * @return PDF字节数组
   */
  public byte[] generateApprovalFormPdf(
      final Approval approval, final Contract contract, final Matter matter, final Client client) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      document.setMargins(WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF);

      PdfFont font = createChineseFont();

      // 标题
      Paragraph title =
          new Paragraph("收 案 审 批 表")
              .setFont(font)
              .setFontSize(FONT_SIZE_XLARGE)
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginBottom(MARGIN_SECTION_TOP);
      document.add(title);

      // 基本信息表格
      Table infoTable =
          new Table(
                  UnitValue.createPercentArray(
                      new float[] {
                        WIDTH_RATIO_QUARTER,
                        WIDTH_RATIO_QUARTER,
                        WIDTH_RATIO_QUARTER,
                        WIDTH_RATIO_QUARTER
                      }))
              .setWidth(UnitValue.createPercentValue(100));

      // 合同编号
      addTableRow(
          infoTable,
          font,
          "合同编号",
          contract != null ? contract.getContractNo() : "-",
          "申请日期",
          approval != null && approval.getCreatedAt() != null
              ? approval.getCreatedAt().format(DATE_FORMATTER)
              : "-");

      // 客户信息
      addTableRow(
          infoTable,
          font,
          "客户名称",
          client != null ? client.getName() : "-",
          "联系电话",
          client != null && client.getContactPhone() != null ? client.getContactPhone() : "-");

      // 项目信息
      addTableRow(
          infoTable,
          font,
          "项目名称",
          matter != null ? matter.getName() : "-",
          "案件类型",
          matter != null && matter.getCaseType() != null ? matter.getCaseType() : "-");

      // 律师费用
      addTableRow(
          infoTable,
          font,
          "律师费",
          contract != null && contract.getTotalAmount() != null
              ? String.format("%.2f 元", contract.getTotalAmount())
              : "-",
          "收费方式",
          contract != null && contract.getFeeType() != null ? contract.getFeeType() : "-");

      document.add(infoTable);

      // 案情简介
      document.add(
          new Paragraph("案情简介：")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL_LARGE)
              .setBold()
              .setMarginTop(MARGIN_CASE_DESCRIPTION_TOP));

      document.add(
          new Paragraph(
                  matter != null && matter.getDescription() != null
                      ? matter.getDescription()
                      : "（无）")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_SECTION_TOP));

      // 审批信息
      document.add(
          new Paragraph("审批意见：")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL_LARGE)
              .setBold()
              .setMarginTop(MARGIN_SUBTITLE_TOP));

      Table approvalTable =
          new Table(
                  UnitValue.createPercentArray(
                      new float[] {WIDTH_RATIO_SIMPLE_LABEL, WIDTH_RATIO_SIMPLE_CONTENT}))
              .setWidth(UnitValue.createPercentValue(100));

      addSimpleRow(
          approvalTable,
          font,
          "审批状态",
          approval != null ? getApprovalStatusText(approval.getStatus()) : "-");
      addSimpleRow(
          approvalTable,
          font,
          "审批人",
          approval != null && approval.getApproverName() != null
              ? approval.getApproverName()
              : "-");
      addSimpleRow(
          approvalTable,
          font,
          "审批时间",
          approval != null && approval.getApprovedAt() != null
              ? approval.getApprovedAt().format(DATE_FORMATTER)
              : "-");
      addSimpleRow(
          approvalTable,
          font,
          "审批意见",
          approval != null && approval.getComment() != null ? approval.getComment() : "（无）");

      document.add(approvalTable);

      // 签章区域
      document.add(new Paragraph("\n"));
      Table signTable =
          new Table(UnitValue.createPercentArray(new float[] {WIDTH_RATIO_HALF, WIDTH_RATIO_HALF}))
              .setWidth(UnitValue.createPercentValue(100))
              .setMarginTop(MARGIN_SIGN_AREA_TOP);

      signTable.addCell(
          new Cell()
              .add(new Paragraph("申请人签字：\n\n\n日期：").setFont(font).setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));
      signTable.addCell(
          new Cell()
              .add(new Paragraph("审批人签字：\n\n\n日期：").setFont(font).setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));

      document.add(signTable);

      // 页脚
      document.add(
          new Paragraph("本表由系统自动生成，仅供参考，请以签字盖章版本为准。")
              .setFont(font)
              .setFontSize(FONT_SIZE_TINY)
              .setFontColor(ColorConstants.GRAY)
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginTop(WIDTH_RATIO_SIMPLE_LABEL));

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("生成收案审批表PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 生成委托合同PDF
   *
   * @param contract 合同信息
   * @param matter 项目信息
   * @param client 客户信息
   * @return PDF字节数组
   */
  public byte[] generateContractPdf(
      final Contract contract, final Matter matter, final Client client) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      document.setMargins(WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF);

      PdfFont font = createChineseFont();

      // 标题
      String contractTitle =
          contract != null && contract.getName() != null ? contract.getName() : "委托代理合同";
      document.add(
          new Paragraph(contractTitle)
              .setFont(font)
              .setFontSize(FONT_SIZE_LARGE_TITLE)
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginBottom(MARGIN_SECTION_TOP));

      // 合同编号
      document.add(
          new Paragraph("合同编号：" + (contract != null ? contract.getContractNo() : "-"))
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setTextAlignment(TextAlignment.RIGHT)
              .setMarginBottom(MARGIN_SECTION_TOP));

      // 甲乙双方信息
      document.add(
          new Paragraph("甲方（委托人）：" + (client != null ? client.getName() : "_______________"))
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_SMALL));

      document.add(
          new Paragraph(
                  "联系电话："
                      + (client != null && client.getContactPhone() != null
                          ? client.getContactPhone()
                          : "_______________"))
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_SMALL));

      document.add(
          new Paragraph("乙方（受托人）：_____________________律师事务所")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginTop(MARGIN_TITLE_TOP_MEDIUM)
              .setMarginBottom(MARGIN_SECTION_TOP));

      // 合同内容
      if (contract != null && contract.getContent() != null && !contract.getContent().isEmpty()) {
        // 处理HTML或富文本内容，简单提取纯文本
        String content = stripHtml(contract.getContent());
        document.add(
            new Paragraph(content)
                .setFont(font)
                .setFontSize(FONT_SIZE_NORMAL)
                .setMarginBottom(MARGIN_SECTION_TOP));
      } else {
        // 默认合同条款
        addDefaultContractTerms(document, font, contract, matter);
      }

      // 签章区域
      Table signTable =
          new Table(UnitValue.createPercentArray(new float[] {WIDTH_RATIO_HALF, WIDTH_RATIO_HALF}))
              .setWidth(UnitValue.createPercentValue(100))
              .setMarginTop(MARGIN_SIGN_AREA_TOP);

      signTable.addCell(
          new Cell()
              .add(
                  new Paragraph("甲方（签章）：\n\n\n日期：     年   月   日")
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));
      signTable.addCell(
          new Cell()
              .add(
                  new Paragraph("乙方（签章）：\n\n\n日期：     年   月   日")
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));

      document.add(signTable);

      // 页脚
      document.add(
          new Paragraph("本合同由系统自动生成，仅供参考，请以签字盖章版本为准。")
              .setFont(font)
              .setFontSize(FONT_SIZE_TINY)
              .setFontColor(ColorConstants.GRAY)
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginTop(WIDTH_RATIO_SIMPLE_LABEL));

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("生成委托合同PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 生成授权委托书PDF（简化版，向后兼容）
   *
   * @param matter 项目信息
   * @param client 客户信息
   * @return PDF字节数组
   */
  public byte[] generatePowerOfAttorneyPdf(final Matter matter, final Client client) {
    return generatePowerOfAttorneyPdf(matter, client, null, null, null);
  }

  /**
   * 生成授权委托书PDF（紧凑排版，确保一页显示）
   *
   * @param matter 项目信息
   * @param client 客户信息
   * @param lawyerName 承办律师姓名（可为null）
   * @param lawyerLicenseNo 律师执业证号（可为null）
   * @param firmName 律所名称（可为null）
   * @return PDF字节数组
   */
  public byte[] generatePowerOfAttorneyPdf(
      final Matter matter,
      final Client client,
      final String lawyerName,
      final String lawyerLicenseNo,
      final String firmName) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      // 减小边距，增加可用空间，确保内容能在一页内显示
      document.setMargins(
          WIDTH_RATIO_SIGN_LABEL, MARGIN_RIGHT, WIDTH_RATIO_SIGN_LABEL, MARGIN_RIGHT);

      PdfFont font = createChineseFont();

      // 标题
      document.add(
          new Paragraph("授 权 委 托 书")
              .setFont(font)
              .setFontSize(FONT_SIZE_XLARGE)
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginBottom(MARGIN_TITLE_TOP_MEDIUM)); // 减小标题底部间距

      // 委托人信息
      String clientName = client != null ? client.getName() : "_______________";
      String clientType =
          client != null && "ENTERPRISE".equals(client.getClientType()) ? "法定代表人" : "身份证号";
      String clientIdValue =
          client != null
              ? ("ENTERPRISE".equals(client.getClientType())
                  ? (client.getLegalRepresentative() != null
                      ? client.getLegalRepresentative()
                      : "_______________")
                  : (client.getIdCard() != null ? client.getIdCard() : "_______________"))
              : "_______________";
      String clientContact =
          client != null && client.getContactPhone() != null
              ? client.getContactPhone()
              : "_______________";

      document.add(
          new Paragraph("委托人：" + clientName)
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_VERY_SMALL)); // 减小间距

      document.add(
          new Paragraph(clientType + "：" + clientIdValue)
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_VERY_SMALL)); // 减小间距

      document.add(
          new Paragraph("联系电话：" + clientContact)
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_MEDIUM)); // 减小间距

      // 受托人信息
      String firmNameDisplay =
          firmName != null && !firmName.isEmpty() ? firmName : "_______________";
      String lawyerNameDisplay =
          lawyerName != null && !lawyerName.isEmpty() ? lawyerName : "_______________";
      String licenseNoDisplay =
          lawyerLicenseNo != null && !lawyerLicenseNo.isEmpty()
              ? lawyerLicenseNo
              : "_______________";

      document.add(
          new Paragraph("受托人：" + firmNameDisplay + " " + lawyerNameDisplay + " 律师")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_VERY_SMALL)); // 减小间距

      document.add(
          new Paragraph("执业证号：" + licenseNoDisplay)
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_MEDIUM)); // 减小间距

      // 委托事项
      String matterName =
          matter != null && matter.getName() != null ? matter.getName() : "_______________";
      String caseType =
          matter != null && matter.getCaseType() != null
              ? getCaseTypeText(matter.getCaseType())
              : "_______________";

      document.add(
          new Paragraph("委托事项：")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setBold()
              .setMarginBottom(MARGIN_VERY_SMALL)); // 减小间距

      document.add(
          new Paragraph("    本人因" + matterName + "（" + caseType + "）一案，特委托上述受托人作为本人的诉讼代理人。")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(MARGIN_MEDIUM)); // 减小间距

      // 代理权限
      document.add(
          new Paragraph("代理权限：")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setBold()
              .setMarginBottom(MARGIN_VERY_SMALL)); // 减小间距

      String[] permissions = {
        "1. 一般代理（代为起诉、应诉、变更诉讼请求、提供证据、进行辩论、申请调解）",
        "2. 特别代理（□ 代为承认、放弃、变更诉讼请求 □ 代为和解 □ 代为提起上诉 □ 代收法律文书）"
      };
      for (String perm : permissions) {
        document.add(
            new Paragraph("    " + perm).setFont(font).setFontSize(10).setMarginBottom(3)); // 减小间距
      }

      // 委托期限
      document.add(
          new Paragraph("委托期限：自签署之日起至本案结案止。")
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginTop(8)
              .setMarginBottom(MARGIN_DATE_BOTTOM)); // 减小间距

      // 签章区域（使用表格并排显示）
      Table signTable =
          new Table(UnitValue.createPercentArray(new float[] {WIDTH_RATIO_HALF, WIDTH_RATIO_HALF}))
              .setWidth(UnitValue.createPercentValue(100))
              .setMarginTop(MARGIN_TITLE_TOP_MEDIUM);

      signTable.addCell(
          new Cell()
              .add(
                  new Paragraph("委托人（签章）：\n\n\n日期：     年   月   日")
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));
      signTable.addCell(
          new Cell()
              .add(
                  new Paragraph("受托人（确认）：\n\n\n日期：     年   月   日")
                      .setFont(font)
                      .setFontSize(FONT_SIZE_NORMAL))
              .setBorder(null));

      document.add(signTable);

      // 页脚
      document.add(
          new Paragraph("本授权委托书由系统自动生成，仅供参考，请以签字盖章版本为准。")
              .setFont(font)
              .setFontSize(FONT_SIZE_TINY)
              .setFontColor(ColorConstants.GRAY)
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginTop(WIDTH_RATIO_SIMPLE_LABEL));

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("生成授权委托书PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 创建中文字体
   *
   * @return PDF字体对象
   */
  private PdfFont createChineseFont() {
    try {
      // 尝试加载内置字体
      return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
    } catch (Exception e) {
      log.warn("无法加载STSong字体，尝试使用系统字体", e);
      try {
        // 尝试加载系统字体
        return PdfFontFactory.createFont(
            "/System/Library/Fonts/STHeiti Light.ttc,0", PdfEncodings.IDENTITY_H);
      } catch (Exception ex) {
        log.error("无法加载中文字体", ex);
        throw new RuntimeException("无法加载中文字体", ex);
      }
    }
  }

  /**
   * 添加表格行（4列）
   *
   * @param table 表格对象
   * @param font 字体
   * @param label1 标签1
   * @param value1 值1
   * @param label2 标签2
   * @param value2 值2
   */
  private void addTableRow(
      final Table table,
      final PdfFont font,
      final String label1,
      final String value1,
      final String label2,
      final String value2) {
    table.addCell(createLabelCell(label1, font));
    table.addCell(createValueCell(value1, font));
    table.addCell(createLabelCell(label2, font));
    table.addCell(createValueCell(value2, font));
  }

  /**
   * 添加简单行（2列）
   *
   * @param table 表格对象
   * @param font 字体
   * @param label 标签
   * @param value 值
   */
  private void addSimpleRow(
      final Table table, final PdfFont font, final String label, final String value) {
    table.addCell(createLabelCell(label, font));
    table.addCell(createValueCell(value, font));
  }

  /**
   * 创建标签单元格
   *
   * @param text 文本内容
   * @param font 字体
   * @return 单元格对象
   */
  private Cell createLabelCell(final String text, final PdfFont font) {
    return new Cell()
        .add(new Paragraph(text).setFont(font).setFontSize(FONT_SIZE_NORMAL))
        .setBackgroundColor(HEADER_BG_COLOR)
        .setTextAlignment(TextAlignment.CENTER)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .setPadding(5);
  }

  /**
   * 创建值单元格
   *
   * @param text 文本内容
   * @param font 字体
   * @return 单元格对象
   */
  private Cell createValueCell(final String text, final PdfFont font) {
    return new Cell()
        .add(new Paragraph(text != null ? text : "-").setFont(font).setFontSize(FONT_SIZE_NORMAL))
        .setTextAlignment(TextAlignment.LEFT)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .setPadding(5);
  }

  /**
   * 获取审批状态文本
   *
   * @param status 审批状态
   * @return 状态文本
   */
  private String getApprovalStatusText(final String status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case "PENDING" -> "待审批";
      case "APPROVED" -> "已通过";
      case "REJECTED" -> "已拒绝";
      case "CANCELLED" -> "已撤销";
      default -> status;
    };
  }

  /**
   * 获取案件类型文本
   *
   * @param caseType 案件类型
   * @return 类型文本
   */
  private String getCaseTypeText(final String caseType) {
    if (caseType == null) {
      return "其他";
    }
    return switch (caseType.toUpperCase()) {
      case "CIVIL" -> "民事案件";
      case "CRIMINAL" -> "刑事案件";
      case "ADMINISTRATIVE" -> "行政案件";
      case "BANKRUPTCY" -> "破产案件";
      case "IP" -> "知识产权案件";
      case "ARBITRATION" -> "仲裁案件";
      case "ENFORCEMENT" -> "执行案件";
      case "LEGAL_COUNSEL" -> "法律顾问";
      case "SPECIAL_SERVICE" -> "专项服务";
      default -> caseType;
    };
  }

  /**
   * 添加全宽行（标题占1列，内容占3列）
   *
   * @param table 表格对象
   * @param font 字体
   * @param label 标签
   * @param value 值
   */
  private void addFullWidthRow(
      final Table table, final PdfFont font, final String label, final String value) {
    table.addCell(createLabelCell(label, font));
    table.addCell(new Cell(1, 3).add(createValueCell(value, font)));
  }

  /**
   * 添加两列行（标题1-内容1，标题2-内容2）
   *
   * @param table 表格对象
   * @param font 字体
   * @param label1 标签1
   * @param value1 值1
   * @param label2 标签2
   * @param value2 值2
   */
  private void addTwoColumnRow(
      final Table table,
      final PdfFont font,
      final String label1,
      final String value1,
      final String label2,
      final String value2) {
    table.addCell(createLabelCell(label1, font));
    table.addCell(createValueCell(value1, font));
    table.addCell(createLabelCell(label2, font));
    table.addCell(createValueCell(value2, font));
  }

  /**
   * 格式化日期为中文格式（yyyy年MM月dd日）
   *
   * @param date 日期
   * @return 格式化后的日期字符串
   */
  private String formatDateChinese(final java.time.LocalDate date) {
    if (date == null) {
      return "____年____月____日";
    }
    return date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA));
  }

  /**
   * 格式化日期时间为中文格式（yyyy年MM月dd日）
   *
   * @param dateTime 日期时间
   * @return 格式化后的日期字符串
   */
  private String formatDateTimeChinese(final java.time.LocalDateTime dateTime) {
    if (dateTime == null) {
      return "____年____月____日";
    }
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA));
  }

  /**
   * 去除HTML标签
   *
   * @param html HTML字符串
   * @return 纯文本字符串
   */
  private String stripHtml(final String html) {
    if (html == null) {
      return "";
    }
    return html.replaceAll("<[^>]*>", "")
        .replaceAll("&nbsp;", " ")
        .replaceAll("&lt;", "<")
        .replaceAll("&gt;", ">")
        .replaceAll("&amp;", "&")
        .trim();
  }

  /**
   * 从模板内容生成 PDF 支持 HTML 富文本模板（已替换变量）
   *
   * <p>支持的HTML格式： - &lt;p&gt; 段落 - &lt;strong&gt;/&lt;b&gt; 加粗 - &lt;em&gt;/&lt;i&gt; 斜体 -
   * &lt;u&gt; 下划线 - &lt;center&gt; 居中 - &lt;h1&gt;-&lt;h4&gt; 标题 - &lt;br&gt; 换行
   *
   * @param title 文档标题
   * @param content 已替换变量的模板内容（支持HTML格式）
   * @return PDF 字节数组
   */
  public byte[] generatePdfFromTemplateContent(final String title, final String content) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      document.setMargins(WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF, WIDTH_RATIO_HALF);

      PdfFont font = createChineseFont();

      // 标题（如果提供）
      if (title != null && !title.isEmpty()) {
        document.add(
            new Paragraph(title)
                .setFont(font)
                .setFontSize(FONT_SIZE_LARGE_TITLE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(MARGIN_SECTION_TOP));
      }

      // 内容 - 解析HTML格式
      if (content != null && !content.isEmpty()) {
        parseHtmlContent(document, content, font);
      }

      // 页脚
      document.add(
          new Paragraph("本文档由系统自动生成，仅供参考，请以签字盖章版本为准。")
              .setFont(font)
              .setFontSize(FONT_SIZE_TINY)
              .setFontColor(ColorConstants.GRAY)
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginTop(WIDTH_RATIO_SIMPLE_LABEL));

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("从模板内容生成PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 解析HTML内容并添加到PDF文档
   *
   * @param document PDF文档对象
   * @param htmlContent HTML内容
   * @param font 字体
   */
  private void parseHtmlContent(
      final Document document, final String htmlContent, final PdfFont font) {
    // 预处理：规范化HTML
    String content =
        htmlContent
            .replaceAll("<br\\s*/?>", "\n") // <br> 转换为换行
            .replaceAll("</p>", "</p>\n") // 段落后加换行
            .replaceAll("</div>", "</div>\n") // div后加换行
            .replaceAll("</h[1-6]>", "</h>\n") // 标题后加换行
            .replaceAll("&nbsp;", " ") // 空格
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&")
            .replaceAll("&quot;", "\"");

    // 按块解析（段落、标题等）
    String[] blocks =
        content.split(
            "(?=<p[^>]*>)|(?=<h[1-6][^>]*>)|(?=<div[^>]*>)|(?<=</p>)|(?<=</h>)|(?<=</div>)|\n");

    for (String block : blocks) {
      if (block.trim().isEmpty()) {
        continue;
      }

      Paragraph para = new Paragraph();
      para.setFont(font);
      para.setMarginBottom(6);

      // 检测块级格式
      boolean isCentered =
          block.contains("text-align: center")
              || block.contains("text-align:center")
              || block.matches("(?s).*<center>.*</center>.*");
      boolean isRight = block.contains("text-align: right") || block.contains("text-align:right");
      boolean isHeading = block.matches("(?s).*<h[1-6][^>]*>.*");

      // 设置对齐方式
      if (isCentered) {
        para.setTextAlignment(TextAlignment.CENTER);
      } else if (isRight) {
        para.setTextAlignment(TextAlignment.RIGHT);
      } else {
        para.setTextAlignment(TextAlignment.LEFT);
      }

      // 设置字号
      float fontSize = FONT_SIZE_NORMAL;
      if (block.matches("(?s).*<h1[^>]*>.*")) {
        fontSize = FONT_SIZE_LARGE_TITLE;
      } else if (block.matches("(?s).*<h2[^>]*>.*")) {
        fontSize = FONT_SIZE_TITLE;
      } else if (block.matches("(?s).*<h3[^>]*>.*")) {
        fontSize = FONT_SIZE_SUBTITLE;
      } else if (block.matches("(?s).*<h4[^>]*>.*")) {
        fontSize = FONT_SIZE_NORMAL_LARGE;
      }
      para.setFontSize(fontSize);

      // 标题加粗
      if (isHeading) {
        para.setBold();
        para.setMarginTop(MARGIN_SUBTITLE_TOP);
        para.setMarginBottom(MARGIN_MEDIUM);
      }

      // 解析内联格式
      String text = stripHtmlTags(block);
      if (text.trim().isEmpty()) {
        continue;
      }

      // 检测内联格式并应用
      boolean isBold =
          block.contains("<strong>")
              || block.contains("<b>")
              || block.contains("font-weight: bold")
              || block.contains("font-weight:bold");
      boolean isItalic = block.contains("<em>") || block.contains("<i>");
      boolean isUnderline =
          block.contains("<u>")
              || block.contains("text-decoration: underline")
              || block.contains("text-decoration:underline");

      if (isBold) {
        para.setBold();
      }
      if (isItalic) {
        para.setItalic();
      }
      if (isUnderline) {
        para.setUnderline();
      }

      // 检测首行缩进
      if (block.contains("text-indent") || text.startsWith("    ") || text.startsWith("\t")) {
        para.setFirstLineIndent(FIRST_LINE_INDENT);
        text = text.stripLeading();
      }

      para.add(text);
      document.add(para);
    }
  }

  /**
   * 去除HTML标签，保留纯文本
   *
   * @param html HTML字符串
   * @return 纯文本字符串
   */
  private String stripHtmlTags(final String html) {
    if (html == null) {
      return "";
    }
    return html.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
  }

  /**
   * 从分块模板生成授权委托书PDF
   *
   * <p>固定格式： - 标题：二号宋体（22pt），居中 - 正文：三号仿宋（16pt） - 备注：五号宋体（10.5pt）
   *
   * <p>支持两种格式： 1. 新结构化格式：{ "_structured": true, "blocks": { "title": {...}, "client": "...", ... }
   * } 2. 旧格式：{ "title": "...", "clientInfo": "...", ... }
   *
   * @param templateContent JSON格式的分块模板内容
   * @return PDF字节数组
   */
  public byte[] generatePowerOfAttorneyFromBlocks(final String templateContent) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);
      // 减小边距，增加可用空间，确保内容能在一页内显示
      document.setMargins(
          WIDTH_RATIO_SIGN_LABEL, MARGIN_RIGHT, WIDTH_RATIO_SIGN_LABEL, MARGIN_RIGHT);

      // 创建中文字体
      PdfFont songFont = createChineseFont();
      PdfFont fangsongFont = createFangsongFont();

      // 解析JSON分块内容
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(templateContent);

      // 检查是否是新的结构化格式
      JsonNode blocks;
      if (rootNode.has("_structured") && rootNode.has("blocks")) {
        // 新格式：{ "_structured": true, "blocks": {...} }
        blocks = rootNode.get("blocks");
      } else {
        // 旧格式：直接是 blocks 对象
        blocks = rootNode;
      }

      // 标题：二号宋体（22pt），居中
      // 新格式：blocks.title.documentTitle，旧格式：blocks.title
      String title = getNestedBlockContent(blocks, "title", "documentTitle", null);
      if (title == null || title.isEmpty()) {
        title = getBlockContent(blocks, "title", "授 权 委 托 书");
      }
      if (title == null || title.isEmpty()) {
        title = "授 权 委 托 书";
      }
      document.add(
          new Paragraph(title)
              .setFont(songFont)
              .setFontSize(FONT_SIZE_XLARGE_TITLE) // 二号字体约22pt
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginBottom(MARGIN_SECTION_TOP)); // 减小标题底部间距

      // 委托人信息：三号仿宋
      // 新格式：blocks.client，旧格式：blocks.clientInfo
      String clientInfo = getBlockContent(blocks, "client", "");
      if (clientInfo.isEmpty()) {
        clientInfo = getBlockContent(blocks, "clientInfo", "");
      }
      if (!clientInfo.isEmpty()) {
        addFormattedParagraphs(document, fangsongFont, 16, clientInfo);
        document.add(new Paragraph().setMarginBottom(MARGIN_MEDIUM)); // 减小间距
      }

      // 受托人信息：三号仿宋
      // 新格式：blocks.agent，旧格式：blocks.agentInfo
      String agentInfo = getBlockContent(blocks, "agent", "");
      if (agentInfo.isEmpty()) {
        agentInfo = getBlockContent(blocks, "agentInfo", "");
      }
      if (!agentInfo.isEmpty()) {
        addFormattedParagraphs(document, fangsongFont, 16, agentInfo);
        document.add(new Paragraph().setMarginBottom(MARGIN_MEDIUM)); // 减小间距
      }

      // 委托事项：三号仿宋
      // 新格式：blocks.matter 包含所有事项内容，旧格式：blocks.matterInfo, blocks.authorization, blocks.duration
      String matter = getBlockContent(blocks, "matter", "");
      if (!matter.isEmpty()) {
        // 新格式：委托事项、代理权限、委托期限都在 matter 中
        addFormattedParagraphs(document, fangsongFont, 16, matter);
        document.add(new Paragraph().setMarginBottom(MARGIN_MEDIUM)); // 减小间距
      } else {
        // 旧格式：分别处理各项
        String matterInfo = getBlockContent(blocks, "matterInfo", "");
        if (!matterInfo.isEmpty()) {
          addFormattedParagraphs(document, fangsongFont, 16, matterInfo);
          document.add(new Paragraph().setMarginBottom(MARGIN_MEDIUM)); // 减小间距
        }

        String authorization = getBlockContent(blocks, "authorization", "");
        if (!authorization.isEmpty()) {
          addFormattedParagraphs(document, fangsongFont, 16, authorization);
          document.add(new Paragraph().setMarginBottom(MARGIN_MEDIUM)); // 减小间距
        }

        String duration = getBlockContent(blocks, "duration", "");
        if (!duration.isEmpty()) {
          addFormattedParagraphs(document, fangsongFont, 16, duration);
          document.add(new Paragraph().setMarginBottom(MARGIN_PARAGRAPH_SPACING)); // 减小间距
        }
      }

      // 签字落款：三号仿宋
      // 新格式：blocks.signature.clientSign, blocks.signature.signDate，旧格式：blocks.signature
      String signature = getNestedBlockContent(blocks, "signature", "clientSign", null);
      String signDate = getNestedBlockContent(blocks, "signature", "signDate", null);

      if (signature != null && !signature.isEmpty()) {
        // 新格式：分别处理签署和日期
        addFormattedParagraphs(document, fangsongFont, 16, signature);
        if (signDate != null && !signDate.isEmpty()) {
          addFormattedParagraphs(document, fangsongFont, 16, signDate);
        }
      } else {
        // 旧格式：合并处理
        signature =
            getBlockContent(blocks, "signature", "委托人（签章）：________________\n\n日    期：    年  月  日");
        addFormattedParagraphs(document, fangsongFont, 16, signature);
      }
      document.add(new Paragraph().setMarginBottom(10)); // 减小间距

      // 分隔线
      document.add(
          new Paragraph("━".repeat(SEPARATOR_REPEAT_COUNT))
              .setFont(songFont)
              .setFontSize(10)
              .setTextAlignment(TextAlignment.CENTER)
              .setFontColor(ColorConstants.GRAY)
              .setMarginTop(MARGIN_SUBTITLE_TOP)
              .setMarginBottom(10));

      // 备注：五号宋体（10.5pt）
      // 新格式：blocks.signature.remarks，旧格式：blocks.remarks
      String remarks = getNestedBlockContent(blocks, "signature", "remarks", null);
      if (remarks == null || remarks.isEmpty()) {
        remarks = getBlockContent(blocks, "remarks", "【本授权委托书由系统自动生成，以签字盖章版本为准】");
      }
      if (remarks == null || remarks.isEmpty()) {
        remarks = "【本授权委托书由系统自动生成，以签字盖章版本为准】";
      }
      addFormattedParagraphs(document, songFont, FONT_SIZE_SONG_5, remarks);

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("从分块模板生成授权委托书PDF失败", e);
      throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取分块内容（支持字符串值）
   *
   * @param blocks JSON节点对象
   * @param key 键名
   * @param defaultValue 默认值
   * @return 内容字符串
   */
  private String getBlockContent(
      final JsonNode blocks, final String key, final String defaultValue) {
    if (blocks.has(key)) {
      JsonNode node = blocks.get(key);
      // 如果是字符串节点
      if (node.isTextual()) {
        String value = node.asText();
        return value != null && !value.isEmpty() ? value : defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * 获取嵌套分块内容（支持对象嵌套） 例如：blocks.signature.clientSign 或 blocks.title.documentTitle
   *
   * @param blocks JSON节点对象
   * @param parentKey 父键名
   * @param childKey 子键名
   * @param defaultValue 默认值
   * @return 内容字符串
   */
  private String getNestedBlockContent(
      final JsonNode blocks,
      final String parentKey,
      final String childKey,
      final String defaultValue) {
    if (blocks.has(parentKey)) {
      JsonNode parentNode = blocks.get(parentKey);
      if (parentNode.isObject() && parentNode.has(childKey)) {
        JsonNode childNode = parentNode.get(childKey);
        if (childNode.isTextual()) {
          String value = childNode.asText();
          return value != null && !value.isEmpty() ? value : defaultValue;
        }
      }
    }
    return defaultValue;
  }

  /**
   * 添加格式化段落（按换行分割）
   *
   * @param document PDF文档对象
   * @param font 字体
   * @param fontSize 字体大小
   * @param content 内容
   */
  private void addFormattedParagraphs(
      final Document document, final PdfFont font, final float fontSize, final String content) {
    if (content == null || content.isEmpty()) {
      return;
    }

    String[] lines = content.split("\n");
    for (String line : lines) {
      // 检测首行缩进
      boolean hasIndent = line.startsWith("    ") || line.startsWith("\t");
      String text = line.stripLeading();

      // 处理空变量为下划线
      text = text.replaceAll("\\$\\{[^}]+\\}", "________________");

      if (text.isEmpty()) {
        document.add(new Paragraph().setMarginBottom(MARGIN_VERY_SMALL));
        continue;
      }

      Paragraph para =
          new Paragraph(text)
              .setFont(font)
              .setFontSize(fontSize)
              .setMarginBottom(MARGIN_VERY_SMALL);

      if (hasIndent) {
        para.setFirstLineIndent(FIRST_LINE_INDENT_DOUBLE); // 两个汉字宽度
      }

      document.add(para);
    }
  }

  /**
   * 创建仿宋字体
   *
   * @return PDF字体对象
   */
  private PdfFont createFangsongFont() {
    try {
      // 尝试使用内置仿宋字体
      return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
    } catch (Exception e) {
      log.warn("无法加载仿宋字体，使用默认中文字体", e);
      return createChineseFont();
    }
  }

  /**
   * 检查模板内容是否为JSON分块格式 支持新格式：{ "_structured": true, "blocks": {...} } 支持旧格式：{ "title": "...",
   * "clientInfo": "...", ... }
   *
   * @param content 模板内容
   * @return 是否为分块格式
   */
  public boolean isBlockTemplateFormat(final String content) {
    if (content == null || content.isEmpty()) {
      return false;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(content);

      // 检查是否是新的结构化格式
      if (node.has("_structured") && node.has("blocks")) {
        JsonNode blocks = node.get("blocks");
        // 检查是否包含授权委托书的典型分块字段
        return blocks.has("title")
            || blocks.has("client")
            || blocks.has("agent")
            || blocks.has("clientInfo")
            || blocks.has("agentInfo");
      }

      // 检查是否是旧格式（直接是 blocks 对象）
      // 检查是否包含授权委托书的典型分块字段
      return node.has("title")
          || node.has("clientInfo")
          || node.has("agentInfo")
          || node.has("client")
          || node.has("agent");
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 添加默认合同条款
   *
   * @param document PDF文档对象
   * @param font 字体
   * @param contract 合同信息
   * @param matter 项目信息
   */
  private void addDefaultContractTerms(
      final Document document, final PdfFont font, final Contract contract, final Matter matter) {
    String[] terms = {
      "第一条 委托事项",
      "    甲方因"
          + (matter != null && matter.getName() != null ? matter.getName() : "_______________")
          + "事宜，委托乙方提供法律服务。",
      "",
      "第二条 代理权限",
      "    乙方接受甲方委托，指派律师担任甲方的诉讼代理人/法律顾问，代理权限为：一般代理。",
      "",
      "第三条 律师费用",
      "    甲方应向乙方支付律师服务费人民币"
          + (contract != null && contract.getTotalAmount() != null
              ? String.format("%.2f", contract.getTotalAmount())
              : "_______________")
          + "元。",
      "",
      "第四条 付款方式",
      "    "
          + (contract != null && contract.getPaymentTerms() != null
              ? contract.getPaymentTerms()
              : "按约定付款"),
      "",
      "第五条 双方权利义务",
      "    1. 甲方应如实向乙方陈述案情，提供相关证据材料。",
      "    2. 乙方应依法维护甲方的合法权益，保守甲方的商业秘密。",
      "",
      "第六条 合同期限",
      "    本合同自双方签字盖章之日起生效，至委托事项办结时终止。"
    };

    for (String term : terms) {
      document.add(
          new Paragraph(term)
              .setFont(font)
              .setFontSize(FONT_SIZE_NORMAL)
              .setMarginBottom(term.isEmpty() ? 10 : 3));
    }
  }
}
