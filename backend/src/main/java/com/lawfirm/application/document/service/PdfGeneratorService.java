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
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.workbench.entity.Approval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

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
     * 生成收案审批表PDF
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
     * 生成授权委托书PDF
     */
    public byte[] generatePowerOfAttorneyPdf(Matter matter, Client client) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(60, 60, 60, 60);

            PdfFont font = createChineseFont();

            // 标题
            document.add(new Paragraph("授 权 委 托 书")
                .setFont(font)
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

            // 委托人信息
            String clientName = client != null ? client.getName() : "_______________";
            String clientType = client != null && "ENTERPRISE".equals(client.getClientType()) ? "法定代表人" : "身份证号";
            String clientContact = client != null && client.getContactPhone() != null ? client.getContactPhone() : "_______________";

            document.add(new Paragraph("委托人：" + clientName)
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(10));
            
            document.add(new Paragraph(clientType + "：_______________")
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(10));
            
            document.add(new Paragraph("联系电话：" + clientContact)
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(20));

            // 受托人信息
            document.add(new Paragraph("受托人：_______________律师事务所 _______________ 律师")
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(10));
            
            document.add(new Paragraph("执业证号：_______________")
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(20));

            // 委托事项
            String matterName = matter != null && matter.getName() != null ? matter.getName() : "_______________";
            String caseType = matter != null && matter.getCaseType() != null ? getCaseTypeText(matter.getCaseType()) : "_______________";

            document.add(new Paragraph("委托事项：")
                .setFont(font)
                .setFontSize(12)
                .setBold()
                .setMarginBottom(10));
            
            document.add(new Paragraph("    本人因" + matterName + "（" + caseType + "）一案，特委托上述受托人作为本人的诉讼代理人。")
                .setFont(font)
                .setFontSize(12)
                .setMarginBottom(20));

            // 代理权限
            document.add(new Paragraph("代理权限：")
                .setFont(font)
                .setFontSize(12)
                .setBold()
                .setMarginBottom(10));

            String[] permissions = {
                "1. 一般代理（代为起诉、应诉、变更诉讼请求、提供证据、进行辩论、申请调解）",
                "2. 特别代理（□ 代为承认、放弃、变更诉讼请求 □ 代为和解 □ 代为提起上诉 □ 代收法律文书）"
            };
            for (String perm : permissions) {
                document.add(new Paragraph("    " + perm)
                    .setFont(font)
                    .setFontSize(11)
                    .setMarginBottom(5));
            }

            // 委托期限
            document.add(new Paragraph("\n委托期限：自签署之日起至本案结案止。")
                .setFont(font)
                .setFontSize(12)
                .setMarginTop(20)
                .setMarginBottom(30));

            // 签章区域
            document.add(new Paragraph("\n\n委托人（签章）：")
                .setFont(font)
                .setFontSize(12)
                .setMarginTop(30));
            
            document.add(new Paragraph("\n日期：     年   月   日")
                .setFont(font)
                .setFontSize(12)
                .setMarginTop(20));

            // 页脚
            document.add(new Paragraph("本授权委托书由系统自动生成，仅供参考，请以签字盖章版本为准。")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(50));

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
     * 支持预格式化的纯文本模板（已替换变量）
     * 
     * @param title 文档标题
     * @param content 已替换变量的模板内容
     * @return PDF 字节数组
     */
    public byte[] generatePdfFromTemplateContent(String title, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont font = createChineseFont();

            // 标题
            if (title != null && !title.isEmpty()) {
                document.add(new Paragraph(title)
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
            }

            // 内容 - 按行处理，保留格式
            if (content != null && !content.isEmpty()) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    // 检测是否是分隔线
                    if (line.trim().matches("^[━─═]+$")) {
                        document.add(new Paragraph(line)
                            .setFont(font)
                            .setFontSize(10)
                            .setFontColor(ColorConstants.GRAY)
                            .setMarginTop(5)
                            .setMarginBottom(5));
                    }
                    // 检测是否是小标题（以【】包围）
                    else if (line.trim().startsWith("【") && line.trim().endsWith("】")) {
                        document.add(new Paragraph(line)
                            .setFont(font)
                            .setFontSize(12)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(15)
                            .setMarginBottom(10));
                    }
                    // 检测是否是空行
                    else if (line.trim().isEmpty()) {
                        document.add(new Paragraph(" ")
                            .setFont(font)
                            .setFontSize(8)
                            .setMarginTop(2)
                            .setMarginBottom(2));
                    }
                    // 普通内容行
                    else {
                        document.add(new Paragraph(line)
                            .setFont(font)
                            .setFontSize(11)
                            .setMarginBottom(3));
                    }
                }
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

