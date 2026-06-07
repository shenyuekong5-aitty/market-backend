package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.Market;
import com.market.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @GetMapping("/my")
    public Result<Market> getMyMarket() {
        // 暂时写死管理员ID=1，后续改为动态获取
        Market market = marketService.getByAdminId(1L);
        return Result.success(market);
    }

    @PostMapping
    public Result<Market> createMarket(@RequestBody Market market) {
        // 当前管理员ID暂写1
        Market newMarket = marketService.createMarket(market, 1L);
        return Result.success(newMarket);
    }

    @PutMapping("/{id}")
    public Result<Market> updateMarket(@PathVariable Long id, @RequestBody Market market) {
        market.setId(id);
        Market updated = marketService.updateMarket(market, 1L);
        return Result.success(updated);
    }

    @PutMapping("/{id}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long id) {
        marketService.toggleMarketStatus(id, 1L);
        return Result.success("状态已更新");
    }
}