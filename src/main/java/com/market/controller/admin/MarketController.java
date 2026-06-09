package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.Market;
import com.market.entity.User;
import com.market.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    // 获取当前管理员管理的集市
    @GetMapping("/my")
    public Result<Market> getMyMarket() {
        Long adminId = getCurrentAdminId();
        Market market = marketService.getByAdminId(adminId);
        return Result.success(market);
    }

    // 创建集市
    @PostMapping
    public Result<Market> createMarket(@RequestBody Market market) {
        Long adminId = getCurrentAdminId();
        Market newMarket = marketService.createMarket(market, adminId);
        return Result.success(newMarket);
    }

    // 更新集市
    @PutMapping("/{id}")
    public Result<Market> updateMarket(@PathVariable Long id, @RequestBody Market market) {
        market.setId(id);
        Long adminId = getCurrentAdminId();
        Market updated = marketService.updateMarket(market, adminId);
        return Result.success(updated);
    }

    // 切换集市状态
    @PutMapping("/{id}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        marketService.toggleMarketStatus(id, adminId);
        return Result.success("状态已更新");
    }

    // 从 SecurityContext 中获取当前登录用户的 ID
    private Long getCurrentAdminId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}