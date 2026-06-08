package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.*;
import com.market.mapper.*;
import com.market.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.market.service.CartService;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BoothMapper boothMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public List<Order> createOrdersFromCart(Long userId) {
        // 1. 查询用户购物车
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));
        if (cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }
        // 2. 按摊位分组（通过商品 -> 摊位 -> 摊主）
        // 收集商品信息
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        // 收集摊位信息
        Set<Long> boothIds = products.stream().map(Product::getBoothId).collect(Collectors.toSet());
        List<Booth> booths = boothMapper.selectBatchIds(boothIds);
        // 收集用户信息（摊主）
        Set<Long> vendorIds = booths.stream().map(Booth::getVendorId).collect(Collectors.toSet());
        List<User> vendors = userMapper.selectBatchIds(vendorIds);

        // 构建商品Map
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        // 构建摊位Map
        Map<Long, Booth> boothMap = booths.stream().collect(Collectors.toMap(Booth::getId, b -> b));
        // 构建用户Map
        Map<Long, User> userMap = vendors.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 按摊主ID分组购物车项
        Map<Long, List<Cart>> vendorCartMap = new HashMap<>();
        for (Cart cart : cartItems) {
            Product p = productMap.get(cart.getProductId());
            if (p == null || !"上架".equals(p.getSaleStatus())) {
                throw new RuntimeException("商品 " + cart.getProductId() + " 已下架或不存在");
            }
            if (cart.getQuantity() > p.getStock()) {
                throw new RuntimeException("商品 " + p.getName() + " 库存不足");
            }
            Booth b = boothMap.get(p.getBoothId());
            if (b == null) throw new RuntimeException("摊位不存在");
            vendorCartMap.computeIfAbsent(b.getVendorId(), k -> new ArrayList<>()).add(cart);
        }

        // 查询当前用户信息
        User customer = userMapper.selectById(userId);
        String customerName = customer != null ? customer.getUsername() : "未知";

        List<Order> orders = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 按摊主生成订单
        for (Map.Entry<Long, List<Cart>> entry : vendorCartMap.entrySet()) {
            Long vendorId = entry.getKey();
            List<Cart> items = entry.getValue();
            User vendor = userMap.get(vendorId);
            String vendorName = vendor != null ? vendor.getUsername() : "未知";

            // 获取该摊主的第一个商品的摊位和集市信息
            Product firstProduct = productMap.get(items.get(0).getProductId());
            Booth booth = boothMap.get(firstProduct.getBoothId());

            // 生成订单编号
            String orderNo = "ORD" + fmt.format(LocalDateTime.now()) + String.format("%04d", new Random().nextInt(10000));

            // 计算总金额
            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            for (Cart cart : items) {
                Product p = productMap.get(cart.getProductId());
                total = total.add(p.getPrice().multiply(new BigDecimal(cart.getQuantity())));
                OrderItem item = new OrderItem();
                item.setProductId(p.getId());
                item.setProductName(p.getName());
                item.setProductPrice(p.getPrice());
                item.setQuantity(cart.getQuantity());
                item.setCreateTime(LocalDateTime.now());
                orderItems.add(item);
            }

            // 创建订单主记录
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setCustomerId(userId);
            order.setCustomerUsername(customerName);
            order.setVendorId(vendorId);
            order.setVendorUsername(vendorName);
            order.setMarketId(booth != null ? booth.getMarketId() : null);
            order.setBoothId(booth != null ? booth.getId() : null);
            order.setTotalAmount(total);
            order.setStatus("待付款");
            baseMapper.insert(order);

            // 插入订单明细并扣减库存
            for (OrderItem item : orderItems) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
                // 扣减库存
                Product p = productMap.get(item.getProductId());
                p.setStock(p.getStock() - item.getQuantity());
                productMapper.updateById(p);
            }
            orders.add(order);
        }

        // 清空购物车
        cartService.clearCart(userId);
        return orders;
    }

    @Override
    public List<Order> listUserOrders(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null || !order.getCustomerId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        if (!"待付款".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }
        order.setStatus("已付款");
        order.setPayTime(LocalDateTime.now());
        baseMapper.updateById(order);
        // 付款成功后可以改为“已完成”或者等确认收货，这里暂直接设为已完成
        // order.setStatus("已完成"); // 根据需求可启用
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                item.setProductImageUrl(product.getImageUrl());
            }
        }
        return items;
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null || !order.getCustomerId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        if (!"待付款".equals(order.getStatus()) && !"已付款".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }
        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productMapper.updateById(p);
            }
        }
        order.setStatus("已取消");
        baseMapper.updateById(order);
    }
}