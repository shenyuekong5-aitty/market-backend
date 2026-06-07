package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.BoothApply;
import com.market.service.BoothApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applies")
public class BoothApplyController {

    @Autowired
    private BoothApplyService boothApplyService;

    @GetMapping("/pending")
    public Result<List<BoothApply>> getPendingApplies() {
        return Result.success(boothApplyService.listPending());
    }

    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        boothApplyService.approve(id, 1L);
        return Result.success("审批通过");
    }

    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id) {
        boothApplyService.reject(id, 1L);
        return Result.success("已拒绝");
    }
}