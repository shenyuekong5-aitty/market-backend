package com.market.websocket;

import com.market.common.JwtUtils;
import com.market.common.UserRole;
import com.market.config.SpringContextHolder;
import com.market.entity.User;
import com.market.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/notification/{userId}")
@Component
public class NotificationEndpoint {

    private static final Logger log = LoggerFactory.getLogger(NotificationEndpoint.class);
    private static final Map<String, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        String token = session.getRequestParameterMap().getOrDefault("token", java.util.Collections.emptyList())
                .stream().findFirst().orElse(null);
        if (!isAuthorized(userId, token)) {
            closeUnauthorized(session);
            return;
        }
        ONLINE_SESSIONS.put(userId, session);
        log.debug("WebSocket 连接建立：userId={}", userId);
    }

    private boolean isAuthorized(String userId, String token) {
        try {
            JwtUtils jwtUtils = SpringContextHolder.getBean(JwtUtils.class);
            UserService userService = SpringContextHolder.getBean(UserService.class);
            if (token == null || !jwtUtils.validateToken(token)) return false;
            Claims claims = jwtUtils.parseToken(token);
            User user = userService.getByUsername(claims.getSubject());
            return user != null && user.getStatus() == 1
                    && String.valueOf(user.getId()).equals(userId)
                    && UserRole.effectiveRole(user).equals(claims.get("role", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private void closeUnauthorized(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "未授权的 WebSocket 连接"));
        } catch (IOException ignored) {
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 客户端发来的消息，暂时不用处理
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        ONLINE_SESSIONS.remove(userId, session);
        log.debug("WebSocket 连接关闭：userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("WebSocket 错误：{}", error.getMessage());
    }

    public static void sendToUser(String userId, String message) {
        Session session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.warn("WebSocket 消息发送失败：userId={}", userId, e);
            }
        }
    }

    public static void broadcast(String message) {
        ONLINE_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.warn("WebSocket 广播发送失败：userId={}", userId, e);
                }
            }
        });
    }
}
