package com.lawfirm.application.document.dto;

import lombok.Data;
import java.util.List;

/**
 * 项目上下文信息 DTO
 * 用于 AI 文书生成时收集项目相关的所有信息
 */
@Data
public class MatterContextDTO {

    /**
     * 项目基本信息
     */
    private MatterInfo matter;

    /**
     * 客户信息列表
     */
    private List<ClientInfo> clients;

    /**
     * 项目成员信息
     */
    private List<ParticipantInfo> participants;

    /**
     * 相关文档列表
     */
    private List<DocumentInfo> documents;

    /**
     * 是否已脱敏
     */
    private boolean masked = false;

    /**
     * 项目基本信息
     */
    @Data
    public static class MatterInfo {
        private Long id;
        private String matterNo;
        private String name;
        private String matterType;
        private String caseType;
        private String status;
        private String description;
        private String court;
        private String caseNo;
        private String opposingParty;
        private String claimAmount;
        private String disputeAmount;
    }

    /**
     * 客户信息
     */
    @Data
    public static class ClientInfo {
        private Long id;
        private String name;
        private String clientType;
        private String creditCode;
        private String idCard;
        private String legalRepresentative;
        private String contactPerson;
        private String contactPhone;
        private String contactEmail;
        private String registeredAddress;
        private String role;  // 委托人/被告等
        private boolean isPrimary;
    }

    /**
     * 参与人信息
     */
    @Data
    public static class ParticipantInfo {
        private Long userId;
        private String name;
        private String role;  // 负责人/协办律师/助理等
        private String phone;
        private String email;
        private String lawyerLicenseNo;
    }

    /**
     * 文档信息
     */
    @Data
    public static class DocumentInfo {
        private Long id;
        private String title;
        private String fileName;
        private String fileType;
        private String category;
        private String description;
        private String content;  // 文档内容摘要（如果可提取）
    }

    /**
     * 转换为文本描述（用于 AI Prompt）
     */
    public String toTextDescription() {
        StringBuilder sb = new StringBuilder();
        
        // 项目信息
        if (matter != null) {
            sb.append("【项目信息】\n");
            sb.append("项目名称：").append(matter.getName()).append("\n");
            sb.append("项目编号：").append(matter.getMatterNo()).append("\n");
            if (matter.getMatterType() != null) {
                sb.append("项目类型：").append(matter.getMatterType()).append("\n");
            }
            if (matter.getCaseType() != null) {
                sb.append("案件类型：").append(matter.getCaseType()).append("\n");
            }
            if (matter.getCourt() != null) {
                sb.append("管辖法院：").append(matter.getCourt()).append("\n");
            }
            if (matter.getCaseNo() != null) {
                sb.append("案号：").append(matter.getCaseNo()).append("\n");
            }
            if (matter.getOpposingParty() != null) {
                sb.append("对方当事人：").append(matter.getOpposingParty()).append("\n");
            }
            if (matter.getClaimAmount() != null) {
                sb.append("诉讼标的：").append(matter.getClaimAmount()).append("\n");
            }
            if (matter.getDescription() != null) {
                sb.append("案情概述：").append(matter.getDescription()).append("\n");
            }
            sb.append("\n");
        }
        
        // 客户信息
        if (clients != null && !clients.isEmpty()) {
            sb.append("【当事人信息】\n");
            for (ClientInfo client : clients) {
                String roleLabel = client.isPrimary() ? "（主要委托人）" : "";
                sb.append(client.getRole() != null ? client.getRole() : "委托人")
                  .append(roleLabel).append("：").append(client.getName()).append("\n");
                if ("ENTERPRISE".equals(client.getClientType())) {
                    if (client.getCreditCode() != null) {
                        sb.append("  统一社会信用代码：").append(client.getCreditCode()).append("\n");
                    }
                    if (client.getLegalRepresentative() != null) {
                        sb.append("  法定代表人：").append(client.getLegalRepresentative()).append("\n");
                    }
                } else {
                    if (client.getIdCard() != null) {
                        sb.append("  身份证号：").append(client.getIdCard()).append("\n");
                    }
                }
                if (client.getRegisteredAddress() != null) {
                    sb.append("  地址：").append(client.getRegisteredAddress()).append("\n");
                }
                if (client.getContactPhone() != null) {
                    sb.append("  联系电话：").append(client.getContactPhone()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // 律师团队信息
        if (participants != null && !participants.isEmpty()) {
            sb.append("【代理律师信息】\n");
            for (ParticipantInfo p : participants) {
                sb.append(p.getRole() != null ? p.getRole() : "律师")
                  .append("：").append(p.getName()).append("\n");
                if (p.getLawyerLicenseNo() != null) {
                    sb.append("  执业证号：").append(p.getLawyerLicenseNo()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // 相关文档
        if (documents != null && !documents.isEmpty()) {
            sb.append("【相关文档】\n");
            for (DocumentInfo doc : documents) {
                sb.append("- ").append(doc.getTitle()).append(" (").append(doc.getFileType()).append(")\n");
                if (doc.getContent() != null && !doc.getContent().isEmpty()) {
                    sb.append("  摘要：").append(doc.getContent()).append("\n");
                }
            }
        }
        
        return sb.toString();
    }
}

