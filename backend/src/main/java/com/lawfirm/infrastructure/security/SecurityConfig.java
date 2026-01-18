package com.lawfirm.infrastructure.security;

import com.lawfirm.infrastructure.security.handler.AuthenticationEntryPointImpl;
import com.lawfirm.infrastructure.security.handler.AccessDeniedHandlerImpl;
import com.lawfirm.infrastructure.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.Arrays;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final Environment environment;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT无需CSRF）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用Session（无状态）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // 配置安全响应头
            // X-Frame-Options: SAMEORIGIN - 允许同源页面嵌入 iframe，防止外部网站点击劫持
            // OnlyOffice 编辑器需要通过 iframe 加载文档内容，因此必须允许同源嵌入
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // 请求授权配置
            .authorizeHttpRequests(auth -> {
                // Swagger UI 仅在开发/测试环境开放
                boolean isDevOrTest = Arrays.asList(environment.getActiveProfiles()).contains("dev") 
                    || Arrays.asList(environment.getActiveProfiles()).contains("test");
                
                if (isDevOrTest) {
                    // 开发/测试环境：包含 Swagger UI 和测试接口
                    auth.requestMatchers(
                    "/auth/login",
                    "/auth/logout",           // 登出接口允许未认证访问（token 可能已过期）
                    "/auth/refresh",
                    "/auth/captcha",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/error",
                    "/test/**",              // 测试接口（仅开发/测试环境）
                    "/document/*/callback",  // OnlyOffice 回调接口（无需认证）
                    "/document/*/content",   // OnlyOffice 文件代理接口（通过 token 验证）
                    "/document/*/file-proxy", // OnlyOffice 文件代理接口（通过 token 或 IP 验证）
                    "/open/portal/**"        // 客户门户接口（通过令牌验证）
                    ).permitAll();
                } else {
                    // 生产环境：不包含 Swagger UI
                    auth.requestMatchers(
                        "/auth/login",
                        "/auth/logout",           // 登出接口允许未认证访问（token 可能已过期）
                        "/auth/refresh",
                        "/auth/captcha",
                        "/actuator/health",
                        "/error",
                        "/document/*/callback",  // OnlyOffice 回调接口（无需认证）
                        "/document/*/content",   // OnlyOffice 文件代理接口（通过 token 验证）
                        "/document/*/file-proxy", // OnlyOffice 文件代理接口（通过 token 或 IP 验证）
                        "/open/portal/**"        // 客户门户接口（通过令牌验证）
                    ).permitAll();
                }
                
                // 其他请求需要认证
                auth.anyRequest().authenticated();
            })
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
