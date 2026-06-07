package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Market;

import java.util.List;

public interface MarketService extends IService<Market> {
    Market getByAdminId(Long adminId);
    Market createMarket(Market market, Long adminId);
    Market updateMarket(Market market, Long adminId);
    void toggleMarketStatus(Long marketId, Long adminId);

    //允许的集市列表
    List<Market> listEnabledMarkets();
}