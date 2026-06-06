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
}