package com.market.controller.common;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
}