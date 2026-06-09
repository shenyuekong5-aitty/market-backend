package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Booth;
import com.market.entity.Follow;
import com.market.entity.User;
import com.market.mapper.BoothMapper;
import com.market.mapper.FollowMapper;
import com.market.mapper.UserMapper;
import com.market.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BoothMapper boothMapper;

    @Override
    @Transactional
    public void follow(Long userId, Long vendorId) {
        // 检查是否已关注
        Follow exist = baseMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getCustomerId, userId)
                .eq(Follow::getVendorId, vendorId));
        if (exist != null) return;
        // 获取摊主用户名
        User vendor = userMapper.selectById(vendorId);
        String vendorName = vendor != null ? (vendor.getNickname() != null ? vendor.getNickname() : vendor.getUsername()) : "未知";
        Follow follow = new Follow();
        follow.setCustomerId(userId);
        follow.setVendorId(vendorId);
        follow.setVendorUsername(vendorName);
        follow.setCreateTime(LocalDateTime.now());
        baseMapper.insert(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long userId, Long vendorId) {
        baseMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getCustomerId, userId)
                .eq(Follow::getVendorId, vendorId));
    }

    @Override
    public List<Map<String, Object>> listMyFollows(Long userId) {
        List<Follow> follows = baseMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getCustomerId, userId)
                .orderByDesc(Follow::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Follow f : follows) {
            Map<String, Object> map = new HashMap<>();
            User vendor = userMapper.selectById(f.getVendorId());
            String displayName = vendor != null ? (vendor.getNickname() != null ? vendor.getNickname() : vendor.getUsername()) : f.getVendorUsername();
            String avatar = (vendor != null && vendor.getAvatar() != null) ? vendor.getAvatar() : "";
            map.put("vendorId", f.getVendorId());
            map.put("vendorName", displayName);
            map.put("avatar", avatar);
            map.put("createTime", f.getCreateTime());
            // 查询该摊主的第一个已占用摊位
            Booth booth = boothMapper.selectOne(new LambdaQueryWrapper<Booth>()
                    .eq(Booth::getVendorId, f.getVendorId())
                    .eq(Booth::getStatus, "已占用")
                    .last("LIMIT 1"));
            if (booth != null) {
                map.put("boothId", booth.getId());
                map.put("boothTitle", booth.getTitle());
                map.put("boothPosition", booth.getPosition());
            } else {
                map.put("boothId", null);
                map.put("boothTitle", "");
                map.put("boothPosition", "");
            }
            result.add(map);
        }
        return result;
    }
    @Override
    public boolean isFollowed(Long userId, Long vendorId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getCustomerId, userId)
                .eq(Follow::getVendorId, vendorId)) > 0;
    }
}