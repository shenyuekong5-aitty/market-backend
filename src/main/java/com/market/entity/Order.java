package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_master")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerUsername;
    private Long vendorId;
    private String vendorUsername;
    private Long marketId;
    private Long boothId;
    private BigDecimal totalAmount;
    private LocalDateTime payTime;
    private String status;  // 待付款/已付款/已完成/已取消
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}