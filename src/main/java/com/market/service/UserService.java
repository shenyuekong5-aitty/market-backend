package com.market.service;

import com.market.dto.SecurityCheckResult;
import com.market.dto.UpdateProfileRequest;
import com.market.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    void register(User user, String code);
    String login(String username, String password, String role);
    User getCurrentUser();

    User getByUsername(String username);

    SecurityCheckResult performSecurityCheck(User user);

    // 修改用户资料
    User updateProfile(UpdateProfileRequest request);

    // 修改密码
    void changePassword(String username, String oldPassword, String newPassword);
}