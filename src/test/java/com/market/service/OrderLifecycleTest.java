package com.market.service;

import com.market.entity.Order;
import com.market.mapper.OrderMapper;
import com.market.mapper.ReservationMapper;
import com.market.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleTest {

    @Mock private OrderMapper orderMapper;
    @Mock private ReservationMapper reservationMapper;
    @Mock private NotificationService notificationService;
    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void injectMyBatisBaseMapper() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    @Test
    void paidOrderCanBeMarkedReadyByItsVendor() {
        Order order = order(11L, 20L, 30L, "已付款");
        when(orderMapper.selectById(11L)).thenReturn(order);
        when(orderMapper.markReadyIfPaid(11L)).thenReturn(1);

        orderService.markReady(20L, 11L);

        verify(orderMapper).markReadyIfPaid(11L);
        verify(notificationService).createNotification(eq(30L), eq("订单状态"), any(), eq(11L));
    }

    @Test
    void customerCanCompleteOnlyReadyOrder() {
        Order order = order(12L, 20L, 30L, "待取货");
        when(orderMapper.selectById(12L)).thenReturn(order);
        when(orderMapper.completeIfReady(eq(12L), any(LocalDateTime.class))).thenReturn(1);

        orderService.confirmReceive(30L, 12L);

        verify(orderMapper).completeIfReady(eq(12L), any(LocalDateTime.class));
        verify(notificationService).createNotification(eq(20L), eq("订单状态"), any(), eq(12L));
    }

    @Test
    void paidOrderCannotBeCompletedBeforeVendorMarksItReady() {
        Order order = order(13L, 20L, 30L, "已付款");
        when(orderMapper.selectById(13L)).thenReturn(order);

        assertThrows(RuntimeException.class, () -> orderService.confirmReceive(30L, 13L));

        verify(orderMapper, never()).completeIfReady(eq(13L), any(LocalDateTime.class));
    }

    private Order order(Long id, Long vendorId, Long customerId, String status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("TEST-" + id);
        order.setVendorId(vendorId);
        order.setCustomerId(customerId);
        order.setStatus(status);
        return order;
    }
}
