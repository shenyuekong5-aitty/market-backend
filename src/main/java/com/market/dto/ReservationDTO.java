package com.market.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReservationDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long userId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long vendorId;
    private String status;
    private Long orderId;
    private LocalDateTime createTime;
}