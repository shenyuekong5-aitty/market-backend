package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.service.BoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/booths")
public class BoothController {

    @Autowired
    private BoothService boothService;

    // 获取指定集市下的摊位列表
    @GetMapping
    public Result<List<Booth>> list(@RequestParam Long marketId) {
        return Result.success(boothService.listByMarketId(marketId));
    }

    // 新增摊位
    @PostMapping
    public Result<Booth> create(@RequestBody Booth booth) {
        // 暂写死管理员ID为1
        return Result.success(boothService.createBooth(booth, 1L));
    }

    // 更新摊位
    @PutMapping("/{id}")
    public Result<Booth> update(@PathVariable Long id, @RequestBody Booth booth) {
        booth.setId(id);
        return Result.success(boothService.updateBooth(booth, 1L));
    }

    // 删除摊位
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boothService.deleteBooth(id, 1L);
        return Result.success("摊位已删除");
    }

    // 切换摊位状态（停用/启用）
    @PutMapping("/{id}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long id) {
        boothService.toggleBoothStatus(id, 1L);
        return Result.success("状态已更新");
    }
}