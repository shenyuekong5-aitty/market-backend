package com.market.controller.common;

import com.market.common.Result;
import com.market.entity.Notification;
import com.market.entity.User;
import com.market.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 获取当前用户的所有通知
    @GetMapping
    public Result<List<Notification>> list() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.listByReceiver(userId));
    }

    // 获取未读通知数量
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.listUnreadByReceiver(userId).size());
    }

    // 标记单条已读
    @PutMapping("/{id}/read")
    public Result<String> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success("已标记已读");
    }

    // 全部标记已读
    @PutMapping("/read-all")
    public Result<String> markAllRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标记已读");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}