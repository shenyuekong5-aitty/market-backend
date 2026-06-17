package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.BoothApplyDTO;
import com.market.entity.*;
import com.market.mapper.*;
import com.market.service.BoothApplyService;
import com.market.service.NotificationService;
import com.market.service.OperationLogService;
import com.market.websocket.NotificationEndpoint;
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
    @Autowired
    private MarketMapper marketMapper;

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

        // 校验管理员是否有权限审批该申请
        validateAdminPermission(apply, adminId);

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
            // WebSocket 通知申请人
            NotificationEndpoint.sendToUser(apply.getVendorId().toString(), "new_notification");
        } else if ("更换".equals(type)) {
            handleApproveChange(apply);
            // 发送通知：更换成功
            notificationService.createNotification(
                    apply.getVendorId(),
                    "申请结果",
                    "您的摊位更换申请已通过审批，新摊位已分配。",
                    apply.getId()
            );
            NotificationEndpoint.sendToUser(apply.getVendorId().toString(), "new_notification");
        } else if ("归还".equals(type)) {
            handleApproveReturn(apply);
            // 发送通知：归还成功，角色可能变更
            notificationService.createNotification(
                    apply.getVendorId(),
                    "申请结果",
                    "您的摊位归还申请已通过审批。",
                    apply.getId()
            );
            NotificationEndpoint.sendToUser(apply.getVendorId().toString(), "new_notification");
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

        // 校验管理员是否有权限审批该申请
        validateAdminPermission(apply, adminId);

        apply.setStatus("拒绝");
        apply.setApproveAdminId(adminId);
        apply.setProcessTime(LocalDateTime.now());
        baseMapper.updateById(apply);

        // 发送拒绝通知
        notificationService.createNotification(
                apply.getVendorId(),
                "申请结果",
                "您的摊位申请已被拒绝（申请ID：" + applyId + "）",
                apply.getId()
        );
        // WebSocket 通知申请人
        NotificationEndpoint.sendToUser(apply.getVendorId().toString(), "new_notification");

        operationLogService.saveLog(adminId, "审批拒绝", "拒绝了摊位申请 ID:" + applyId);
    }

    @Override
    @Transactional
    public void applyForBooth(Long userId, Long boothId) {
        System.out.println("[applyForBooth] 收到入住申请: userId=" + userId + ", boothId=" + boothId);
        Booth booth = boothMapper.selectById(boothId);
        if (booth == null || !"空闲".equals(booth.getStatus())) {
            System.out.println("[applyForBooth] 摊位不可申请: booth=" + (booth != null ? booth.getStatus() : "null"));
            throw new RuntimeException("该摊位不可申请");
        }
        System.out.println("[applyForBooth] 摊位信息: boothId=" + booth.getId() + ", marketId=" + booth.getMarketId() + ", title=" + booth.getTitle());
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
        System.out.println("[applyForBooth] 申请已创建: applyId=" + apply.getId() + ", type=入住, vendorId=" + userId + ", targetBoothId=" + boothId);

        // 通知摊位所属集市的管理员
        notifyAdmin(booth, userId, "入住", apply.getId());
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

        // 通知目标摊位所属集市的管理员
        notifyAdmin(targetBooth, userId, "更换", apply.getId());
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

        // 通知当前摊位所属集市的管理员
        notifyAdmin(currentBooth, userId, "归还", apply.getId());
    }

    @Override
    public boolean hasPendingReservationsOrOrders(Long userId) {
        // 只检查"待确认"的预定，因为"已确认"的预定已有关联订单，由订单状态检查覆盖
        Long pendingReservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getVendorId, userId)
                        .eq(Reservation::getStatus, "待确认")
        );
        if (pendingReservationCount > 0) return true;

        Long pendingOrderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getVendorId, userId)
                        .in(Order::getStatus, "待付款", "已付款")
        );
        return pendingOrderCount > 0;
    }

    @Override
    public List<BoothApply> listPendingByAdmin(Long adminId) {
        System.out.println("[listPendingByAdmin] 查询待审批申请: adminId=" + adminId);

        // 超级管理员：返回所有待审批申请，不受集市限制
        User admin = userMapper.selectById(adminId);
        if (admin != null && admin.getIsSuperAdmin() != null && admin.getIsSuperAdmin() == 1) {
            System.out.println("[listPendingByAdmin] 超级管理员，返回所有待审批申请");
            List<BoothApply> result = baseMapper.selectList(new LambdaQueryWrapper<BoothApply>()
                    .eq(BoothApply::getStatus, "待审批"));
            System.out.println("[listPendingByAdmin] 超级管理员查询结果数量=" + result.size());
            return result;
        }

        // 普通管理员：只返回自己集市下的待审批申请
        Market market = marketMapper.selectOne(new LambdaQueryWrapper<Market>()
                .eq(Market::getAdminId, adminId));
        if (market == null) {
            System.out.println("[listPendingByAdmin] 未找到该管理员管理的集市, adminId=" + adminId);
            return new ArrayList<>();
        }
        System.out.println("[listPendingByAdmin] 找到集市: marketId=" + market.getId() + ", name=" + market.getName());

        List<Booth> booths = boothMapper.selectList(new LambdaQueryWrapper<Booth>()
                .eq(Booth::getMarketId, market.getId()));
        if (booths.isEmpty()) {
            System.out.println("[listPendingByAdmin] 该集市下没有摊位, marketId=" + market.getId());
            return new ArrayList<>();
        }

        List<Long> boothIds = booths.stream().map(Booth::getId).collect(Collectors.toList());
        System.out.println("[listPendingByAdmin] 该集市下摊位数量=" + booths.size() + ", boothIds=" + boothIds);

        List<BoothApply> result = baseMapper.selectList(new LambdaQueryWrapper<BoothApply>()
                .eq(BoothApply::getStatus, "待审批")
                .and(w -> w.in(BoothApply::getTargetBoothId, boothIds)
                        .or()
                        .in(BoothApply::getOriginBoothId, boothIds)));
        System.out.println("[listPendingByAdmin] 查询结果数量=" + result.size());
        for (BoothApply a : result) {
            System.out.println("[listPendingByAdmin] 申请: id=" + a.getId() + ", type=" + a.getType() + ", vendorId=" + a.getVendorId() + ", targetBoothId=" + a.getTargetBoothId() + ", originBoothId=" + a.getOriginBoothId() + ", status=" + a.getStatus());
        }
        return result;
    }

    @Override
    public List<BoothApplyDTO> listPendingWithDetailsByAdmin(Long adminId) {
        List<BoothApply> applies = listPendingByAdmin(adminId);
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

    private void validateAdminPermission(BoothApply apply, Long adminId) {
        // 超级管理员：跳过集市归属校验，可审批所有申请
        User admin = userMapper.selectById(adminId);
        if (admin != null && admin.getIsSuperAdmin() != null && admin.getIsSuperAdmin() == 1) {
            System.out.println("[validateAdminPermission] 超级管理员审批，跳过集市归属校验");
            return;
        }

        Long boothId = apply.getTargetBoothId() != null ? apply.getTargetBoothId() : apply.getOriginBoothId();
        if (boothId == null) {
            throw new RuntimeException("申请信息不完整");
        }
        Booth booth = boothMapper.selectById(boothId);
        if (booth == null) {
            throw new RuntimeException("关联摊位不存在");
        }
        Market market = marketMapper.selectById(booth.getMarketId());
        if (market == null || !market.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权审批该申请，该摊位不属于您管理的集市");
        }
    }

    /**
     * 通知摊位所属集市的管理员有新申请
     */
    private void notifyAdmin(Booth booth, Long userId, String applyType, Long applyId) {
        Market market = marketMapper.selectById(booth.getMarketId());
        if (market == null || market.getAdminId() == null) {
            return;
        }
        User applicant = userMapper.selectById(userId);
        String applicantName = applicant != null ? applicant.getNickname() : "未知用户";
        String typeName;
        switch (applyType) {
            case "入住": typeName = "入住"; break;
            case "更换": typeName = "更换"; break;
            case "归还": typeName = "归还"; break;
            default: typeName = applyType;
        }
        notificationService.createNotification(
                market.getAdminId(),
                typeName + "申请",
                "用户「" + applicantName + "」提交了摊位「" + booth.getTitle() + "」的" + typeName + "申请",
                applyId
        );
        // WebSocket 通知该管理员
        NotificationEndpoint.sendToUser(market.getAdminId().toString(), "new_notification");
    }
}