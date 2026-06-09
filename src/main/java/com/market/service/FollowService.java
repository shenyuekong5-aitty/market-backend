package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Follow;
import java.util.List;
import java.util.Map;

public interface FollowService extends IService<Follow> {
    // 关注
    void follow(Long userId, Long vendorId);
    // 取消关注
    void unfollow(Long userId, Long vendorId);
    // 查看我的关注列表（返回DTO，含摊主昵称）
    List<Map<String, Object>> listMyFollows(Long userId);
    // 判断是否已关注
    boolean isFollowed(Long userId, Long vendorId);
}