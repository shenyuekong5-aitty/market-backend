package com.market.controller.admin;

import com.market.common.Result;
import com.market.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminNotificationController")
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 管理员发送通知
     * @param role 目标角色：all / admin / vendor / user
     * @param content 通知内容
     */
    @PostMapping("/send")
    public Result<String> send(@RequestParam String role, @RequestParam String content) {
        notificationService.sendByRole(role, content);
        return Result.success("通知已发送");
    }
}