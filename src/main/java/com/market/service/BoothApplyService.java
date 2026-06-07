package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.BoothApply;

import java.util.List;

public interface BoothApplyService extends IService<BoothApply> {
    List<BoothApply> listPending();
}