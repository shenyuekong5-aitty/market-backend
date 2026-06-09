package com.market.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/notification/{userId}")
@Component
public class NotificationEndpoint {

    private static final Map<String, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        ONLINE_SESSIONS.put(userId, session);
        System.out.println("WebSocket 连接建立：userId=" + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 客户端发来的消息，暂时不用处理
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        ONLINE_SESSIONS.remove(userId);
        System.out.println("WebSocket 连接关闭：userId=" + userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket 错误：" + error.getMessage());
    }

    public static void sendToUser(String userId, String message) {
        Session session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void broadcast(String message) {
        ONLINE_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}