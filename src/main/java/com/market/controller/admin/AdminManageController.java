package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/manage")
public class AdminManageController {

    @Autowired
    private UserService userService;

    // 获取所有管理员列表
    @GetMapping("/list")
    public Result<List<User>> listAdmins() {
        User currentUser = getCurrentUser();
        return Result.success(userService.listAllAdmins(currentUser.getId()));
    }

    // 创建管理员（已存在）
    @PostMapping("/create")
    public Result<String> createAdmin(@RequestBody User user) {
        User currentUser = getCurrentUser();
        userService.createAdmin(currentUser.getId(), user);
        return Result.success("管理员账号创建成功");
    }

    // 切换管理员状态（启用/停用）
    @PutMapping("/{adminId}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long adminId) {
        User currentUser = getCurrentUser();
        userService.toggleAdminStatus(currentUser.getId(), adminId);
        return Result.success("状态已更新");
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new RuntimeException("用户未登录");
    }
}