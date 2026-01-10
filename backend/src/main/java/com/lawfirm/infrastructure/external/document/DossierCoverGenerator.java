package com.lawfirm.infrastructure.external.document;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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
 * 生成牛皮纸风格的卷宗封面PDF
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DossierCoverGenerator {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SysConfigAppService sysConfigAppService;

    // 牛皮纸颜色（RGB: 222, 184, 135）
    private static final DeviceRgb KRAFT_PAPER_COLOR = new DeviceRgb(222, 184, 135);
    // 深棕色文字（RGB: 101, 67, 33）
    private static final DeviceRgb DARK_BROWN = new DeviceRgb(101, 67, 33);
    // 边框颜色（RGB: 139, 90, 43）
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(139, 90, 43);

    /**
     * 生成卷宗封面PDF
     * 
     * @param matter 项目信息
     * @param archiveNo 档案编号
     * @return PDF字节数组
     */
    public byte[] generateCover(Matter matter, String archiveNo) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            
            // 设置页面大小（A4）
            Document document = new Document(pdf);
            
            // 设置背景色为牛皮纸色
            document.setBackgroundColor(KRAFT_PAPER_COLOR);
            
            // 获取中文字体
            PdfFont font = getChineseFont();
            
            // 添加外边框
            addOuterBorder(document);
            
            // 添加标题区域
            addTitleSection(document, matter, font);
            
            // 添加项目信息区域
            addMatterInfoSection(document, matter, archiveNo, font);
            
            // 添加客户和对方当事人信息
            addPartyInfoSection(document, matter, font);
            
            // 添加时间信息
            addTimeInfoSection(document, matter, font);
            
            // 添加底部信息
            addFooterSection(document, matter, font);
            
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("生成卷宗封面失败", e);
            throw new RuntimeException("生成卷宗封面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取中文字体
     */
    private PdfFont getChineseFont() {
        try {
            // 尝试使用系统中文字体
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            try {
                // 尝试使用SimSun字体
                return PdfFontFactory.createFont("SimSun", "UniGB-UCS2-H");
            } catch (Exception e2) {
                // 回退到默认字体
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
     * 添加外边框
     */
    private void addOuterBorder(Document document) {
        // 使用表格创建边框效果
        Table borderTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(BORDER_COLOR, 3))
                .setMargin(20);
        
        Cell borderCell = new Cell()
                .setHeight(750) // A4高度约842，留边距
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        
        borderTable.addCell(borderCell);
        document.add(borderTable);
    }

    /**
     * 添加标题区域
     */
    private void addTitleSection(Document document, Matter matter, PdfFont font) {
        Paragraph title = new Paragraph("业务档案卷宗")
                .setFont(font)
                .setFontSize(32)
                .setBold()
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(60)
                .setMarginBottom(20);
        document.add(title);
        
        // 案件类型标签
        String caseTypeName = MatterConstants.getCaseTypeName(matter.getCaseType());
        if (caseTypeName != null) {
            Paragraph typeLabel = new Paragraph(caseTypeName)
                    .setFont(font)
                    .setFontSize(18)
                    .setFontColor(DARK_BROWN)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(40);
            document.add(typeLabel);
        }
    }

    /**
     * 添加项目信息区域
     */
    private void addMatterInfoSection(Document document, Matter matter, String archiveNo, PdfFont font) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginLeft(60)
                .setMarginRight(60)
                .setMarginTop(30)
                .setMarginBottom(20);
        
        // 项目名称
        addInfoRow(infoTable, "项目名称", matter.getName() != null ? matter.getName() : "", font);
        
        // 项目编号
        if (matter.getMatterNo() != null) {
            addInfoRow(infoTable, "项目编号", matter.getMatterNo(), font);
        }
        
        // 档案编号
        if (archiveNo != null) {
            addInfoRow(infoTable, "档案编号", archiveNo, font);
        }
        
        // 项目大类
        String matterTypeName = MatterConstants.getMatterTypeName(matter.getMatterType());
        if (matterTypeName != null) {
            addInfoRow(infoTable, "项目大类", matterTypeName, font);
        }
        
        // 案件类型
        String caseTypeName = MatterConstants.getCaseTypeName(matter.getCaseType());
        if (caseTypeName != null) {
            addInfoRow(infoTable, "案件类型", caseTypeName, font);
        }
        
        // 案由
        if (matter.getCauseOfAction() != null) {
            addInfoRow(infoTable, "案由", matter.getCauseOfAction(), font);
        }
        
        document.add(infoTable);
    }

    /**
     * 添加客户和对方当事人信息
     */
    private void addPartyInfoSection(Document document, Matter matter, PdfFont font) {
        Table partyTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginLeft(60)
                .setMarginRight(60)
                .setMarginTop(20)
                .setMarginBottom(20);
        
        // 委托人（客户）
        if (matter.getClientId() != null) {
            String clientName = getClientName(matter.getClientId());
            addInfoRow(partyTable, "委托人", clientName, font);
        }
        
        // 对方当事人
        if (matter.getOpposingParty() != null) {
            addInfoRow(partyTable, "对方当事人", matter.getOpposingParty(), font);
        }
        
        // 主办律师
        if (matter.getLeadLawyerId() != null) {
            String lawyerName = getLawyerName(matter.getLeadLawyerId());
            addInfoRow(partyTable, "主办律师", lawyerName, font);
        }
        
        if (partyTable.getNumberOfRows() > 0) {
            document.add(partyTable);
        }
    }

    /**
     * 添加时间信息
     */
    private void addTimeInfoSection(Document document, Matter matter, PdfFont font) {
        Table timeTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginLeft(60)
                .setMarginRight(60)
                .setMarginTop(20)
                .setMarginBottom(20);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        
        // 立案日期
        if (matter.getFilingDate() != null) {
            addInfoRow(timeTable, "立案日期", matter.getFilingDate().format(formatter), font);
        }
        
        // 结案日期
        if (matter.getActualClosingDate() != null) {
            addInfoRow(timeTable, "结案日期", matter.getActualClosingDate().format(formatter), font);
        } else if (matter.getCreatedAt() != null) {
            // 如果没有结案日期，使用创建日期
            LocalDate createdDate = matter.getCreatedAt().toLocalDate();
            addInfoRow(timeTable, "创建日期", createdDate.format(formatter), font);
        }
        
        // 归档日期
        addInfoRow(timeTable, "归档日期", LocalDate.now().format(formatter), font);
        
        document.add(timeTable);
    }

    /**
     * 添加底部信息
     */
    private void addFooterSection(Document document, Matter matter, PdfFont font) {
        Paragraph footer = new Paragraph("本卷宗共    卷    页")
                .setFont(font)
                .setFontSize(14)
                .setFontColor(DARK_BROWN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(60)
                .setMarginBottom(20);
        document.add(footer);
        
        // 律所名称（从系统配置获取）
        String firmNameValue = "";
        try {
            firmNameValue = sysConfigAppService.getConfigValue("firm.name");
            if (firmNameValue == null || firmNameValue.isEmpty()) {
                firmNameValue = "";
            }
        } catch (Exception e) {
            log.warn("获取律所名称失败，使用空值", e);
        }
        
        if (!firmNameValue.isEmpty()) {
            Paragraph firmName = new Paragraph(firmNameValue)
                    .setFont(font)
                    .setFontSize(12)
                    .setFontColor(DARK_BROWN)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(firmName);
        }
    }

    /**
     * 添加信息行
     */
    private void addInfoRow(Table table, String label, String value, PdfFont font) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(14)
                        .setBold()
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(new DeviceRgb(245, 222, 179)); // 浅棕色背景
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value != null ? value : "")
                        .setFont(font)
                        .setFontSize(14)
                        .setFontColor(DARK_BROWN))
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(KRAFT_PAPER_COLOR);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * 获取律师姓名
     */
    private String getLawyerName(Long lawyerId) {
        try {
            User lawyer = userRepository.findById(lawyerId);
            if (lawyer != null) {
                return lawyer.getRealName() != null ? lawyer.getRealName() : lawyer.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取律师姓名失败: lawyerId={}", lawyerId, e);
        }
        return "未知";
    }

    /**
     * 获取客户名称
     */
    private String getClientName(Long clientId) {
        try {
            Client client = clientRepository.findById(clientId);
            if (client != null) {
                return client.getName();
            }
        } catch (Exception e) {
            log.warn("获取客户名称失败: clientId={}", clientId, e);
        }
        return "未知";
    }
}

