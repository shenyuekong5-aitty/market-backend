package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Notification;
import java.util.List;

public interface NotificationService extends IService<Notification> {
    List<Notification> listByReceiver(Long receiverId);
    List<Notification> listUnreadByReceiver(Long receiverId);
    void markAsRead(Long notificationId, Long receiverId);
    void markAllAsRead(Long receiverId);
    void createNotification(Long receiverId, String type, String content, Long businessId);

    /**
     * 管理员发送通知（支持按角色或全体发送）
     * @param role 目标角色：all / admin / vendor / user
     * @param content 通知内容
     */
    void sendByRole(String role, String content);
}