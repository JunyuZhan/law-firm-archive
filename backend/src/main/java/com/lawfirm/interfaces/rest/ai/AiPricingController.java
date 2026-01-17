package com.lawfirm.interfaces.rest.ai;

import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.ai.entity.AiPricingConfig;
import com.lawfirm.domain.ai.repository.AiPricingConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI定价配置 Controller
 */
@Slf4j
@Tag(name = "AI定价配置", description = "AI模型定价配置管理接口")
@RestController
@RequestMapping("/ai/pricing")
@RequiredArgsConstructor
public class AiPricingController {

    private final AiPricingConfigRepository pricingConfigRepository;

    /**
     * 获取所有定价配置
     */
    @Operation(summary = "定价配置列表", description = "获取所有AI模型的定价配置")
    @GetMapping
    @RequirePermission("ai:pricing:manage")
    public Result<List<AiPricingConfig>> list() {
        List<AiPricingConfig> list = pricingConfigRepository.findAll();
        return Result.success(list);
    }

    /**
     * 获取单个定价配置
     */
    @Operation(summary = "获取定价配置", description = "根据ID获取定价配置详情")
    @GetMapping("/{id}")
    @RequirePermission("ai:pricing:manage")
    public Result<AiPricingConfig> getById(@PathVariable Long id) {
        AiPricingConfig config = pricingConfigRepository.findById(id);
        return Result.success(config);
    }

    /**
     * 创建定价配置
     */
    @Operation(summary = "创建定价配置", description = "新增AI模型定价配置")
    @PostMapping
    @RequirePermission("ai:pricing:manage")
    @OperationLog(module = "AI定价配置", action = "创建定价")
    public Result<AiPricingConfig> create(@RequestBody @Valid AiPricingConfig config) {
        pricingConfigRepository.save(config);
        return Result.success(config);
    }

    /**
     * 更新定价配置
     */
    @Operation(summary = "更新定价配置", description = "修改AI模型定价配置")
    @PutMapping("/{id}")
    @RequirePermission("ai:pricing:manage")
    @OperationLog(module = "AI定价配置", action = "更新定价")
    public Result<AiPricingConfig> update(@PathVariable Long id, @RequestBody @Valid AiPricingConfig config) {
        config.setId(id);
        pricingConfigRepository.update(config);
        return Result.success(config);
    }

    /**
     * 删除定价配置
     */
    @Operation(summary = "删除定价配置", description = "删除AI模型定价配置")
    @DeleteMapping("/{id}")
    @RequirePermission("ai:pricing:manage")
    @OperationLog(module = "AI定价配置", action = "删除定价")
    public Result<Void> delete(@PathVariable Long id) {
        pricingConfigRepository.deleteById(id);
        return Result.success();
    }
}
