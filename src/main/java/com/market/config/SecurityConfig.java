package com.market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

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

                // 4. 统一认证和角色授权
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeJson(response, 401, "请先登录"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeJson(response, 403, "没有权限访问")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/check-phone",
                                "/api/sms/send",
                                "/api/sms/send-reset",
                                "/uploads/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vendor/markets", "/api/user/booths/**", "/api/user/products").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/vendor/products/**",
                                "/api/vendor/booth/**",
                                "/api/vendor/reservations/**",
                                "/api/vendor/orders/**",
                                "/api/vendor/income-stats"
                        ).hasRole("VENDOR")
                        .requestMatchers("/api/vendor/**").authenticated()
                        .requestMatchers(
                                "/api/user/**",
                                "/api/notifications/**",
                                "/api/account/**",
                                "/api/upload/**",
                                "/api/sms/send-change-phone"
                        ).authenticated()
                        .anyRequest().authenticated()
                )

                // 5. 在用户名密码过滤器之前插入我们的 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
