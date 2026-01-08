package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.service.MyFinanceService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 律师视角的财务接口
 * 提供"我的收款"、"我的提成"等功能
 */
@Slf4j
@RestController
@RequestMapping("/finance/my")
@RequiredArgsConstructor
public class MyFinanceController {

    private final MyFinanceService myFinanceService;

    /**
     * 获取我参与的合同收款情况
     */
    @GetMapping("/payments")
    @RequirePermission("finance:my:payment")
    public Result<List<Map<String, Object>>> getMyPayments() {
        try {
            log.info("获取我的收款记录");
            List<Map<String, Object>> result = myFinanceService.getMyContractPayments();
            log.info("获取我的收款记录成功，数量: {}", result != null ? result.size() : 0);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的收款记录失败", e);
            return Result.error("获取收款记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的提成记录
     */
    @GetMapping("/commissions")
    @RequirePermission("finance:my:commission")
    public Result<List<Map<String, Object>>> getMyCommissions(
            @RequestParam(required = false) String status) {
        try {
            log.info("获取我的提成记录, status: {}", status);
            List<Map<String, Object>> result = myFinanceService.getMyCommissions(status);
            log.info("获取我的提成记录成功，数量: {}", result != null ? result.size() : 0);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的提成记录失败", e);
            return Result.error("获取提成记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的费用报销记录
     */
    @GetMapping("/expenses")
    @RequirePermission("finance:expense:apply")
    public Result<List<Map<String, Object>>> getMyExpenses() {
        try {
            log.info("获取我的费用报销记录");
            List<Map<String, Object>> result = myFinanceService.getMyExpenses();
            log.info("获取我的费用报销记录成功，数量: {}", result != null ? result.size() : 0);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的费用报销记录失败", e);
            return Result.error("获取费用报销记录失败: " + e.getMessage());
        }
    }
}
