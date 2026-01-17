package com.lawfirm.application.document.service;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.finance.dto.ContractPrintDTO;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.workbench.entity.Approval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * PDF 生成服务
 * 
 * 用于生成卷宗材料的PDF文档：
 * - 收案审批表
 * - 委托合同
 * - 授权委托书
 */
@Slf4j
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DeviceRgb HEADER_BG_COLOR = new DeviceRgb(240, 240, 240);

    /**
     * 生成收案审批表PDF（完整版，与合同管理模块预览一致）
     * 使用 ContractPrintDTO 生成包含所有字段的完整审批表
     */
    public byte[] generateApprovalFormPdfFromPrintDTO(ContractPrintDTO printDTO) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            PdfFont font = createChineseFont();

            // 律所名称标题
            if (printDTO.getFirmName() != null && !printDTO.getFirmName().isEmpty()) {
                document.add(new Paragraph(printDTO.getFirmName())
                    .setFont(font)
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            }

            // 主标题：收案审批表
            Paragraph title = new Paragraph("收案审批表")
                .setFont(font)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(title);

            // 合同编号（右对齐）
            if (printDTO.getContractNo() != null) {
                document.add(new Paragraph("合同编号：" + printDTO.getContractNo())
                    .setFont(font)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(10));
            }

            // 创建主表格（包含所有字段）
            // 调整列宽比例：标签列15%，三个值列各28.33%，确保表格宽度100%且所有列都能显示
            Table mainTable = new Table(UnitValue.createPercentArray(new float[]{15, 28.33f, 28.33f, 28.34f}))
                .setWidth(UnitValue.createPercentValue(100));

            // 委托人
            addFullWidthRow(mainTable, font, "委托人", 
                printDTO.getClientName() != null ? printDTO.getClientName() : "-");

            // 案由
            String causeOfActionDisplay = printDTO.getCauseOfActionName() != null && !printDTO.getCauseOfActionName().isEmpty()
                ? printDTO.getCauseOfActionName()
                : (printDTO.getCaseTypeName() != null ? printDTO.getCaseTypeName() : "-");
            addFullWidthRow(mainTable, font, "案由", causeOfActionDisplay);

            // 关联当事人
            addFullWidthRow(mainTable, font, "关联当事人", 
                printDTO.getOpposingParty() != null ? printDTO.getOpposingParty() : "-");

            // 委托程序
            addFullWidthRow(mainTable, font, "委托程序", 
                printDTO.getTrialStageName() != null ? printDTO.getTrialStageName() : "-");

            // 有无利益冲突
            String conflictResult = printDTO.getConflictCheckResult() != null 
                ? printDTO.getConflictCheckResult() : "待审查";
            addFullWidthRow(mainTable, font, "有无利益冲突", conflictResult);

            // 代理/辩护费 + 委托时间
            String feeDisplay = printDTO.getTotalAmount() != null 
                ? "¥" + String.format("%.0f", printDTO.getTotalAmount()) : "-";
            String signDateStr = formatDateChinese(printDTO.getSignDate());
            addTwoColumnRow(mainTable, font, "代理/辩护费", feeDisplay, "委托时间", signDateStr);

            // 接待人 + 办案单位
            String originatorName = printDTO.getOriginatorName() != null 
                ? printDTO.getOriginatorName() 
                : (printDTO.getSignerName() != null ? printDTO.getSignerName() : "-");
            String jurisdictionCourt = printDTO.getJurisdictionCourt() != null 
                ? printDTO.getJurisdictionCourt() : "-";
            addTwoColumnRow(mainTable, font, "接待人", originatorName, "办案单位", jurisdictionCourt);

            // 案情摘要（高度更大的单元格）
            String description = printDTO.getDescription() != null && !printDTO.getDescription().isEmpty()
                ? printDTO.getDescription() : "暂无案情摘要";
            Cell summaryLabelCell = createLabelCell("案情摘要\n（附接待笔录）", font);
            summaryLabelCell.setHeight(140);
            Cell summaryValueCell = createValueCell(description, font);
            summaryValueCell.setHeight(140);
            summaryValueCell.setVerticalAlignment(VerticalAlignment.TOP);
            mainTable.addCell(summaryLabelCell);
            mainTable.addCell(new Cell(1, 3).add(summaryValueCell));

            document.add(mainTable);

            // 审查意见部分
            document.add(new Paragraph("\n审 查 意 见")
                .setFont(font)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(8));

            // 审批意见表格：标签列25%，意见列75%
            Table approvalTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .setWidth(UnitValue.createPercentValue(100));

            // 接待律师意见
            Cell receptionLabelCell = createLabelCell("接待律师意见", font);
            approvalTable.addCell(new Cell(3, 1).add(receptionLabelCell));
            approvalTable.addCell(new Cell().add(new Paragraph("拟接受委托").setFont(font).setFontSize(12)).setHeight(50));
            
            // 接待律师签名
            approvalTable.addCell(new Cell().add(new Paragraph("签名：" + originatorName).setFont(font).setFontSize(12))
                .setTextAlignment(TextAlignment.RIGHT).setBorder(null));
            approvalTable.addCell(new Cell().add(new Paragraph("日期：" + signDateStr).setFont(font).setFontSize(12))
                .setBorder(null));

            // 律所领导意见
            if (printDTO.getApprovals() != null && !printDTO.getApprovals().isEmpty()) {
                ContractPrintDTO.ApprovalInfo leaderApproval = printDTO.getApprovals().get(0);
                String leaderComment = leaderApproval.getComment() != null ? leaderApproval.getComment() : "";
                String leaderName = leaderApproval.getApproverName() != null ? leaderApproval.getApproverName() : "";
                String leaderDate = formatDateTimeChinese(leaderApproval.getApprovedAt());

                Cell leaderLabelCell = createLabelCell("律所领导意见", font);
                approvalTable.addCell(new Cell(3, 1).add(leaderLabelCell));
                approvalTable.addCell(new Cell().add(new Paragraph(leaderComment).setFont(font).setFontSize(12)).setHeight(50));
                
                approvalTable.addCell(new Cell().add(new Paragraph("签名：" + leaderName).setFont(font).setFontSize(12))
                    .setTextAlignment(TextAlignment.RIGHT).setBorder(null));
                approvalTable.addCell(new Cell().add(new Paragraph("日期：" + leaderDate).setFont(font).setFontSize(12))
                    .setBorder(null));
            } else {
                // 预留律所领导签字区域
                Cell leaderLabelCell = createLabelCell("律所领导意见", font);
                approvalTable.addCell(new Cell(3, 1).add(leaderLabelCell));
                approvalTable.addCell(new Cell().add(new Paragraph("").setFont(font).setFontSize(12)).setHeight(50));
                
                approvalTable.addCell(new Cell().add(new Paragraph("签名：").setFont(font).setFontSize(12))
                    .setTextAlignment(TextAlignment.RIGHT).setBorder(null));
                approvalTable.addCell(new Cell().add(new Paragraph("日期：____年____月____日").setFont(font).setFontSize(12))
                    .setBorder(null));
            }

            document.add(approvalTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成收案审批表PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成收案审批表PDF（简化版，保持向后兼容）
     */
    public byte[] generateApprovalFormPdf(Approval approval, Contract contract, Matter matter, Client client) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont font = createChineseFont();

            // 标题
            Paragraph title = new Paragraph("收 案 审 批 表")
                .setFont(font)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);

            // 基本信息表格
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));

            // 合同编号
            addTableRow(infoTable, font, "合同编号", 
                contract != null ? contract.getContractNo() : "-", 
                "申请日期", 
                approval != null && approval.getCreatedAt() != null 
                    ? approval.getCreatedAt().format(DATE_FORMATTER) : "-");

            // 客户信息
            addTableRow(infoTable, font, "客户名称", 
                client != null ? client.getName() : "-", 
                "联系电话", 
                client != null && client.getContactPhone() != null ? client.getContactPhone() : "-");

            // 项目信息
            addTableRow(infoTable, font, "项目名称", 
                matter != null ? matter.getName() : "-", 
                "案件类型", 
                matter != null && matter.getCaseType() != null ? matter.getCaseType() : "-");

            // 律师费用
            addTableRow(infoTable, font, "律师费", 
                contract != null && contract.getTotalAmount() != null 
                    ? String.format("%.2f 元", contract.getTotalAmount()) : "-", 
                "收费方式", 
                contract != null && contract.getFeeType() != null ? contract.getFeeType() : "-");

            document.add(infoTable);

            // 案情简介
            document.add(new Paragraph("案情简介：")
                .setFont(font)
                .setFontSize(12)
                .setBold()
                .setMarginTop(20));
            
            document.add(new Paragraph(matter != null && matter.getDescription() != null 
                    ? matter.getDescription() : "（无）")
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(20));

            // 审批信息
            document.add(new Paragraph("审批意见：")
                .setFont(font)
                .setFontSize(12)
                .setBold()
                .setMarginTop(10));

            Table approvalTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

            addSimpleRow(approvalTable, font, "审批状态", 
                approval != null ? getApprovalStatusText(approval.getStatus()) : "-");
            addSimpleRow(approvalTable, font, "审批人", 
                approval != null && approval.getApproverName() != null 
                    ? approval.getApproverName() : "-");
            addSimpleRow(approvalTable, font, "审批时间", 
                approval != null && approval.getApprovedAt() != null 
                    ? approval.getApprovedAt().format(DATE_FORMATTER) : "-");
            addSimpleRow(approvalTable, font, "审批意见", 
                approval != null && approval.getComment() != null 
                    ? approval.getComment() : "（无）");

            document.add(approvalTable);

            // 签章区域
            document.add(new Paragraph("\n"));
            Table signTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(40);

            signTable.addCell(new Cell()
                .add(new Paragraph("申请人签字：\n\n\n日期：").setFont(font).setFontSize(11))
                .setBorder(null));
            signTable.addCell(new Cell()
                .add(new Paragraph("审批人签字：\n\n\n日期：").setFont(font).setFontSize(11))
                .setBorder(null));

            document.add(signTable);

            // 页脚
            document.add(new Paragraph("本表由系统自动生成，仅供参考，请以签字盖章版本为准。")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成收案审批表PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成委托合同PDF
     */
    public byte[] generateContractPdf(Contract contract, Matter matter, Client client) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont font = createChineseFont();

            // 标题
            String contractTitle = contract != null && contract.getName() != null 
                ? contract.getName() : "委托代理合同";
            document.add(new Paragraph(contractTitle)
                .setFont(font)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            // 合同编号
            document.add(new Paragraph("合同编号：" + 
                (contract != null ? contract.getContractNo() : "-"))
                .setFont(font)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20));

            // 甲乙双方信息
            document.add(new Paragraph("甲方（委托人）：" + 
                (client != null ? client.getName() : "_______________"))
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(5));
            
            document.add(new Paragraph("联系电话：" + 
                (client != null && client.getContactPhone() != null ? client.getContactPhone() : "_______________"))
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(5));

            document.add(new Paragraph("乙方（受托人）：_____________________律师事务所")
                .setFont(font)
                .setFontSize(11)
                .setMarginTop(15)
                .setMarginBottom(20));

            // 合同内容
            if (contract != null && contract.getContent() != null && !contract.getContent().isEmpty()) {
                // 处理HTML或富文本内容，简单提取纯文本
                String content = stripHtml(contract.getContent());
                document.add(new Paragraph(content)
                    .setFont(font)
                    .setFontSize(11)
                    .setMarginBottom(20));
            } else {
                // 默认合同条款
                addDefaultContractTerms(document, font, contract, matter);
            }

            // 签章区域
            Table signTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(40);

            signTable.addCell(new Cell()
                .add(new Paragraph("甲方（签章）：\n\n\n日期：     年   月   日").setFont(font).setFontSize(11))
                .setBorder(null));
            signTable.addCell(new Cell()
                .add(new Paragraph("乙方（签章）：\n\n\n日期：     年   月   日").setFont(font).setFontSize(11))
                .setBorder(null));

            document.add(signTable);

            // 页脚
            document.add(new Paragraph("本合同由系统自动生成，仅供参考，请以签字盖章版本为准。")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成委托合同PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成授权委托书PDF（简化版，向后兼容）
     */
    public byte[] generatePowerOfAttorneyPdf(Matter matter, Client client) {
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
     */
    public byte[] generatePowerOfAttorneyPdf(Matter matter, Client client, 
            String lawyerName, String lawyerLicenseNo, String firmName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            // 减小边距，增加可用空间，确保内容能在一页内显示
            document.setMargins(35, 50, 35, 50);

            PdfFont font = createChineseFont();

            // 标题
            document.add(new Paragraph("授 权 委 托 书")
                .setFont(font)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15)); // 减小标题底部间距

            // 委托人信息
            String clientName = client != null ? client.getName() : "_______________";
            String clientType = client != null && "ENTERPRISE".equals(client.getClientType()) ? "法定代表人" : "身份证号";
            String clientIdValue = client != null ? 
                ("ENTERPRISE".equals(client.getClientType()) ? 
                    (client.getLegalRepresentative() != null ? client.getLegalRepresentative() : "_______________") :
                    (client.getIdCard() != null ? client.getIdCard() : "_______________")
                ) : "_______________";
            String clientContact = client != null && client.getContactPhone() != null ? client.getContactPhone() : "_______________";

            document.add(new Paragraph("委托人：" + clientName)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(4)); // 减小间距
            
            document.add(new Paragraph(clientType + "：" + clientIdValue)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(4)); // 减小间距
            
            document.add(new Paragraph("联系电话：" + clientContact)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(8)); // 减小间距

            // 受托人信息
            String firmNameDisplay = firmName != null && !firmName.isEmpty() ? firmName : "_______________";
            String lawyerNameDisplay = lawyerName != null && !lawyerName.isEmpty() ? lawyerName : "_______________";
            String licenseNoDisplay = lawyerLicenseNo != null && !lawyerLicenseNo.isEmpty() ? lawyerLicenseNo : "_______________";
            
            document.add(new Paragraph("受托人：" + firmNameDisplay + " " + lawyerNameDisplay + " 律师")
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(4)); // 减小间距
            
            document.add(new Paragraph("执业证号：" + licenseNoDisplay)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(8)); // 减小间距

            // 委托事项
            String matterName = matter != null && matter.getName() != null ? matter.getName() : "_______________";
            String caseType = matter != null && matter.getCaseType() != null ? getCaseTypeText(matter.getCaseType()) : "_______________";

            document.add(new Paragraph("委托事项：")
                .setFont(font)
                .setFontSize(11)
                .setBold()
                .setMarginBottom(4)); // 减小间距
            
            document.add(new Paragraph("    本人因" + matterName + "（" + caseType + "）一案，特委托上述受托人作为本人的诉讼代理人。")
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(8)); // 减小间距

            // 代理权限
            document.add(new Paragraph("代理权限：")
                .setFont(font)
                .setFontSize(11)
                .setBold()
                .setMarginBottom(4)); // 减小间距

            String[] permissions = {
                "1. 一般代理（代为起诉、应诉、变更诉讼请求、提供证据、进行辩论、申请调解）",
                "2. 特别代理（□ 代为承认、放弃、变更诉讼请求 □ 代为和解 □ 代为提起上诉 □ 代收法律文书）"
            };
            for (String perm : permissions) {
                document.add(new Paragraph("    " + perm)
                    .setFont(font)
                    .setFontSize(10)
                    .setMarginBottom(3)); // 减小间距
            }

            // 委托期限
            document.add(new Paragraph("委托期限：自签署之日起至本案结案止。")
                .setFont(font)
                .setFontSize(11)
                .setMarginTop(8)
                .setMarginBottom(12)); // 减小间距

            // 签章区域（使用表格并排显示）
            Table signTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(15);

            signTable.addCell(new Cell()
                .add(new Paragraph("委托人（签章）：\n\n\n日期：     年   月   日").setFont(font).setFontSize(11))
                .setBorder(null));
            signTable.addCell(new Cell()
                .add(new Paragraph("受托人（确认）：\n\n\n日期：     年   月   日").setFont(font).setFontSize(11))
                .setBorder(null));

            document.add(signTable);

            // 页脚
            document.add(new Paragraph("本授权委托书由系统自动生成，仅供参考，请以签字盖章版本为准。")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成授权委托书PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建中文字体
     */
    private PdfFont createChineseFont() {
        try {
            // 尝试加载内置字体
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            log.warn("无法加载STSong字体，尝试使用系统字体", e);
            try {
                // 尝试加载系统字体
                return PdfFontFactory.createFont("/System/Library/Fonts/STHeiti Light.ttc,0", PdfEncodings.IDENTITY_H);
            } catch (Exception ex) {
                log.error("无法加载中文字体", ex);
                throw new RuntimeException("无法加载中文字体", ex);
            }
        }
    }

    /**
     * 添加表格行（4列）
     */
    private void addTableRow(Table table, PdfFont font, String label1, String value1, String label2, String value2) {
        table.addCell(createLabelCell(label1, font));
        table.addCell(createValueCell(value1, font));
        table.addCell(createLabelCell(label2, font));
        table.addCell(createValueCell(value2, font));
    }

    /**
     * 添加简单行（2列）
     */
    private void addSimpleRow(Table table, PdfFont font, String label, String value) {
        table.addCell(createLabelCell(label, font));
        table.addCell(createValueCell(value, font));
    }

    private Cell createLabelCell(String text, PdfFont font) {
        return new Cell()
            .add(new Paragraph(text).setFont(font).setFontSize(11))
            .setBackgroundColor(HEADER_BG_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(5);
    }

    private Cell createValueCell(String text, PdfFont font) {
        return new Cell()
            .add(new Paragraph(text != null ? text : "-").setFont(font).setFontSize(11))
            .setTextAlignment(TextAlignment.LEFT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(5);
    }

    /**
     * 获取审批状态文本
     */
    private String getApprovalStatusText(String status) {
        if (status == null) return "未知";
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
     */
    private String getCaseTypeText(String caseType) {
        if (caseType == null) return "其他";
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
     */
    private void addFullWidthRow(Table table, PdfFont font, String label, String value) {
        table.addCell(createLabelCell(label, font));
        table.addCell(new Cell(1, 3).add(createValueCell(value, font)));
    }

    /**
     * 添加两列行（标题1-内容1，标题2-内容2）
     */
    private void addTwoColumnRow(Table table, PdfFont font, String label1, String value1, String label2, String value2) {
        table.addCell(createLabelCell(label1, font));
        table.addCell(createValueCell(value1, font));
        table.addCell(createLabelCell(label2, font));
        table.addCell(createValueCell(value2, font));
    }

    /**
     * 格式化日期为中文格式（yyyy年MM月dd日）
     */
    private String formatDateChinese(java.time.LocalDate date) {
        if (date == null) return "____年____月____日";
        return date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA));
    }

    /**
     * 格式化日期时间为中文格式（yyyy年MM月dd日）
     */
    private String formatDateTimeChinese(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "____年____月____日";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA));
    }

    /**
     * 去除HTML标签
     */
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&amp;", "&")
                   .trim();
    }

    /**
     * 从模板内容生成 PDF
     * 支持 HTML 富文本模板（已替换变量）
     * 
     * 支持的HTML格式：
     * - &lt;p&gt; 段落
     * - &lt;strong&gt;/&lt;b&gt; 加粗
     * - &lt;em&gt;/&lt;i&gt; 斜体
     * - &lt;u&gt; 下划线
     * - &lt;center&gt; 居中
     * - &lt;h1&gt;-&lt;h4&gt; 标题
     * - &lt;br&gt; 换行
     * 
     * @param title 文档标题
     * @param content 已替换变量的模板内容（支持HTML格式）
     * @return PDF 字节数组
     */
    public byte[] generatePdfFromTemplateContent(String title, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont font = createChineseFont();

            // 标题（如果提供）
            if (title != null && !title.isEmpty()) {
                document.add(new Paragraph(title)
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
            }

            // 内容 - 解析HTML格式
            if (content != null && !content.isEmpty()) {
                parseHtmlContent(document, content, font);
            }

            // 页脚
            document.add(new Paragraph("本文档由系统自动生成，仅供参考，请以签字盖章版本为准。")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("从模板内容生成PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析HTML内容并添加到PDF文档
     */
    private void parseHtmlContent(Document document, String htmlContent, PdfFont font) {
        // 预处理：规范化HTML
        String content = htmlContent
            .replaceAll("<br\\s*/?>", "\n")           // <br> 转换为换行
            .replaceAll("</p>", "</p>\n")             // 段落后加换行
            .replaceAll("</div>", "</div>\n")         // div后加换行
            .replaceAll("</h[1-6]>", "</h>\n")        // 标题后加换行
            .replaceAll("&nbsp;", " ")                // 空格
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&")
            .replaceAll("&quot;", "\"");

        // 按块解析（段落、标题等）
        String[] blocks = content.split("(?=<p[^>]*>)|(?=<h[1-6][^>]*>)|(?=<div[^>]*>)|(?<=</p>)|(?<=</h>)|(?<=</div>)|\n");
        
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            
            Paragraph para = new Paragraph();
            para.setFont(font);
            para.setMarginBottom(6);
            
            // 检测块级格式
            boolean isCentered = block.contains("text-align: center") || 
                                block.contains("text-align:center") ||
                                block.matches("(?s).*<center>.*</center>.*");
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
            float fontSize = 11;
            if (block.matches("(?s).*<h1[^>]*>.*")) fontSize = 18;
            else if (block.matches("(?s).*<h2[^>]*>.*")) fontSize = 16;
            else if (block.matches("(?s).*<h3[^>]*>.*")) fontSize = 14;
            else if (block.matches("(?s).*<h4[^>]*>.*")) fontSize = 12;
            para.setFontSize(fontSize);
            
            // 标题加粗
            if (isHeading) {
                para.setBold();
                para.setMarginTop(10);
                para.setMarginBottom(8);
            }
            
            // 解析内联格式
            String text = stripHtmlTags(block);
            if (text.trim().isEmpty()) continue;
            
            // 检测内联格式并应用
            boolean isBold = block.contains("<strong>") || block.contains("<b>") || 
                           block.contains("font-weight: bold") || block.contains("font-weight:bold");
            boolean isItalic = block.contains("<em>") || block.contains("<i>");
            boolean isUnderline = block.contains("<u>") || 
                                block.contains("text-decoration: underline") ||
                                block.contains("text-decoration:underline");
            
            if (isBold) para.setBold();
            if (isItalic) para.setItalic();
            if (isUnderline) para.setUnderline();
            
            // 检测首行缩进
            if (block.contains("text-indent") || text.startsWith("    ") || text.startsWith("\t")) {
                para.setFirstLineIndent(24);
                text = text.stripLeading();
            }
            
            para.add(text);
            document.add(para);
        }
    }

    /**
     * 去除HTML标签，保留纯文本
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * 从分块模板生成授权委托书PDF
     * 
     * 固定格式：
     * - 标题：二号宋体（22pt），居中
     * - 正文：三号仿宋（16pt）
     * - 备注：五号宋体（10.5pt）
     * 
     * 支持两种格式：
     * 1. 新结构化格式：{ "_structured": true, "blocks": { "title": {...}, "client": "...", ... } }
     * 2. 旧格式：{ "title": "...", "clientInfo": "...", ... }
     * 
     * @param templateContent JSON格式的分块模板内容
     * @return PDF字节数组
     */
    public byte[] generatePowerOfAttorneyFromBlocks(String templateContent) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            // 减小边距，增加可用空间，确保内容能在一页内显示
            document.setMargins(35, 50, 35, 50);

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
            document.add(new Paragraph(title)
                .setFont(songFont)
                .setFontSize(22)  // 二号字体约22pt
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)); // 减小标题底部间距

            // 委托人信息：三号仿宋
            // 新格式：blocks.client，旧格式：blocks.clientInfo
            String clientInfo = getBlockContent(blocks, "client", "");
            if (clientInfo.isEmpty()) {
                clientInfo = getBlockContent(blocks, "clientInfo", "");
            }
            if (!clientInfo.isEmpty()) {
                addFormattedParagraphs(document, fangsongFont, 16, clientInfo);
                document.add(new Paragraph().setMarginBottom(8)); // 减小间距
            }

            // 受托人信息：三号仿宋
            // 新格式：blocks.agent，旧格式：blocks.agentInfo
            String agentInfo = getBlockContent(blocks, "agent", "");
            if (agentInfo.isEmpty()) {
                agentInfo = getBlockContent(blocks, "agentInfo", "");
            }
            if (!agentInfo.isEmpty()) {
                addFormattedParagraphs(document, fangsongFont, 16, agentInfo);
                document.add(new Paragraph().setMarginBottom(8)); // 减小间距
            }

            // 委托事项：三号仿宋
            // 新格式：blocks.matter 包含所有事项内容，旧格式：blocks.matterInfo, blocks.authorization, blocks.duration
            String matter = getBlockContent(blocks, "matter", "");
            if (!matter.isEmpty()) {
                // 新格式：委托事项、代理权限、委托期限都在 matter 中
                addFormattedParagraphs(document, fangsongFont, 16, matter);
                document.add(new Paragraph().setMarginBottom(8)); // 减小间距
            } else {
                // 旧格式：分别处理各项
                String matterInfo = getBlockContent(blocks, "matterInfo", "");
                if (!matterInfo.isEmpty()) {
                    addFormattedParagraphs(document, fangsongFont, 16, matterInfo);
                    document.add(new Paragraph().setMarginBottom(8)); // 减小间距
                }

                String authorization = getBlockContent(blocks, "authorization", "");
                if (!authorization.isEmpty()) {
                    addFormattedParagraphs(document, fangsongFont, 16, authorization);
                    document.add(new Paragraph().setMarginBottom(8)); // 减小间距
                }

                String duration = getBlockContent(blocks, "duration", "");
                if (!duration.isEmpty()) {
                    addFormattedParagraphs(document, fangsongFont, 16, duration);
                    document.add(new Paragraph().setMarginBottom(12)); // 减小间距
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
                signature = getBlockContent(blocks, "signature", "委托人（签章）：________________\n\n日    期：    年  月  日");
                addFormattedParagraphs(document, fangsongFont, 16, signature);
            }
            document.add(new Paragraph().setMarginBottom(10)); // 减小间距

            // 分隔线
            document.add(new Paragraph("━".repeat(40))
                .setFont(songFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(10)
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
            addFormattedParagraphs(document, songFont, 10.5f, remarks);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("从分块模板生成授权委托书PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取分块内容（支持字符串值）
     */
    private String getBlockContent(JsonNode blocks, String key, String defaultValue) {
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
     * 获取嵌套分块内容（支持对象嵌套）
     * 例如：blocks.signature.clientSign 或 blocks.title.documentTitle
     */
    private String getNestedBlockContent(JsonNode blocks, String parentKey, String childKey, String defaultValue) {
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
     */
    private void addFormattedParagraphs(Document document, PdfFont font, float fontSize, String content) {
        if (content == null || content.isEmpty()) return;
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            // 检测首行缩进
            boolean hasIndent = line.startsWith("    ") || line.startsWith("\t");
            String text = line.stripLeading();
            
            // 处理空变量为下划线
            text = text.replaceAll("\\$\\{[^}]+\\}", "________________");
            
            if (text.isEmpty()) {
                document.add(new Paragraph().setMarginBottom(4));
                continue;
            }
            
            Paragraph para = new Paragraph(text)
                .setFont(font)
                .setFontSize(fontSize)
                .setMarginBottom(4);
            
            if (hasIndent) {
                para.setFirstLineIndent(32); // 两个汉字宽度
            }
            
            document.add(para);
        }
    }

    /**
     * 创建仿宋字体
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
     * 检查模板内容是否为JSON分块格式
     * 支持新格式：{ "_structured": true, "blocks": {...} }
     * 支持旧格式：{ "title": "...", "clientInfo": "...", ... }
     */
    public boolean isBlockTemplateFormat(String content) {
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
                return blocks.has("title") || blocks.has("client") || blocks.has("agent") 
                    || blocks.has("clientInfo") || blocks.has("agentInfo");
            }
            
            // 检查是否是旧格式（直接是 blocks 对象）
            // 检查是否包含授权委托书的典型分块字段
            return node.has("title") || node.has("clientInfo") || node.has("agentInfo")
                || node.has("client") || node.has("agent");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加默认合同条款
     */
    private void addDefaultContractTerms(Document document, PdfFont font, Contract contract, Matter matter) {
        String[] terms = {
            "第一条 委托事项",
            "    甲方因" + (matter != null && matter.getName() != null ? matter.getName() : "_______________") 
                + "事宜，委托乙方提供法律服务。",
            "",
            "第二条 代理权限",
            "    乙方接受甲方委托，指派律师担任甲方的诉讼代理人/法律顾问，代理权限为：一般代理。",
            "",
            "第三条 律师费用",
            "    甲方应向乙方支付律师服务费人民币" 
                + (contract != null && contract.getTotalAmount() != null ? String.format("%.2f", contract.getTotalAmount()) : "_______________") 
                + "元。",
            "",
            "第四条 付款方式",
            "    " + (contract != null && contract.getPaymentTerms() != null ? contract.getPaymentTerms() : "按约定付款"),
            "",
            "第五条 双方权利义务",
            "    1. 甲方应如实向乙方陈述案情，提供相关证据材料。",
            "    2. 乙方应依法维护甲方的合法权益，保守甲方的商业秘密。",
            "",
            "第六条 合同期限",
            "    本合同自双方签字盖章之日起生效，至委托事项办结时终止。"
        };

        for (String term : terms) {
            document.add(new Paragraph(term)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(term.isEmpty() ? 10 : 3));
        }
    }
}

