package com.market.controller.common;

import com.market.common.Result;
import com.market.dto.SecurityCheckResult;
import com.market.entity.User;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private UserService userService;

    @GetMapping("/security-check")
    public Result<SecurityCheckResult> securityCheck() {
        User currentUser = userService.getCurrentUser();
        SecurityCheckResult result = userService.performSecurityCheck(currentUser);
        return Result.success(result);
    }
}