package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private LocalDateTime reserveStartTime;
    private LocalDateTime reserveEndTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //非持久化字段--用于商品图片
    @TableField(exist = false)
    private String productImageUrl;

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
}