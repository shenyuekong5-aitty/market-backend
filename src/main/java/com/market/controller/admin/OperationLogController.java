package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.OperationLog;
import com.market.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<List<OperationLog>> list(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        // 当前管理员ID暂时写死为1，后续改为动态获取
        Long adminId = 1L;
        List<OperationLog> logs = operationLogService.listByAdminAndTime(adminId, start, end);
        return Result.success(logs);
    }
}