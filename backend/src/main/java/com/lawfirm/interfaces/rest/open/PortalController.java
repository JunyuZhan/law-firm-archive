package com.lawfirm.interfaces.rest.open;

import com.lawfirm.application.openapi.dto.PortalMatterDTO;
import com.lawfirm.application.openapi.service.PortalDataService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.openapi.entity.ClientAccessToken;
import com.lawfirm.domain.openapi.entity.OpenApiAccessLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 客户门户公开接口
 * 
 * 注意：此接口面向客户开放，通过令牌验证身份
 * 不使用系统内部的 JWT 认证
 */
@Slf4j
@Tag(name = "客户门户", description = "客户门户公开访问接口（通过令牌验证）")
@RestController
@RequestMapping("/open/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalDataService portalDataService;

    /**
     * 获取项目信息
     * 客户通过此接口查看其项目的进度和详情
     */
    @Operation(summary = "获取项目信息", description = "客户通过令牌查看项目详情")
    @GetMapping("/matter")
    public Result<PortalMatterDTO> getMatterInfo(
            @Parameter(description = "访问令牌", required = true)
            @RequestHeader(value = "X-Portal-Token", required = false) String headerToken,
            @Parameter(description = "访问令牌（Query参数，优先使用Header）")
            @RequestParam(value = "token", required = false) String queryToken,
            HttpServletRequest request) {
        
        // 优先使用 Header 中的令牌
        String token = headerToken != null ? headerToken : queryToken;
        if (token == null || token.isEmpty()) {
            return Result.fail("请提供访问令牌");
        }

        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            PortalMatterDTO dto = portalDataService.getMatterInfo(token, clientIp, userAgent);
            return Result.success(dto);
        } catch (BusinessException e) {
            log.warn("【Portal】访问失败: ip={}, error={}", clientIp, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 验证令牌有效性
     * 客户门户可以先调用此接口检查令牌是否有效
     */
    @Operation(summary = "验证令牌", description = "检查访问令牌是否有效")
    @GetMapping("/validate")
    public Result<TokenValidationResult> validateToken(
            @Parameter(description = "访问令牌", required = true)
            @RequestHeader(value = "X-Portal-Token", required = false) String headerToken,
            @Parameter(description = "访问令牌（Query参数）")
            @RequestParam(value = "token", required = false) String queryToken,
            HttpServletRequest request) {
        
        String token = headerToken != null ? headerToken : queryToken;
        if (token == null || token.isEmpty()) {
            return Result.success(new TokenValidationResult(false, "令牌不能为空", null, null));
        }

        String clientIp = getClientIp(request);

        try {
            ClientAccessToken accessToken = portalDataService.validateToken(token, clientIp);
            
            TokenValidationResult result = new TokenValidationResult(
                    true,
                    "令牌有效",
                    accessToken.getExpiresAt() != null ? accessToken.getExpiresAt().toString() : null,
                    accessToken.getScope() != null ? accessToken.getScope().split(",") : new String[0]
            );
            return Result.success(result);
        } catch (BusinessException e) {
            return Result.success(new TokenValidationResult(false, e.getMessage(), null, null));
        }
    }

    /**
     * 令牌验证结果
     */
    public record TokenValidationResult(
            boolean valid,
            String message,
            String expiresAt,
            String[] scopes
    ) {}

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

