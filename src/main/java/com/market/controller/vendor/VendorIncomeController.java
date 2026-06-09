package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("vendorIncomeController")
@RequestMapping("/api/vendor/income-stats")
public class VendorIncomeController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public Result<Map<String, Object>> stats() {
        Long vendorId = getCurrentUserId();
        return Result.success(orderService.getVendorIncomeStats(vendorId));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}