package com.market.service;

import com.market.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    void register(User user, String code);
    String login(String username, String password, String role);
    User getCurrentUser();
}