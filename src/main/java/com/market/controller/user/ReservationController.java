package com.market.controller.user;

import com.market.common.Result;
import com.market.dto.ReservationDTO;
import com.market.entity.User;
import com.market.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController("userReservationController")
@RequestMapping("/api/user/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public Result<String> submit(@RequestParam Long productId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = getCurrentUserId();
        reservationService.submitReservation(userId, productId, startTime, endTime);
        return Result.success("预定已提交");
    }

    @GetMapping
    public Result<List<ReservationDTO>> list() {
        Long userId = getCurrentUserId();
        return Result.success(reservationService.listUserReservations(userId));
    }

    @PutMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        reservationService.cancelReservation(userId, id);
        return Result.success("预定已取消");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}