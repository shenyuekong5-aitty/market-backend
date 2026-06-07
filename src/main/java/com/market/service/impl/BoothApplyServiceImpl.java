package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.BoothApply;
import com.market.mapper.BoothApplyMapper;
import com.market.service.BoothApplyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoothApplyServiceImpl extends ServiceImpl<BoothApplyMapper, BoothApply> implements BoothApplyService {
    @Override
    public List<BoothApply> listPending() {
        return baseMapper.selectList(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getStatus, "待审批"));
    }
}