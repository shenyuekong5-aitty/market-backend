package com.market.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SecurityCheckResult {
    private int score;                              // 总评分（0-100）
    private List<Map<String, Object>> items;        // 各检测项明细
    private String message;                         // 整体评语
}