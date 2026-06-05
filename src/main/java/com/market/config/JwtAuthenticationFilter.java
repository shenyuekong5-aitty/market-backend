package com.market.config;

import com.market.common.JwtUtils;
import com.market.entity.User;
import com.market.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 每次请求到达时，检查请求头中的 Authorization，解析出当前用户，并存入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头获取 token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);  // 去掉 "Bearer " 前缀
            // 2. 验证 token 有效性
            if (jwtUtils.validateToken(token)) {
                // 3. 从 token 中提取用户名
                String username = jwtUtils.parseToken(token).getSubject();
                // 4. 从数据库加载用户
                User user = userService.getByUsername(username);
                if (user != null) {
                    // 5. 将用户信息封装为 Authentication，存入 SecurityContext
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        // 放行请求，继续后续处理
        filterChain.doFilter(request, response);
    }
}