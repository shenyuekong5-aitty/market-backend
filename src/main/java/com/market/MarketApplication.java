package com.market;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 本项目使用 JwtAuthenticationFilter 完成认证，不启用 Spring Security 的临时内存账号。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.market.mapper")
@EnableScheduling
public class MarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketApplication.class, args);
    }
}
