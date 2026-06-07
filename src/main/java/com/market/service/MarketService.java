package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Market;

public interface MarketService extends IService<Market> {
    Market getByAdminId(Long adminId);
    Market createMarket(Market market, Long adminId);
    Market updateMarket(Market market, Long adminId);
    void toggleMarketStatus(Long marketId, Long adminId);
}