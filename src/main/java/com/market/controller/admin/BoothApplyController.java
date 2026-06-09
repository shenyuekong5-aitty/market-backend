package com.market.controller.admin;

import com.market.common.Result;
import com.market.dto.BoothApplyDTO;
import com.market.entity.User;
import com.market.service.BoothApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applies")
public class BoothApplyController {

    @Autowired
    private BoothApplyService boothApplyService;

    // 获取待审批申请（只显示当前管理员管理集市的申请）
    @GetMapping("/pending")
    public Result<List<BoothApplyDTO>> getPendingApplies() {
        Long adminId = getCurrentAdminId();
        return Result.success(boothApplyService.listPendingWithDetailsByAdmin(adminId));
    }

    // 审批通过
    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        boothApplyService.approve(id, adminId);
        return Result.success("审批通过");
    }

    // 审批拒绝
    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        boothApplyService.reject(id, adminId);
        return Result.success("已拒绝");
    }

    private Long getCurrentAdminId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}