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
}