package com.market.controller.user;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/follows")
public class FollowController {

    @Autowired
    private FollowService followService;

    // 关注摊主
    @PostMapping("/{vendorId}")
    public Result<String> follow(@PathVariable Long vendorId) {
        Long userId = getCurrentUserId();
        followService.follow(userId, vendorId);
        return Result.success("关注成功");
    }

    // 取消关注
    @DeleteMapping("/{vendorId}")
    public Result<String> unfollow(@PathVariable Long vendorId) {
        Long userId = getCurrentUserId();
        followService.unfollow(userId, vendorId);
        return Result.success("已取消关注");
    }

    // 我的关注列表
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long userId = getCurrentUserId();
        return Result.success(followService.listMyFollows(userId));
    }

    // 检查是否已关注
    @GetMapping("/check/{vendorId}")
    public Result<Boolean> check(@PathVariable Long vendorId) {
        Long userId = getCurrentUserId();
        return Result.success(followService.isFollowed(userId, vendorId));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}