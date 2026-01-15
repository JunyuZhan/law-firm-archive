package com.lawfirm.infrastructure.external.document;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 卷宗封面生成器
 * 生成标准律师事务所业务档案卷宗封面PDF（牛皮纸风格）
 * 
 * 支持两种模板：
 * - 民事/行政/仲裁案件模板：包含当事人（原告、被告、第三人）
 * - 刑事案件模板：包含被告人
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DossierCoverGenerator {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SysConfigAppService sysConfigAppService;
    private final CauseOfActionService causeOfActionService;

    // 牛皮纸颜色（RGB: 210, 180, 140）- 更接近真实牛皮纸
    private static final DeviceRgb KRAFT_PAPER_COLOR = new DeviceRgb(210, 180, 140);
    // 深棕色文字（RGB: 80, 50, 20）
    private static final DeviceRgb DARK_BROWN = new DeviceRgb(80, 50, 20);
    // 边框颜色（深棕色）
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(60, 40, 20);
    
    // 边框宽度
    private static final float BORDER_WIDTH = 0.5f;
    // 单元格内边距
    private static final float CELL_PADDING = 6f;
    // 字体大小
    private static final float FONT_SIZE_TITLE = 24f;
    private static final float FONT_SIZE_SUBTITLE = 14f;
    private static final float FONT_SIZE_NORMAL = 12f;
    private static final float FONT_SIZE_SMALL = 10f;

    /**
     * 生成卷宗封面PDF
     */
    public byte[] generateCover(Matter matter, String archiveNo) {
        return generateCover(matter, archiveNo, null, null, null);
    }
    
    /**
     * 生成卷宗封面PDF（完整版本）
     */
    public byte[] generateCover(Matter matter, String archiveNo, Integer volumeCount, 
                                 Integer pageCount, String retentionPeriod) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            
            Document document = new Document(pdf, PageSize.A4);
            // 设置页边距（上、右、下、左）
            document.setMargins(40, 50, 40, 50);
            document.setBackgroundColor(KRAFT_PAPER_COLOR);
            
            PdfFont font = getChineseFont();
            
            // 根据案件类型选择模板
            String caseType = matter.getCaseType();
            if ("CRIMINAL".equals(caseType)) {
                generateCriminalCover(document, matter, archiveNo, volumeCount, pageCount, retentionPeriod, font);
            } else {
                generateCivilCover(document, matter, archiveNo, volumeCount, pageCount, retentionPeriod, font);
            }
            
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("生成卷宗封面失败", e);
            throw new RuntimeException("生成卷宗封面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成民事/行政/仲裁案件封面
     */
    private void generateCivilCover(Document document, Matter matter, String archiveNo,
                                     Integer volumeCount, Integer pageCount, String retentionPeriod, PdfFont font) {
        // 律所名称
        String firmName = getFirmName();
        if (firmName != null && !firmName.isEmpty()) {
            Paragraph firmPara = new Paragraph(firmName)
                    .setFont(font)
                    .setFontSize(FONT_SIZE_SUBTITLE)
                    .setFontColor(DARK_BROWN)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            document.add(firmPara);
        }
        
        // 主标题：业务档案卷宗
        Paragraph title = new Paragraph("业 务 档 案 卷 宗")
                .setFont(font)
                .setFontSize(FONT_SIZE_TITLE)
                .setBold()
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);
        
        // 年度字号行
        String yearNo = buildYearNo(matter, archiveNo);
        Paragraph yearPara = new Paragraph(yearNo)
                .setFont(font)
                .setFontSize(FONT_SIZE_SUBTITLE)
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(25);
        document.add(yearPara);
        
        // 创建主表格
        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth();
        
        // 案由行（转换代码为名称）
        addFullWidthRow(mainTable, "案    由", getCauseOfActionName(matter), font);
        
        // 委托人 | 承办人
        addTwoColumnRow(mainTable, "委 托 人", getClientName(matter.getClientId()),
                       "承 办 人", getLawyerName(matter.getLeadLawyerId()), font);
        
        // 当事人区域
        addPartySection(mainTable, matter, font);
        
        // 审理法院
        addFullWidthRow(mainTable, "审理法院", "", font); // 暂无此字段，留空
        
        // 收案日期 | 结案日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String filingDateStr = matter.getFilingDate() != null ? matter.getFilingDate().format(formatter) : "    年  月  日";
        String closingDateStr = matter.getActualClosingDate() != null ? matter.getActualClosingDate().format(formatter) : "    年  月  日";
        addTwoColumnRow(mainTable, "收案日期", filingDateStr, "结案日期", closingDateStr, font);
        
        // 审/仲裁/结果区域
        addResultSection(mainTable, matter, font);
        
        // 归档日期 | 保存期限
        String archiveDateStr = LocalDate.now().format(formatter);
        String periodStr = getRetentionPeriodName(retentionPeriod);
        addTwoColumnRow(mainTable, "归档日期", archiveDateStr, "保存期限", periodStr, font);
        
        // 立卷人 | 备注
        addTwoColumnRow(mainTable, "立 卷 人", "", "备    注", 
                       matter.getRemark() != null ? truncate(matter.getRemark(), 15) : "", font);
        
        document.add(mainTable);
    }

    /**
     * 生成刑事案件封面
     */
    private void generateCriminalCover(Document document, Matter matter, String archiveNo,
                                        Integer volumeCount, Integer pageCount, String retentionPeriod, PdfFont font) {
        // 律所名称
        String firmName = getFirmName();
        if (firmName != null && !firmName.isEmpty()) {
            Paragraph firmPara = new Paragraph(firmName)
                    .setFont(font)
                    .setFontSize(FONT_SIZE_SUBTITLE)
                    .setFontColor(DARK_BROWN)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            document.add(firmPara);
        }
        
        // 主标题
        Paragraph title = new Paragraph("业 务 档 案 卷 宗")
                .setFont(font)
                .setFontSize(FONT_SIZE_TITLE)
                .setBold()
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);
        
        // 副标题：（刑事诉讼类）
        Paragraph subTitle = new Paragraph("（刑事诉讼类）")
                .setFont(font)
                .setFontSize(FONT_SIZE_SUBTITLE)
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(25);
        document.add(subTitle);
        
        // 创建主表格
        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth();
        
        // 类别 | 年度
        String yearStr = matter.getCreatedAt() != null ? String.valueOf(matter.getCreatedAt().getYear()) : "____";
        addTwoColumnRow(mainTable, "类    别", "刑事诉讼", "年    度", yearStr, font);
        
        // 承办律师
        addFullWidthRow(mainTable, "承办律师", getLawyerName(matter.getLeadLawyerId()), font);
        
        // 被告人（刑事案件的对方当事人就是被告人）
        addFullWidthRow(mainTable, "被 告 人", matter.getOpposingParty(), font);
        
        // 案由 | 第___号
        String noStr = archiveNo != null ? archiveNo : "第    号";
        addTwoColumnRow(mainTable, "案    由", getCauseOfActionName(matter), "", noStr, font);
        
        // 收案日期 | 委托人
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String filingDateStr = matter.getFilingDate() != null ? matter.getFilingDate().format(formatter) : "    年  月  日";
        addTwoColumnRow(mainTable, "收案日期", filingDateStr, "委 托 人", getClientName(matter.getClientId()), font);
        
        // 审理法院
        addFullWidthRow(mainTable, "审理法院", "", font);
        
        // 法院收案号 | 结案日期
        String closingDateStr = matter.getActualClosingDate() != null ? matter.getActualClosingDate().format(formatter) : "    年  月  日";
        addTwoColumnRow(mainTable, "法院收案号", "", "结案日期", closingDateStr, font);
        
        // 审（办）理结果 | 审级
        String stageName = MatterConstants.getLitigationStageName(matter.getLitigationStage());
        addTwoColumnRow(mainTable, "审理结果", matter.getOutcome() != null ? truncate(matter.getOutcome(), 10) : "", 
                       "审    级", stageName != null ? stageName : "", font);
        
        // 归档日期 | 保存期限
        String archiveDateStr = LocalDate.now().format(formatter);
        String periodStr = getRetentionPeriodName(retentionPeriod);
        addTwoColumnRow(mainTable, "归档日期", archiveDateStr, "保存期限", periodStr, font);
        
        // 立卷人
        addFullWidthRow(mainTable, "立 卷 人", "", font);
        
        document.add(mainTable);
    }

    /**
     * 添加当事人区域（原告、被告、第三人）
     */
    private void addPartySection(Table table, Matter matter, PdfFont font) {
        // 当事人标签单元格（跨3行）- 使用构造函数指定rowspan
        Cell labelCell = new Cell(3, 1)  // rowspan=3, colspan=1
                .add(new Paragraph("当\n事\n人")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(labelCell);
        
        // 原告行
        Cell plaintiffLabel = createLabelCell("原告", font);
        Cell plaintiffValue = new Cell(1, 2)  // colspan=2
                .add(new Paragraph(getClientName(matter.getClientId()))
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(plaintiffLabel);
        table.addCell(plaintiffValue);
        
        // 被告行
        Cell defendantLabel = createLabelCell("被告", font);
        Cell defendantValue = new Cell(1, 2)  // colspan=2
                .add(new Paragraph(matter.getOpposingParty() != null ? matter.getOpposingParty() : "")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(defendantLabel);
        table.addCell(defendantValue);
        
        // 第三人行
        Cell thirdPartyLabel = createLabelCell("第三人", font);
        Cell thirdPartyValue = new Cell(1, 2)  // colspan=2
                .add(new Paragraph("")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(thirdPartyLabel);
        table.addCell(thirdPartyValue);
    }

    /**
     * 添加审/仲裁/结果区域
     */
    private void addResultSection(Table table, Matter matter, PdfFont font) {
        // 审/仲裁/结果标签（跨2行）
        Cell labelCell = new Cell(2, 1)  // rowspan=2, colspan=1
                .add(new Paragraph("审\n仲裁\n结果")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(labelCell);
        
        // 一审结果 | 再审结果
        Cell firstLabel = createLabelCell("一审结果", font);
        Cell firstValue = createValueCell(matter.getOutcome() != null ? truncate(matter.getOutcome(), 10) : "", font);
        table.addCell(firstLabel);
        table.addCell(firstValue);
        
        // 二审/再审
        Cell secondLabel = createLabelCell("再审结果", font);
        Cell secondValue = createValueCell("", font);
        table.addCell(secondLabel);
        table.addCell(secondValue);
    }

    /**
     * 添加全宽行（标签+值）
     */
    private void addFullWidthRow(Table table, String label, String value, PdfFont font) {
        Cell labelCell = createLabelCell(label, font);
        Cell valueCell = new Cell(1, 3)  // colspan=3
                .add(new Paragraph(value != null ? value : "")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * 添加两列行（标签1+值1 | 标签2+值2）
     */
    private void addTwoColumnRow(Table table, String label1, String value1, 
                                  String label2, String value2, PdfFont font) {
        table.addCell(createLabelCell(label1, font));
        table.addCell(createValueCell(value1, font));
        table.addCell(createLabelCell(label2, font));
        table.addCell(createValueCell(value2, font));
    }

    /**
     * 创建标签单元格
     */
    private Cell createLabelCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
    }

    /**
     * 创建值单元格
     */
    private Cell createValueCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "")
                        .setFont(font)
                        .setFontSize(FONT_SIZE_NORMAL)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, BORDER_WIDTH))
                .setPadding(CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
    }

    /**
     * 构建年度字号
     */
    private String buildYearNo(Matter matter, String archiveNo) {
        int year = matter.getCreatedAt() != null ? matter.getCreatedAt().getYear() : LocalDate.now().getYear();
        String yearCn = convertToChineseYear(year);
        String no = archiveNo != null ? archiveNo : "    ";
        return yearCn + "年度        字第" + no + "号";
    }

    /**
     * 年份转中文
     */
    private String convertToChineseYear(int year) {
        String[] cnNumbers = {"〇", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        StringBuilder sb = new StringBuilder();
        String yearStr = String.valueOf(year);
        for (char c : yearStr.toCharArray()) {
            sb.append(cnNumbers[c - '0']);
        }
        return sb.toString();
    }

    /**
     * 获取中文字体
     */
    private PdfFont getChineseFont() {
        try {
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont("SimSun", "UniGB-UCS2-H");
            } catch (Exception e2) {
                log.warn("无法加载中文字体，使用默认字体", e2);
                try {
                    return PdfFontFactory.createFont();
                } catch (Exception e3) {
                    throw new RuntimeException("无法创建字体", e3);
                }
            }
        }
    }

    /**
     * 获取律所名称
     */
    private String getFirmName() {
        try {
            return sysConfigAppService.getConfigValue("firm.name");
        } catch (Exception e) {
            log.warn("获取律所名称失败", e);
            return null;
        }
    }

    /**
     * 获取律师姓名
     */
    private String getLawyerName(Long lawyerId) {
        if (lawyerId == null) return "";
        try {
            User lawyer = userRepository.findById(lawyerId);
            if (lawyer != null) {
                return lawyer.getRealName() != null ? lawyer.getRealName() : lawyer.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取律师姓名失败: lawyerId={}", lawyerId, e);
        }
        return "";
    }

    /**
     * 获取客户名称
     */
    private String getClientName(Long clientId) {
        if (clientId == null) return "";
        try {
            Client client = clientRepository.findById(clientId);
            if (client != null) {
                return client.getName();
            }
        } catch (Exception e) {
            log.warn("获取客户名称失败: clientId={}", clientId, e);
        }
        return "";
    }

    /**
     * 获取保管期限名称
     */
    private String getRetentionPeriodName(String retentionPeriod) {
        if (retentionPeriod == null) return "";
        switch (retentionPeriod) {
            case "PERMANENT": return "永久";
            case "30_YEARS": return "30年";
            case "20_YEARS": return "20年";
            case "10_YEARS": return "10年";
            case "5_YEARS": return "5年";
            case "3_YEARS": return "3年";
            case "1_YEAR": return "1年";
            default: return retentionPeriod;
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    /**
     * 获取案由名称
     */
    private String getCauseOfActionName(Matter matter) {
        if (matter.getCauseOfAction() == null || matter.getCauseOfAction().isEmpty()) {
            return "";
        }
        
        String causeType = switch (matter.getCaseType() != null ? matter.getCaseType() : "") {
            case "CRIMINAL" -> CauseOfActionService.TYPE_CRIMINAL;
            case "ADMINISTRATIVE" -> CauseOfActionService.TYPE_ADMIN;
            default -> CauseOfActionService.TYPE_CIVIL;
        };
        
        return causeOfActionService.getCauseName(matter.getCauseOfAction(), causeType);
    }
}
