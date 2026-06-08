package com.market.controller.vendor;

import com.market.common.Result;
import com.market.dto.ReservationDTO;
import com.market.entity.User;
import com.market.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("vendorReservationController")
@RequestMapping("/api/vendor/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    public Result<List<ReservationDTO>> list() {
        Long vendorId = getCurrentUserId();
        return Result.success(reservationService.listVendorReservations(vendorId));
    }

    @PutMapping("/{id}/confirm")
    public Result<String> confirm(@PathVariable Long id) {
        Long vendorId = getCurrentUserId();
        reservationService.confirmReservation(vendorId, id);
        return Result.success("预定已确认，订单已生成");
    }

    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id) {
        Long vendorId = getCurrentUserId();
        reservationService.rejectReservation(vendorId, id);
        return Result.success("预定已拒绝");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}