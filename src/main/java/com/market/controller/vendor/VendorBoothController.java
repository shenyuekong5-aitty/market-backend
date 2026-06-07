package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.entity.User;
import com.market.service.BoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/booth")
public class VendorBoothController {

    @Autowired
    private BoothService boothService;

    @GetMapping("/my")
    public Result<Booth> getMyBooth() {
        Long vendorId = getCurrentUserId();
        Booth booth = boothService.getByVendorId(vendorId);
        return Result.success(booth);
    }

    @PutMapping("/my")
    public Result<Booth> updateMyBooth(@RequestBody Booth booth) {
        Long vendorId = getCurrentUserId();
        Booth updated = boothService.updateByVendor(vendorId, booth);
        return Result.success(updated);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}