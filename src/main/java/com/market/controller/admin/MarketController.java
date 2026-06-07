package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.Market;
import com.market.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}