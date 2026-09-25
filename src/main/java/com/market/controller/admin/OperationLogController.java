package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.OperationLog;
import com.market.entity.User;
import com.market.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User)) {
            throw new RuntimeException("用户未登录");
        }
        Long adminId = ((User) principal).getId();
        List<OperationLog> logs = operationLogService.listByAdminAndTime(adminId, start, end);
        return Result.success(logs);
    }
}
