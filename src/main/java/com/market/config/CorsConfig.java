package com.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置
 * 解决前后端分离开发时，浏览器拦截跨域请求的问题（前端 5173 端口 -> 后端 8088 端口）
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建跨域配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 2. 默认只允许本机前端；部署时用 app.cors.allowed-origins 配置实际域名。
        for (String origin : allowedOrigins.split(",")) {
            String value = origin.trim();
            if (!value.isEmpty()) {
                config.addAllowedOrigin(value);
            }
        }

        // 3. 允许所有 HTTP 方法（GET, POST, PUT, DELETE 等）
        config.addAllowedMethod("*");

        // 4. 允许所有请求头
        config.addAllowedHeader("*");

        // 5. 允许携带凭证（如 Cookie、Authorization 头）
        config.setAllowCredentials(true);

        // 6. 创建跨域配置源，并注册针对所有路径的跨域规则
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 7. 返回跨域过滤器
        return new CorsFilter(source);
    }
}
