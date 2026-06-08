package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.dto.ReservationDTO;
import com.market.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService extends IService<Reservation> {
    void submitReservation(Long userId, Long productId, LocalDateTime startTime, LocalDateTime endTime);
    List<ReservationDTO> listUserReservations(Long userId);
    List<ReservationDTO> listVendorReservations(Long vendorId);
    void confirmReservation(Long vendorId, Long reservationId);
    void rejectReservation(Long vendorId, Long reservationId);
    void cancelReservation(Long userId, Long reservationId);
}