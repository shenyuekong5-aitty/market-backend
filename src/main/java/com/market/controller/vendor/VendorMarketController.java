package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.entity.Market;
import com.market.entity.User;
import com.market.service.BoothApplyService;
import com.market.service.BoothService;
import com.market.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor")
public class VendorMarketController {

    @Autowired
    private MarketService marketService;
    @Autowired
    private BoothService boothService;
    @Autowired
    private BoothApplyService boothApplyService;

    // 获取所有启用状态的集市
    @GetMapping("/markets")
    public Result<List<Market>> listEnabledMarkets() {
        return Result.success(marketService.listEnabledMarkets());
    }

    // 获取指定集市下的空闲摊位
    @GetMapping("/markets/{marketId}/booths")
    public Result<List<Booth>> listFreeBooths(@PathVariable Long marketId) {
        Long userId = getCurrentUserId();
        return Result.success(boothService.listFreeBoothsByMarketId(marketId, userId));
    }

    // 提交入住申请（动态获取当前用户ID）
    @PostMapping("/applies")
    public Result<String> applyBooth(@RequestParam Long boothId) {
        Long userId = getCurrentUserId();
        boothApplyService.applyForBooth(userId, boothId);
        return Result.success("申请已提交");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }

    //集市中所有摊位
    @GetMapping("/markets/{marketId}/all-booths")
    public Result<List<Booth>> listAllBooths(@PathVariable Long marketId) {
        Long userId = getCurrentUserId();
        return Result.success(boothService.listAllBoothsByMarketId(marketId, userId));
    }
}