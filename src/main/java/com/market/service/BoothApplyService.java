package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.dto.BoothApplyDTO;
import com.market.entity.BoothApply;
import java.util.List;

public interface BoothApplyService extends IService<BoothApply> {
    List<BoothApply> listPending();
    void approve(Long applyId, Long adminId);
    void reject(Long applyId, Long adminId);

    //申请摊位
    List<BoothApplyDTO> listPendingWithDetails();
    //申请空闲摊位
    void applyForBooth(Long userId, Long boothId);

    //申请改变摊位
    void applyChangeBooth(Long userId, Long targetBoothId);
    //申请归还摊位
    void applyReturnBooth(Long userId);

    //判断是否有未处理的订单跟预定，服务于更换摊位/归还摊位
    boolean hasPendingReservationsOrOrders(Long userId);

    // 根据管理员ID查询待审批申请（只查自己集市下的）
    List<BoothApply> listPendingByAdmin(Long adminId);
    List<BoothApplyDTO> listPendingWithDetailsByAdmin(Long adminId);
}