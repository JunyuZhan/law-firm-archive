package com.lawfirm.application.system.service;

import com.lawfirm.domain.system.entity.CauseOfAction;
import com.lawfirm.infrastructure.persistence.mapper.CauseOfActionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 案由/罪名服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CauseOfActionService {

    private final CauseOfActionMapper causeOfActionMapper;

    /**
     * 案由类型常量
     */
    public static final String TYPE_CIVIL = "CIVIL";      // 民事
    public static final String TYPE_CRIMINAL = "CRIMINAL"; // 刑事
    public static final String TYPE_ADMIN = "ADMIN";       // 行政

    /**
     * 根据代码获取案由名称
     * 如果找不到，返回原代码
     */
    @Cacheable(value = "causeName", key = "#code + '_' + #causeType")
    public String getCauseName(String code, String causeType) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        
        // 如果已经是中文名称，直接返回
        if (isChineseName(code)) {
            return code;
        }
        
        String name = causeOfActionMapper.findNameByCode(code, causeType);
        return StringUtils.hasText(name) ? name : code;
    }

    /**
     * 获取民事案由名称
     */
    public String getCivilCauseName(String code) {
        return getCauseName(code, TYPE_CIVIL);
    }

    /**
     * 获取刑事罪名
     */
    public String getCriminalChargeName(String code) {
        return getCauseName(code, TYPE_CRIMINAL);
    }

    /**
     * 获取行政案由名称
     */
    public String getAdminCauseName(String code) {
        return getCauseName(code, TYPE_ADMIN);
    }

    /**
     * 获取所有民事案由（树形结构）
     */
    @Cacheable(value = "causeTree", key = "'civil'")
    public List<CauseTreeNode> getCivilCauseTree() {
        return buildCauseTree(TYPE_CIVIL);
    }

    /**
     * 获取所有刑事罪名（树形结构）
     */
    @Cacheable(value = "causeTree", key = "'criminal'")
    public List<CauseTreeNode> getCriminalChargeTree() {
        return buildCauseTree(TYPE_CRIMINAL);
    }

    /**
     * 获取所有行政案由（树形结构）
     */
    @Cacheable(value = "causeTree", key = "'admin'")
    public List<CauseTreeNode> getAdminCauseTree() {
        return buildCauseTree(TYPE_ADMIN);
    }

    /**
     * 搜索案由
     */
    public List<CauseOfAction> searchCauses(String causeType, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        return causeOfActionMapper.searchByName(causeType, keyword);
    }

    /**
     * 构建树形结构
     */
    private List<CauseTreeNode> buildCauseTree(String causeType) {
        List<CauseOfAction> allCauses = causeOfActionMapper.findAllByType(causeType);
        
        // 按分类分组
        Map<String, List<CauseOfAction>> groupedByCategory = allCauses.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCategoryCode() + "|" + c.getCategoryName(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        
        List<CauseTreeNode> result = new ArrayList<>();
        
        for (Map.Entry<String, List<CauseOfAction>> entry : groupedByCategory.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String categoryCode = parts[0];
            String categoryName = parts.length > 1 ? parts[1] : categoryCode;
            
            CauseTreeNode categoryNode = new CauseTreeNode();
            categoryNode.setCode(categoryCode);
            categoryNode.setName(categoryName);
            categoryNode.setLevel(0);
            categoryNode.setChildren(new ArrayList<>());
            
            // 分离一级和二级案由
            List<CauseOfAction> level1Causes = entry.getValue().stream()
                .filter(c -> c.getLevel() == null || c.getLevel() == 1)
                .collect(Collectors.toList());
            
            List<CauseOfAction> level2Causes = entry.getValue().stream()
                .filter(c -> c.getLevel() != null && c.getLevel() == 2)
                .collect(Collectors.toList());
            
            // 构建一级案由节点
            for (CauseOfAction cause : level1Causes) {
                CauseTreeNode node = new CauseTreeNode();
                node.setCode(cause.getCode());
                node.setName(cause.getName());
                node.setLevel(1);
                node.setChildren(new ArrayList<>());
                
                // 添加子案由
                String parentCode = cause.getCode();
                for (CauseOfAction child : level2Causes) {
                    if (child.getCode().startsWith(parentCode + ".")) {
                        CauseTreeNode childNode = new CauseTreeNode();
                        childNode.setCode(child.getCode());
                        childNode.setName(child.getName());
                        childNode.setLevel(2);
                        node.getChildren().add(childNode);
                    }
                }
                
                categoryNode.getChildren().add(node);
            }
            
            result.add(categoryNode);
        }
        
        return result;
    }

    /**
     * 判断是否已经是中文名称
     */
    private boolean isChineseName(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        // 如果包含中文字符，认为是名称而非代码
        return text.chars().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
    }

    /**
     * 案由树节点
     */
    @lombok.Data
    public static class CauseTreeNode {
        private String code;
        private String name;
        private Integer level;
        private List<CauseTreeNode> children;
    }
}
