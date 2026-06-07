package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.service.BoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/booth")
public class VendorBoothController {

    @Autowired
    private BoothService boothService;

    @GetMapping("/my")
    public Result<Booth> getMyBooth() {
        // 从 SecurityContext 获取当前小贩ID，暂写死
        Long vendorId = 2L;
        Booth booth = boothService.getByVendorId(vendorId);
        return Result.success(booth);
    }

    @PutMapping("/my")
    public Result<Booth> updateMyBooth(@RequestBody Booth booth) {
        Long vendorId = 2L;
        Booth updated = boothService.updateByVendor(vendorId, booth);
        return Result.success(updated);
    }
}