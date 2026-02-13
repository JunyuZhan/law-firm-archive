package com.archivesystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 开放API接口（供外部系统调用）
                        .requestMatchers("/api/open/**").permitAll()
                        // 健康检查
                        .requestMatchers("/api/actuator/**").permitAll()
                        // API文档
                        .requestMatchers("/api/doc.html", "/api/swagger-ui/**", "/api/v3/api-docs/**", "/api/webjars/**").permitAll()
                        // 其他接口需要认证（暂时开放，后续添加JWT认证）
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
