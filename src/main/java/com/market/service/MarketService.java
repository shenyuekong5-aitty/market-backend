package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Market;

public interface MarketService extends IService<Market> {
    Market getByAdminId(Long adminId);
}