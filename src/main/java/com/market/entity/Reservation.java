package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long vendorId;
    private String status;       // 待确认/已确认/已拒绝/已取消
    private Long orderId;        // 确认后关联的订单ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}