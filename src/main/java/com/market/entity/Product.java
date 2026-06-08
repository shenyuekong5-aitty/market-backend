package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long boothId;         // 所属摊位ID
    private Long vendorId;        // 所属摊主ID
    private String name;
    private BigDecimal price;
    private Integer canReserve;   // 是否支持预定 0否 1是
    private String saleStatus;    // 上架/下架
    private Integer stock;        // 库存数量
    private String imageUrl;      // 商品图片URL
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}