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

    // 重置密码（忘记密码功能）
    void resetPassword(String phone, String code, String newPassword);

    // 检查手机号是否已注册
    boolean isPhoneRegistered(String phone);

    /**
     * 注销账号（逻辑删除）
     * @param userId 当前用户ID
     */
    void deactivateAccount(Long userId);

    /**
     * 检查用户是否允许注销（无未完成的业务）
     * @param userId 用户ID
     * @return true-可以注销，false-不可注销
     */
    boolean canDeactivate(Long userId);
}