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

    /**
     * 获取当前管理员管理的集市
     */
    @GetMapping("/my")
    public Result<Market> getMyMarket() {
        // 从SecurityContext中获取当前管理员ID
        // 目前先返回管理员ID为1的集市（aitty）
        Market market = marketService.getByAdminId(1L);  // 暂时写死，后续改为动态获取
        return Result.success(market);
    }
}