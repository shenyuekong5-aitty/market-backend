package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Booth;
import com.market.entity.Market;
import com.market.mapper.BoothMapper;
import com.market.mapper.MarketMapper;
import com.market.service.BoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoothServiceImpl extends ServiceImpl<BoothMapper, Booth> implements BoothService {

    @Autowired
    private MarketMapper marketMapper;

    @Override
    public List<Booth> listByMarketId(Long marketId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Booth>()
                .eq(Booth::getMarketId, marketId));
    }

    @Override
    public Booth createBooth(Booth booth, Long adminId) {
        Market market = marketMapper.selectById(booth.getMarketId());
        if (market == null || !market.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权在该集市下创建摊位");
        }
        // 管理员只负责创建摊位位置，经营信息留空或给默认值
        if (booth.getTitle() == null || booth.getTitle().isEmpty()) {
            booth.setTitle("新摊位");
        }
        booth.setStatus("空闲");
        baseMapper.insert(booth);
        return booth;
    }

    @Override
    @Transactional
    public Booth updateBooth(Booth booth, Long adminId) {
        Booth existing = baseMapper.selectById(booth.getId());
        if (existing == null) throw new RuntimeException("摊位不存在");
        // 校验集市归属
        Market market = marketMapper.selectById(existing.getMarketId());
        if (market == null || !market.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权修改该摊位");
        }
        // 只能修改非状态字段
        existing.setTitle(booth.getTitle());
        existing.setPosition(booth.getPosition());
        existing.setDescription(booth.getDescription());
        existing.setOpenTime(booth.getOpenTime());
        baseMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteBooth(Long boothId, Long adminId) {
        Booth booth = baseMapper.selectById(boothId);
        if (booth == null) throw new RuntimeException("摊位不存在");
        Market market = marketMapper.selectById(booth.getMarketId());
        if (market == null || !market.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权删除该摊位");
        }
        if (!"空闲".equals(booth.getStatus())) {
            throw new RuntimeException("只有空闲状态的摊位才能删除");
        }
        baseMapper.deleteById(boothId);
    }

    @Override
    @Transactional
    public void toggleBoothStatus(Long boothId, Long adminId) {
        Booth booth = baseMapper.selectById(boothId);
        if (booth == null) throw new RuntimeException("摊位不存在");
        Market market = marketMapper.selectById(booth.getMarketId());
        if (market == null || !market.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权操作该摊位");
        }
        // 如果当前是停用，则恢复为空闲；如果是空闲或已占用，则可以停用（但已占用不能停用？这里简单处理为停用）
        if ("停用".equals(booth.getStatus())) {
            booth.setStatus("空闲");
        } else {
            if ("已占用".equals(booth.getStatus())) {
                throw new RuntimeException("已占用的摊位不能停用");
            }
            booth.setStatus("停用");
        }
        baseMapper.updateById(booth);
    }

    //小贩端
    @Override
    public Booth getByVendorId(Long vendorId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Booth>()
                .eq(Booth::getVendorId, vendorId)
                .eq(Booth::getStatus, "已占用"));
    }

    @Override
    @Transactional
    public Booth updateByVendor(Long vendorId, Booth boothInfo) {
        Booth booth = getByVendorId(vendorId);
        if (booth == null) {
            throw new RuntimeException("您还没有已占用的摊位");
        }
        // 只允许更新经营信息
        booth.setTitle(boothInfo.getTitle());
        booth.setDescription(boothInfo.getDescription());
        booth.setOpenTime(boothInfo.getOpenTime());
        baseMapper.updateById(booth);
        return booth;
    }
}