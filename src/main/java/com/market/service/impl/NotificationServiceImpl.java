package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Notification;
import com.market.mapper.NotificationMapper;
import com.market.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

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
}