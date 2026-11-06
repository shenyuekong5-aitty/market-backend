package com.market.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理通知模块的 SSE 长连接。
 *
 * <p>SSE 作为 WebSocket 的降级通道，只推送“有新通知”的轻量事件；
 * 通知详情仍由前端通过普通 HTTP 接口读取。</p>
 */
@Service
public class NotificationSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);
    private static final long CONNECTION_TIMEOUT = 30 * 60 * 1000L;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException | IllegalStateException e) {
            remove(userId, emitter);
            // 浏览器切换页面或 WebSocket 恢复时会主动关闭 SSE，按正常结束处理，
            // 避免把客户端断开记录成服务端异常。
            emitter.complete();
            log.debug("SSE 连接建立后已被客户端关闭：userId={}", userId);
        }
        return emitter;
    }

    public void sendToUser(Long userId, String message) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
                emitter.complete();
                log.debug("SSE 通知连接已失效：userId={}", userId);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (ignored, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }
}
