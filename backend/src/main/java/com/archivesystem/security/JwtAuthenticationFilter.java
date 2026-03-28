package com.archivesystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                // 检查Token是否在黑名单中
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("Token已在黑名单中，拒绝访问");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 检查用户Token是否被全部吊销
                Claims claims = jwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                long issuedAt = claims.getIssuedAt().getTime();
                
                if (tokenBlacklistService.isUserBlacklisted(userId, issuedAt)) {
                    log.warn("用户Token已被全部吊销，拒绝访问: userId={}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }
                
                String username = jwtUtils.getUsernameFromToken(token);
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期");
        } catch (Exception e) {
            log.error("无法设置用户认证", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
