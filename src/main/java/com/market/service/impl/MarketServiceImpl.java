package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Market;
import com.market.mapper.MarketMapper;
import com.market.service.MarketService;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl extends ServiceImpl<MarketMapper, Market> implements MarketService {
    @Override
    public Market getByAdminId(Long adminId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Market>().eq(Market::getAdminId, adminId));
    }
}