package com.market.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoothApplyDTO {
    private Long id;
    private String type;
    private Long vendorId;
    private String vendorName;        // 申请人姓名
    private Long targetBoothId;
    private String targetBoothTitle;  // 目标摊位名称
    private String status;
    private LocalDateTime applyTime;
}