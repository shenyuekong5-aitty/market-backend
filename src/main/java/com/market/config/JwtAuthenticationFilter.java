package com.market.config;

import com.market.common.JwtUtils;
import com.market.entity.User;
import com.market.service.UserService;
import io.jsonwebtoken.Claims;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!jwtUtils.validateToken(token)) {
                // 直接返回 401，不继续走过滤器链
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"token已过期或无效，请重新登录\"}");
                return;   // 关键：立即结束，不执行 filterChain.doFilter
            }

            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            String tokenRole = claims.get("role", String.class);

            User user = userService.getByUsername(username);
            if (user != null && user.getStatus() == 1) {
                if (!user.getRole().equals(tokenRole)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"角色已变更，请重新登录\"}");
                    return;
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}