package com.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;       // 接收用户ID
    private String type;           // 通知类型：预定请求、预定结果、申请结果、系统通知等
    private String content;        // 通知内容
    private Long businessId;       // 关联业务ID（如预定ID、申请ID）
    private Integer isRead;        // 是否已读 0未读 1已读
    private LocalDateTime createTime;
}