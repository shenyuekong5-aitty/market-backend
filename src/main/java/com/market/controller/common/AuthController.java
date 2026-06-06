package com.market.controller.common;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 注册
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user, @RequestParam String code) {
        userService.register(user, code);
        return Result.success("注册成功");
    }

    // 登录
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String role) {
        String token = userService.login(username, password, role);
        return Result.success(token);
    }

    // 获取当前登录用户信息（暂时返回 null，等 JWT 过滤器完成后实现）
    @GetMapping("/currentUser")
    public Result<User> currentUser() {
        return Result.success(userService.getCurrentUser());
    }

    // 修改密码--登录状态，根据旧密码
    @PutMapping("/change-password")
    public Result<String> changePassword(@RequestParam String oldPassword,
                                         @RequestParam String newPassword) {
        // 从 SecurityContext 中获取当前用户名
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = null;
        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        if (username == null) {
            throw new RuntimeException("用户未登录");
        }
        userService.changePassword(username, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }
}