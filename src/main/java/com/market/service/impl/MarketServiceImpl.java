package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Market;
import com.market.mapper.MarketMapper;
import com.market.service.MarketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketServiceImpl extends ServiceImpl<MarketMapper, Market> implements MarketService {

    @Override
    public Market getByAdminId(Long adminId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Market>().eq(Market::getAdminId, adminId));
    }

    @Override
    @Transactional
    public Market createMarket(Market market, Long adminId) {
        // 检查管理员是否已有集市
        Market existing = getByAdminId(adminId);
        if (existing != null) {
            throw new RuntimeException("您已经管理了一个集市，不能重复创建");
        }
        market.setAdminId(adminId);
        market.setStatus(1); // 默认启用
        baseMapper.insert(market);
        return market;
    }

    @Override
    @Transactional
    public Market updateMarket(Market market, Long adminId) {
        Market existing = getByAdminId(adminId);
        if (existing == null || !existing.getId().equals(market.getId())) {
            throw new RuntimeException("无权修改该集市");
        }
        // 只允许修改名称和位置
        existing.setName(market.getName());
        existing.setLocation(market.getLocation());
        baseMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void toggleMarketStatus(Long marketId, Long adminId) {
        Market existing = baseMapper.selectById(marketId);
        if (existing == null || !existing.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权操作该集市");
        }
        // 切换状态：启用 -> 停用，停用 -> 启用
        existing.setStatus(existing.getStatus() == 1 ? 0 : 1);
        baseMapper.updateById(existing);
    }
}