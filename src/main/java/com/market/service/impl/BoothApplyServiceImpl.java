package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Booth;
import com.market.entity.BoothApply;
import com.market.entity.User;
import com.market.mapper.BoothApplyMapper;
import com.market.mapper.BoothMapper;
import com.market.mapper.UserMapper;
import com.market.service.BoothApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BoothApplyServiceImpl extends ServiceImpl<BoothApplyMapper, BoothApply> implements BoothApplyService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BoothMapper boothMapper;

    @Override
    public List<BoothApply> listPending() {
        return baseMapper.selectList(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getStatus, "待审批"));
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long adminId) {
        BoothApply apply = baseMapper.selectById(applyId);
        if (apply == null || !"待审批".equals(apply.getStatus())) {
            throw new RuntimeException("申请不存在或已处理");
        }
        apply.setStatus("通过");
        apply.setApproveAdminId(adminId);
        apply.setProcessTime(LocalDateTime.now());
        baseMapper.updateById(apply);

        if ("入住".equals(apply.getType())) {
            Booth booth = boothMapper.selectById(apply.getTargetBoothId());
            if (booth == null || !"空闲".equals(booth.getStatus())) {
                throw new RuntimeException("目标摊位不可用");
            }
            booth.setVendorId(apply.getVendorId());
            booth.setStatus("已占用");
            boothMapper.updateById(booth);

            User user = userMapper.selectById(apply.getVendorId());
            if (user != null && !"vendor".equals(user.getRole())) {
                user.setRole("vendor");
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public void reject(Long applyId, Long adminId) {
        BoothApply apply = baseMapper.selectById(applyId);
        if (apply == null || !"待审批".equals(apply.getStatus())) {
            throw new RuntimeException("申请不存在或已处理");
        }
        apply.setStatus("拒绝");
        apply.setApproveAdminId(adminId);
        apply.setProcessTime(LocalDateTime.now());
        baseMapper.updateById(apply);
    }
}