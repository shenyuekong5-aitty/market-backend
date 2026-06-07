package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Booth;

import java.util.List;

public interface BoothService extends IService<Booth> {
    //摊位相关
    List<Booth> listByMarketId(Long marketId);
    Booth createBooth(Booth booth, Long adminId);
    Booth updateBooth(Booth booth, Long adminId);
    void deleteBooth(Long boothId, Long adminId);
    void toggleBoothStatus(Long boothId, Long adminId);

    // 小贩端方法
    Booth getByVendorId(Long vendorId);
    Booth updateByVendor(Long vendorId, Booth boothInfo);
    //集市中空闲的摊位
    List<Booth> listFreeBoothsByMarketId(Long marketId,Long userId);
}