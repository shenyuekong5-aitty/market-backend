package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.BoothApplyDTO;
import com.market.entity.*;
import com.market.mapper.*;
import com.market.service.BoothApplyService;
import com.market.service.NotificationService;
import com.market.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private NotificationService notificationService;

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

            User vendor = userMapper.selectById(apply.getVendorId());
            dto.setVendorName(vendor != null ? vendor.getNickname() : "未知用户");

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

        String type = apply.getType();
        if ("入住".equals(type)) {
            handleApproveMoveIn(apply);
            // 发送通知：申请通过，成为小贩
            notificationService.createNotification(
                    apply.getVendorId(),
                    "申请结果",
                    "您的摊位入住申请已通过审批，您已成为小贩。",
                    apply.getId()
            );
        } else if ("更换".equals(type)) {
            handleApproveChange(apply);
            // 发送通知：更换成功
            notificationService.createNotification(
                    apply.getVendorId(),
                    "申请结果",
                    "您的摊位更换申请已通过审批，新摊位已分配。",
                    apply.getId()
            );
        } else if ("归还".equals(type)) {
            handleApproveReturn(apply);
            // 发送通知：归还成功，角色可能变更
            notificationService.createNotification(
                    apply.getVendorId(),
                    "申请结果",
                    "您的摊位归还申请已通过审批。",
                    apply.getId()
            );
        }

        operationLogService.saveLog(adminId, "审批通过", "通过了摊位申请 ID:" + applyId + " 类型:" + type);
    }

    private void handleApproveMoveIn(BoothApply apply) {
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

    private void handleApproveChange(BoothApply apply) {
        Booth originBooth = boothMapper.selectById(apply.getOriginBoothId());
        if (originBooth != null) {
            clearBoothData(originBooth.getId());
            originBooth.setVendorId(null);
            originBooth.setStatus("空闲");
            boothMapper.updateById(originBooth);
        }
        Booth targetBooth = boothMapper.selectById(apply.getTargetBoothId());
        if (targetBooth == null || !"空闲".equals(targetBooth.getStatus())) {
            throw new RuntimeException("目标摊位不可用");
        }
        targetBooth.setVendorId(apply.getVendorId());
        targetBooth.setStatus("已占用");
        boothMapper.updateById(targetBooth);
    }

    private void handleApproveReturn(BoothApply apply) {
        Booth booth = boothMapper.selectById(apply.getOriginBoothId());
        if (booth != null) {
            clearBoothData(booth.getId());
            booth.setVendorId(null);
            booth.setStatus("空闲");
            boothMapper.updateById(booth);

            Long otherBoothCount = boothMapper.selectCount(new LambdaQueryWrapper<Booth>()
                    .eq(Booth::getVendorId, apply.getVendorId())
                    .eq(Booth::getStatus, "已占用"));
            if (otherBoothCount == 0) {
                User user = userMapper.selectById(apply.getVendorId());
                if (user != null && "vendor".equals(user.getRole())) {
                    user.setRole("user");
                    userMapper.updateById(user);
                }
            }
        }
    }

    private void clearBoothData(Long boothId) {
        // 先查出该摊位下的所有商品ID
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getBoothId, boothId));
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());

        // 删除这些商品对应的购物车记录
        if (!productIds.isEmpty()) {
            cartMapper.delete(new LambdaQueryWrapper<Cart>()
                    .in(Cart::getProductId, productIds));
        }

        // 删除商品
        productMapper.delete(new LambdaQueryWrapper<Product>()
                .eq(Product::getBoothId, boothId));
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

        operationLogService.saveLog(adminId, "审批拒绝", "拒绝了摊位申请 ID:" + applyId);
    }

    @Override
    @Transactional
    public void applyForBooth(Long userId, Long boothId) {
        Booth booth = boothMapper.selectById(boothId);
        if (booth == null || !"空闲".equals(booth.getStatus())) {
            throw new RuntimeException("该摊位不可申请");
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getVendorId, userId)
                .eq(BoothApply::getTargetBoothId, boothId)
                .eq(BoothApply::getStatus, "待审批")
                .eq(BoothApply::getType, "入住"));
        if (count > 0) {
            throw new RuntimeException("您已提交过该摊位的入住申请，请等待审批");
        }
        BoothApply apply = new BoothApply();
        apply.setType("入住");
        apply.setVendorId(userId);
        apply.setTargetBoothId(boothId);
        apply.setStatus("待审批");
        apply.setApplyTime(LocalDateTime.now());
        baseMapper.insert(apply);
    }

    @Override
    @Transactional
    public void applyChangeBooth(Long userId, Long targetBoothId) {
        if (hasPendingReservationsOrOrders(userId)) {
            throw new RuntimeException("您还有未完成的预定或订单，请先处理完毕再更换摊位");
        }
        Booth currentBooth = boothMapper.selectOne(new LambdaQueryWrapper<Booth>()
                .eq(Booth::getVendorId, userId)
                .eq(Booth::getStatus, "已占用"));
        if (currentBooth == null) {
            throw new RuntimeException("您当前没有占用的摊位");
        }
        Booth targetBooth = boothMapper.selectById(targetBoothId);
        if (targetBooth == null || !"空闲".equals(targetBooth.getStatus())) {
            throw new RuntimeException("目标摊位不可用");
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getVendorId, userId)
                .eq(BoothApply::getType, "更换")
                .eq(BoothApply::getStatus, "待审批"));
        if (count > 0) {
            throw new RuntimeException("您已有待审批的更换申请");
        }
        BoothApply apply = new BoothApply();
        apply.setType("更换");
        apply.setVendorId(userId);
        apply.setOriginBoothId(currentBooth.getId());
        apply.setTargetBoothId(targetBoothId);
        apply.setStatus("待审批");
        apply.setApplyTime(LocalDateTime.now());
        baseMapper.insert(apply);
    }

    @Override
    @Transactional
    public void applyReturnBooth(Long userId) {
        if (hasPendingReservationsOrOrders(userId)) {
            throw new RuntimeException("您还有未完成的预定或订单，请先处理完毕再归还摊位");
        }
        Booth currentBooth = boothMapper.selectOne(new LambdaQueryWrapper<Booth>()
                .eq(Booth::getVendorId, userId)
                .eq(Booth::getStatus, "已占用"));
        if (currentBooth == null) {
            throw new RuntimeException("您当前没有占用的摊位");
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getVendorId, userId)
                .eq(BoothApply::getType, "归还")
                .eq(BoothApply::getStatus, "待审批"));
        if (count > 0) {
            throw new RuntimeException("您已有待审批的归还申请");
        }
        BoothApply apply = new BoothApply();
        apply.setType("归还");
        apply.setVendorId(userId);
        apply.setOriginBoothId(currentBooth.getId());
        apply.setStatus("待审批");
        apply.setApplyTime(LocalDateTime.now());
        baseMapper.insert(apply);
    }

    @Override
    public boolean hasPendingReservationsOrOrders(Long userId) {
        Long pendingReservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getVendorId, userId)
                        .in(Reservation::getStatus, "待确认", "已确认")
        );
        if (pendingReservationCount > 0) return true;

        Long pendingOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getVendorId, userId)
                        .in(Order::getStatus, "待付款", "已付款")
        );
        return pendingOrderCount > 0;
    }
}