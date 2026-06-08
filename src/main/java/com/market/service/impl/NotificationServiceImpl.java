package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Notification;
import com.market.entity.User;
import com.market.mapper.NotificationMapper;
import com.market.mapper.UserMapper;
import com.market.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Notification> listByReceiver(Long receiverId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .orderByDesc(Notification::getCreateTime));
    }

    @Override
    public List<Notification> listUnreadByReceiver(Long receiverId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreateTime));
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long receiverId) {
        Notification notification = baseMapper.selectById(notificationId);
        if (notification != null && notification.getReceiverId().equals(receiverId)) {
            notification.setIsRead(1);
            baseMapper.updateById(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long receiverId) {
        List<Notification> list = listUnreadByReceiver(receiverId);
        list.forEach(n -> n.setIsRead(1));
        updateBatchById(list);
    }

    @Override
    @Transactional
    public void createNotification(Long receiverId, String type, String content, Long businessId) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setContent(content);
        notification.setBusinessId(businessId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        baseMapper.insert(notification);
    }

    @Override
    @Transactional
    public void sendByRole(String role, String content) {
        List<User> users;
        if ("all".equals(role)) {
            // 发送给所有用户
            users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        } else {
            // 发送给指定角色（admin / vendor / user）
            users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, role)
                    .eq(User::getStatus, 1));
        }
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = users.stream().map(user -> {
            Notification n = new Notification();
            n.setReceiverId(user.getId());
            n.setType("系统通知");
            n.setContent(content);
            n.setIsRead(0);
            n.setCreateTime(now);
            return n;
        }).collect(Collectors.toList());
        saveBatch(notifications);
    }
}