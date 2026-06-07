package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("booth_apply")
public class BoothApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;         // 入住/更换/归还
    private Long vendorId;
    private Long originBoothId;
    private Long targetBoothId;
    private String status;       // 待审批/通过/拒绝
    private Long approveAdminId;
    private LocalDateTime applyTime;
    private LocalDateTime processTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getOriginBoothId() {
        return originBoothId;
    }

    public void setOriginBoothId(Long originBoothId) {
        this.originBoothId = originBoothId;
    }

    public Long getTargetBoothId() {
        return targetBoothId;
    }

    public void setTargetBoothId(Long targetBoothId) {
        this.targetBoothId = targetBoothId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApproveAdminId() {
        return approveAdminId;
    }

    public void setApproveAdminId(Long approveAdminId) {
        this.approveAdminId = approveAdminId;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
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