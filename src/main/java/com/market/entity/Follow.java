package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("follow")
public class Follow {
    private Long customerId;
    private Long vendorId;
    private String vendorUsername;
    private LocalDateTime createTime;
}