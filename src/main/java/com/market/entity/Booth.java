package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("booth")
public class Booth {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long marketId;
    private String position;
    private String status;   // 空闲/已占用/停用
    private Long vendorId;
    private String title;
    private String description;
    private String openTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //用来判断用户是否对某个摊位的申请情况的临时字段
    @TableField(exist = false)
    private Boolean hasPendingApply;

    public Boolean getHasPendingApply() { return hasPendingApply; }
    public void setHasPendingApply(Boolean hasPendingApply) { this.hasPendingApply = hasPendingApply; }
    //用来判断用户是否对某个摊位的申请情况的临时字段


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}