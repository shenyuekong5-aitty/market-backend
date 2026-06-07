package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.BoothApplyDTO;
import com.market.entity.Booth;
import com.market.entity.BoothApply;
import com.market.entity.User;
import com.market.mapper.BoothApplyMapper;
import com.market.mapper.BoothMapper;
import com.market.mapper.UserMapper;
import com.market.service.BoothApplyService;
import com.market.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoothApplyServiceImpl extends ServiceImpl<BoothApplyMapper, BoothApply> implements BoothApplyService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BoothMapper boothMapper;
    @Autowired
    private OperationLogService operationLogService;

    @Override
    public List<BoothApply> listPending() {
        return baseMapper.selectList(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getStatus, "待审批"));
    }

    @Override
    public List<BoothApplyDTO> listPendingWithDetails() {
        List<BoothApply> applies = baseMapper.selectList(
                new LambdaQueryWrapper<BoothApply>().eq(BoothApply::getStatus, "待审批")
        );

        return applies.stream().map(apply -> {
            BoothApplyDTO dto = new BoothApplyDTO();
            dto.setId(apply.getId());
            dto.setType(apply.getType());
            dto.setVendorId(apply.getVendorId());
            dto.setTargetBoothId(apply.getTargetBoothId());
            dto.setStatus(apply.getStatus());
            dto.setApplyTime(apply.getApplyTime());

            // 查询申请人姓名
            User vendor = userMapper.selectById(apply.getVendorId());
            dto.setVendorName(vendor != null ? vendor.getNickname() : "未知用户");

            // 查询目标摊位标题
            Booth booth = boothMapper.selectById(apply.getTargetBoothId());
            dto.setTargetBoothTitle(booth != null ? booth.getTitle() : "未知摊位");

            return dto;
        }).collect(Collectors.toList());
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

        // 记录操作日志
        operationLogService.saveLog(adminId, "审批通过", "通过了摊位申请 ID:" + applyId);
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

        // 记录操作日志
        operationLogService.saveLog(adminId, "审批拒绝", "拒绝了摊位申请 ID:" + applyId);
    }
}