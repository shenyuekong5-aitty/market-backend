package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("adminIncomeController")
@RequestMapping("/api/admin/income-stats")
public class AdminIncomeController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public Result<Map<String, Object>> stats() {
        Long adminId = getCurrentUserId();
        return Result.success(orderService.getAdminIncomeStats(adminId));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}