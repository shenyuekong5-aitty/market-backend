package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/manage")
public class AdminManageController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public Result<String> createAdmin(@RequestBody User user) {
        User currentUser = getCurrentUser();
        userService.createAdmin(currentUser.getId(), user);
        return Result.success("管理员账号创建成功");
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new RuntimeException("用户未登录");
    }
}