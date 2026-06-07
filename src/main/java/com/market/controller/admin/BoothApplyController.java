package com.market.controller.admin;

import com.market.common.Result;
import com.market.entity.BoothApply;
import com.market.service.BoothApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applies")
public class BoothApplyController {

    @Autowired
    private BoothApplyService boothApplyService;

    /**
     * 获取待审批的摊位申请
     */
    @GetMapping("/pending")
    public Result<List<BoothApply>> getPendingApplies() {
        List<BoothApply> list = boothApplyService.listPending();
        return Result.success(list);
    }
}