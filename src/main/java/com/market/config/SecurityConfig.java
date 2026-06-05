package com.market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 * 负责密码加密、请求拦截规则、Session策略等
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    /**
     * 安全过滤器链
     * 配置哪些请求需要认证，哪些可以直接访问
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 启用 CORS 支持（使用我们自定义的 CorsFilter）
                .cors(cors -> {})

                // 2. 关闭 CSRF 保护（前后端分离项目常用，因为我们用 JWT 保证安全）
                .csrf(csrf -> csrf.disable())

                // 3. 设置为无状态会话（不使用 Session，而是用 JWT token 识别用户）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 授权规则：暂时所有请求都放行（开发阶段，后续再严格限制）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()   // 所有接口无需登录即可访问
                )

                // 5. 在用户名密码过滤器之前插入我们的 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}