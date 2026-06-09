package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.entity.User;
import com.market.service.BoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/booths")
public class BoothController {

    @Autowired
    private BoothService boothService;

    // 获取指定集市下的摊位列表（只允许查看自己管理的集市）
    @GetMapping
    public Result<List<Booth>> list(@RequestParam Long marketId) {
        Long adminId = getCurrentAdminId();
        return Result.success(boothService.listByMarketId(marketId, adminId));  // 修改为带权限校验的方法
    }

    // 新增摊位
    @PostMapping
    public Result<Booth> create(@RequestBody Booth booth) {
        Long adminId = getCurrentAdminId();
        return Result.success(boothService.createBooth(booth, adminId));
    }

    // 更新摊位
    @PutMapping("/{id}")
    public Result<Booth> update(@PathVariable Long id, @RequestBody Booth booth) {
        booth.setId(id);
        Long adminId = getCurrentAdminId();
        return Result.success(boothService.updateBooth(booth, adminId));
    }

    // 删除摊位
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        boothService.deleteBooth(id, adminId);
        return Result.success("摊位已删除");
    }

    // 切换摊位状态
    @PutMapping("/{id}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        boothService.toggleBoothStatus(id, adminId);
        return Result.success("状态已更新");
    }

    private Long getCurrentAdminId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}