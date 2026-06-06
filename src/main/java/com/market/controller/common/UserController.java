package com.market.controller.common;

import com.market.common.Result;
import com.market.dto.UpdateProfileRequest;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(request);
        return Result.success(updatedUser);
    }

    /**
     * 注销当前登录账号
     */
    @PostMapping("/deactivate")
    public Result<String> deactivate() {
        // 从 SecurityContext 中获取当前用户
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = null;
        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else {
            // 如果你的 UserDetails 实现里存了用户ID，可自行获取
            throw new RuntimeException("无法获取当前用户");
        }

        userService.deactivateAccount(userId);
        return Result.success("账号已注销");
    }

    /**
     * 检查当前账号是否可注销（可选前端调用）
     */
    @GetMapping("/can-deactivate")
    public Result<Boolean> canDeactivate() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = null;
        if (principal instanceof User) {
            userId = ((User) principal).getId();
        }
        boolean can = userService.canDeactivate(userId);
        return Result.success(can);
    }
}


