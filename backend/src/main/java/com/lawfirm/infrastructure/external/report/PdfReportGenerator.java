package com.lawfirm.infrastructure.external.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * PDF报表生成器（使用iText）
 */
@Slf4j
@Component
public class PdfReportGenerator {

    /**
     * 生成收入报表
     */
    public ByteArrayInputStream generateRevenueReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 标题
        Paragraph title = new Paragraph("收入报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // 创建表格
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 3, 2, 2}))
                .useAllAvailableWidth();

        // 表头
        table.addHeaderCell(createHeaderCell("日期"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("案件名称"));
        table.addHeaderCell(createHeaderCell("收费金额"));
        table.addHeaderCell(createHeaderCell("收款状态"));

        // 数据行
        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(row.get("date") != null ? row.get("date").toString() : ""));
            table.addCell(createDataCell(row.get("clientName") != null ? row.get("clientName").toString() : ""));
            table.addCell(createDataCell(row.get("matterName") != null ? row.get("matterName").toString() : ""));
            table.addCell(createDataCell(row.get("amount") != null ? row.get("amount").toString() : ""));
            table.addCell(createDataCell(row.get("status") != null ? row.get("status").toString() : ""));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成案件报表
     */
    public ByteArrayInputStream generateMatterReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("案件报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 2, 1.5f, 2}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("案件编号"));
        table.addHeaderCell(createHeaderCell("案件名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("案件类型"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("创建日期"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(row.get("matterNo") != null ? row.get("matterNo").toString() : ""));
            table.addCell(createDataCell(row.get("matterName") != null ? row.get("matterName").toString() : ""));
            table.addCell(createDataCell(row.get("clientName") != null ? row.get("clientName").toString() : ""));
            table.addCell(createDataCell(row.get("matterType") != null ? row.get("matterType").toString() : ""));
            table.addCell(createDataCell(row.get("status") != null ? row.get("status").toString() : ""));
            table.addCell(createDataCell(row.get("createdAt") != null ? row.get("createdAt").toString() : ""));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成客户报表
     */
    public ByteArrayInputStream generateClientReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("客户报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1.5f, 2, 2, 1.5f}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("客户编号"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("客户类型"));
        table.addHeaderCell(createHeaderCell("联系人"));
        table.addHeaderCell(createHeaderCell("联系电话"));
        table.addHeaderCell(createHeaderCell("状态"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(row.get("clientNo") != null ? row.get("clientNo").toString() : ""));
            table.addCell(createDataCell(row.get("clientName") != null ? row.get("clientName").toString() : ""));
            table.addCell(createDataCell(row.get("clientType") != null ? row.get("clientType").toString() : ""));
            table.addCell(createDataCell(row.get("contactPerson") != null ? row.get("contactPerson").toString() : ""));
            table.addCell(createDataCell(row.get("contactPhone") != null ? row.get("contactPhone").toString() : ""));
            table.addCell(createDataCell(row.get("status") != null ? row.get("status").toString() : ""));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成律师业绩报表
     */
    public ByteArrayInputStream generateLawyerPerformanceReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("律师业绩报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1.5f, 2, 1.5f, 2}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("律师姓名"));
        table.addHeaderCell(createHeaderCell("案件数量"));
        table.addHeaderCell(createHeaderCell("总收入"));
        table.addHeaderCell(createHeaderCell("总工时"));
        table.addHeaderCell(createHeaderCell("平均案件收入"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(row.get("lawyer_name") != null ? row.get("lawyer_name").toString() : ""));
            table.addCell(createDataCell(row.get("matter_count") != null ? row.get("matter_count").toString() : ""));
            Object revenue = row.get("total_revenue");
            if (revenue == null) {
                revenue = row.get("revenue");
            }
            table.addCell(createDataCell(revenue != null ? revenue.toString() : ""));
            table.addCell(createDataCell(row.get("total_hours") != null ? row.get("total_hours").toString() : ""));
            table.addCell(createDataCell(row.get("avg_revenue") != null ? row.get("avg_revenue").toString() : ""));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 创建表头单元格
     */
    private com.itextpdf.layout.element.Cell createHeaderCell(String text) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
    }

    /**
     * 生成应收报表
     */
    public ByteArrayInputStream generateReceivableReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("应收报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1, 1.5f}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("案件名称"));
        table.addHeaderCell(createHeaderCell("案件编号"));
        table.addHeaderCell(createHeaderCell("合同编号"));
        table.addHeaderCell(createHeaderCell("合同金额"));
        table.addHeaderCell(createHeaderCell("已收金额"));
        table.addHeaderCell(createHeaderCell("应收金额"));
        table.addHeaderCell(createHeaderCell("账龄(天)"));
        table.addHeaderCell(createHeaderCell("收款状态"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(row.get("client_name") != null ? row.get("client_name").toString() : ""));
            table.addCell(createDataCell(row.get("matter_name") != null ? row.get("matter_name").toString() : ""));
            table.addCell(createDataCell(row.get("matter_no") != null ? row.get("matter_no").toString() : ""));
            table.addCell(createDataCell(row.get("contract_no") != null ? row.get("contract_no").toString() : ""));
            table.addCell(createDataCell(row.get("contract_amount") != null ? row.get("contract_amount").toString() : ""));
            table.addCell(createDataCell(row.get("received_amount") != null ? row.get("received_amount").toString() : ""));
            table.addCell(createDataCell(row.get("receivable_amount") != null ? row.get("receivable_amount").toString() : ""));
            table.addCell(createDataCell(row.get("aging_days") != null ? row.get("aging_days").toString() : ""));
            table.addCell(createDataCell(row.get("receivable_status") != null ? row.get("receivable_status").toString() : ""));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目进度报表（M3-025）
     */
    public ByteArrayInputStream generateMatterProgressReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 标题
        Paragraph title = new Paragraph("项目进度报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // 创建表格（16列）
        Table table = new Table(UnitValue.createPercentArray(new float[]{
            1.5f, 2f, 1.5f, 1f, 1f, 1.5f, 0.8f, 0.8f, 0.8f, 1f, 1f, 1f, 1f, 1f, 1f, 1.2f
        }))
                .useAllAvailableWidth();

        // 表头
        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("业务类型"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("主办律师"));
        table.addHeaderCell(createHeaderCell("总任务"));
        table.addHeaderCell(createHeaderCell("已完成"));
        table.addHeaderCell(createHeaderCell("进行中"));
        table.addHeaderCell(createHeaderCell("完成率"));
        table.addHeaderCell(createHeaderCell("总工时"));
        table.addHeaderCell(createHeaderCell("已审批"));
        table.addHeaderCell(createHeaderCell("创建日期"));
        table.addHeaderCell(createHeaderCell("预计完成"));
        table.addHeaderCell(createHeaderCell("实际完成"));
        table.addHeaderCell(createHeaderCell("预估收费"));

        // 数据行
        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("business_type"))));
            table.addCell(createDataCell(getStringValue(row.get("status"))));
            table.addCell(createDataCell(getStringValue(row.get("lead_lawyer_name"))));
            table.addCell(createDataCell(getStringValue(row.get("total_tasks"))));
            table.addCell(createDataCell(getStringValue(row.get("completed_tasks"))));
            table.addCell(createDataCell(getStringValue(row.get("in_progress_tasks"))));
            
            // 完成率（百分比）
            String completionRate = "";
            if (row.get("task_completion_rate") != null) {
                if (row.get("task_completion_rate") instanceof Number) {
                    double rate = ((Number) row.get("task_completion_rate")).doubleValue();
                    completionRate = String.format("%.2f%%", rate);
                } else {
                    completionRate = row.get("task_completion_rate").toString();
                }
            }
            table.addCell(createDataCell(completionRate));
            
            table.addCell(createDataCell(getStringValue(row.get("total_hours"))));
            table.addCell(createDataCell(getStringValue(row.get("approved_hours"))));
            table.addCell(createDataCell(getStringValue(row.get("created_date"))));
            table.addCell(createDataCell(getStringValue(row.get("expected_end_date"))));
            table.addCell(createDataCell(getStringValue(row.get("actual_end_date"))));
            table.addCell(createDataCell(getStringValue(row.get("estimated_fee"))));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目工时报表（M3-026）
     */
    public ByteArrayInputStream generateMatterTimesheetReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目工时报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2f, 1.5f, 1f, 1f, 2f, 0.8f, 0.8f, 0.8f, 1.2f}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("工作日期"));
        table.addHeaderCell(createHeaderCell("工作类型"));
        table.addHeaderCell(createHeaderCell("工作内容"));
        table.addHeaderCell(createHeaderCell("工时"));
        table.addHeaderCell(createHeaderCell("计费"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("工作人员"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("work_date"))));
            table.addCell(createDataCell(getStringValue(row.get("work_type"))));
            table.addCell(createDataCell(getStringValue(row.get("work_content"))));
            table.addCell(createDataCell(getStringValue(row.get("hours"))));
            table.addCell(createDataCell(getStringValue(row.get("billable"))));
            table.addCell(createDataCell(getStringValue(row.get("timesheet_status"))));
            table.addCell(createDataCell(getStringValue(row.get("worker_name"))));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目任务报表（M3-027）
     */
    public ByteArrayInputStream generateMatterTaskReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目任务报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.2f, 1.8f, 1.2f, 1f, 2f, 0.8f, 0.8f, 0.8f, 1f, 1f, 1f}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("任务编号"));
        table.addHeaderCell(createHeaderCell("任务标题"));
        table.addHeaderCell(createHeaderCell("优先级"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("进度"));
        table.addHeaderCell(createHeaderCell("截止日期"));
        table.addHeaderCell(createHeaderCell("完成时间"));
        table.addHeaderCell(createHeaderCell("执行人"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("task_no"))));
            table.addCell(createDataCell(getStringValue(row.get("task_title"))));
            table.addCell(createDataCell(getStringValue(row.get("priority"))));
            table.addCell(createDataCell(getStringValue(row.get("task_status_name"))));
            table.addCell(createDataCell(getStringValue(row.get("progress"))));
            table.addCell(createDataCell(getStringValue(row.get("due_date"))));
            table.addCell(createDataCell(getStringValue(row.get("completed_at"))));
            table.addCell(createDataCell(getStringValue(row.get("assignee_name"))));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目阶段进度报表（M3-028）
     */
    public ByteArrayInputStream generateMatterStageReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目阶段进度报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{
            1.2f, 1.8f, 1.2f, 1f, 1f, 1.2f, 1f, 1f, 1f, 1f, 0.8f, 0.8f, 1f
        }))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("业务类型"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("主办律师"));
        table.addHeaderCell(createHeaderCell("部门"));
        table.addHeaderCell(createHeaderCell("创建日期"));
        table.addHeaderCell(createHeaderCell("预计完成"));
        table.addHeaderCell(createHeaderCell("实际完成"));
        table.addHeaderCell(createHeaderCell("总任务"));
        table.addHeaderCell(createHeaderCell("已完成"));
        table.addHeaderCell(createHeaderCell("完成率"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("business_type"))));
            table.addCell(createDataCell(getStringValue(row.get("progress_status"))));
            table.addCell(createDataCell(getStringValue(row.get("lead_lawyer_name"))));
            table.addCell(createDataCell(getStringValue(row.get("department_name"))));
            table.addCell(createDataCell(getStringValue(row.get("created_date"))));
            table.addCell(createDataCell(getStringValue(row.get("expected_end_date"))));
            table.addCell(createDataCell(getStringValue(row.get("actual_end_date"))));
            table.addCell(createDataCell(getStringValue(row.get("total_tasks"))));
            table.addCell(createDataCell(getStringValue(row.get("completed_tasks"))));
            
            String completionRate = "";
            if (row.get("completion_rate") != null) {
                if (row.get("completion_rate") instanceof Number) {
                    double rate = ((Number) row.get("completion_rate")).doubleValue();
                    completionRate = String.format("%.2f%%", rate);
                } else {
                    completionRate = row.get("completion_rate").toString();
                }
            }
            table.addCell(createDataCell(completionRate));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目趋势分析报表（M3-029）
     */
    public ByteArrayInputStream generateMatterTrendReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目趋势分析报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1.5f, 1.5f, 1.5f, 2f}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("月份"));
        table.addHeaderCell(createHeaderCell("新增项目数"));
        table.addHeaderCell(createHeaderCell("完成项目数"));
        table.addHeaderCell(createHeaderCell("进行中项目数"));
        table.addHeaderCell(createHeaderCell("平均周期(天)"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("period"))));
            table.addCell(createDataCell(getStringValue(row.get("new_matters_count"))));
            table.addCell(createDataCell(getStringValue(row.get("closed_matters_count"))));
            table.addCell(createDataCell(getStringValue(row.get("active_matters_count"))));
            table.addCell(createDataCell(getStringValue(row.get("avg_duration_days"))));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 获取字符串值
     */
    private String getStringValue(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    /**
     * 生成项目成本分析报表（M4-044）
     */
    public ByteArrayInputStream generateCostAnalysisReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目成本分析报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{
            1.2f, 1.8f, 1.2f, 1.2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f
        }))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("主办律师"));
        table.addHeaderCell(createHeaderCell("归集成本"));
        table.addHeaderCell(createHeaderCell("分摊成本"));
        table.addHeaderCell(createHeaderCell("总成本"));
        table.addHeaderCell(createHeaderCell("合同金额"));
        table.addHeaderCell(createHeaderCell("已收款"));
        table.addHeaderCell(createHeaderCell("利润"));
        table.addHeaderCell(createHeaderCell("利润率"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("lead_lawyer_name"))));
            table.addCell(createDataCell(getStringValue(row.get("allocated_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("split_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("total_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("contract_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("received_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("profit"))));
            
            String profitRate = "";
            if (row.get("profit_rate") != null) {
                if (row.get("profit_rate") instanceof Number) {
                    double rate = ((Number) row.get("profit_rate")).doubleValue();
                    profitRate = String.format("%.2f%%", rate);
                } else {
                    profitRate = row.get("profit_rate").toString();
                }
            }
            table.addCell(createDataCell(profitRate));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成应收账款账龄分析报表（M4-053）
     */
    public ByteArrayInputStream generateAgingAnalysisReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("应收账款账龄分析报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{
            1.5f, 1.8f, 1.2f, 1.2f, 1f, 1f, 1f, 0.8f, 1f, 1f
        }))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("合同编号"));
        table.addHeaderCell(createHeaderCell("合同金额"));
        table.addHeaderCell(createHeaderCell("已收款"));
        table.addHeaderCell(createHeaderCell("应收金额"));
        table.addHeaderCell(createHeaderCell("账龄天数"));
        table.addHeaderCell(createHeaderCell("账龄区间"));
        table.addHeaderCell(createHeaderCell("签署日期"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("contract_no"))));
            table.addCell(createDataCell(getStringValue(row.get("contract_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("received_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("receivable_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("aging_days"))));
            table.addCell(createDataCell(getStringValue(row.get("aging_range"))));
            table.addCell(createDataCell(getStringValue(row.get("sign_date"))));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 生成项目利润分析报表（M4-054）
     */
    public ByteArrayInputStream generateProfitAnalysisReport(List<Map<String, Object>> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Paragraph title = new Paragraph("项目利润分析报表")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{
            1f, 1.5f, 1.2f, 1.2f, 0.8f, 0.8f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f
        }))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("项目编号"));
        table.addHeaderCell(createHeaderCell("项目名称"));
        table.addHeaderCell(createHeaderCell("客户名称"));
        table.addHeaderCell(createHeaderCell("主办律师"));
        table.addHeaderCell(createHeaderCell("业务类型"));
        table.addHeaderCell(createHeaderCell("状态"));
        table.addHeaderCell(createHeaderCell("合同金额"));
        table.addHeaderCell(createHeaderCell("已收款"));
        table.addHeaderCell(createHeaderCell("应收金额"));
        table.addHeaderCell(createHeaderCell("归集成本"));
        table.addHeaderCell(createHeaderCell("分摊成本"));
        table.addHeaderCell(createHeaderCell("总成本"));
        table.addHeaderCell(createHeaderCell("利润"));
        table.addHeaderCell(createHeaderCell("利润率(已收款)"));
        table.addHeaderCell(createHeaderCell("利润率(合同)"));

        for (Map<String, Object> row : data) {
            table.addCell(createDataCell(getStringValue(row.get("matter_no"))));
            table.addCell(createDataCell(getStringValue(row.get("matter_name"))));
            table.addCell(createDataCell(getStringValue(row.get("client_name"))));
            table.addCell(createDataCell(getStringValue(row.get("lead_lawyer_name"))));
            table.addCell(createDataCell(getStringValue(row.get("business_type"))));
            table.addCell(createDataCell(getStringValue(row.get("status"))));
            table.addCell(createDataCell(getStringValue(row.get("contract_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("received_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("receivable_amount"))));
            table.addCell(createDataCell(getStringValue(row.get("allocated_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("split_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("total_cost"))));
            table.addCell(createDataCell(getStringValue(row.get("profit"))));
            
            String profitRateReceived = "";
            if (row.get("profit_rate_on_received") != null) {
                if (row.get("profit_rate_on_received") instanceof Number) {
                    double rate = ((Number) row.get("profit_rate_on_received")).doubleValue();
                    profitRateReceived = String.format("%.2f%%", rate);
                } else {
                    profitRateReceived = row.get("profit_rate_on_received").toString();
                }
            }
            table.addCell(createDataCell(profitRateReceived));
            
            String profitRateContract = "";
            if (row.get("profit_rate_on_contract") != null) {
                if (row.get("profit_rate_on_contract") instanceof Number) {
                    double rate = ((Number) row.get("profit_rate_on_contract")).doubleValue();
                    profitRateContract = String.format("%.2f%%", rate);
                } else {
                    profitRateContract = row.get("profit_rate_on_contract").toString();
                }
            }
            table.addCell(createDataCell(profitRateContract));
        }

        document.add(table);
        document.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 创建数据单元格
     */
    private com.itextpdf.layout.element.Cell createDataCell(String text) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text != null ? text : ""))
                .setTextAlignment(TextAlignment.LEFT);
    }
}

