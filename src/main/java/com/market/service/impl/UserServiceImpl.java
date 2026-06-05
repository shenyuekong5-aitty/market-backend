package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.common.JwtUtils;
import com.market.entity.User;
import com.market.mapper.UserMapper;
import com.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void register(User user, String code) {
        // 1. 校验验证码
        String key = "sms:register:" + user.getPhone();
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null || !savedCode.equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 2. 检查账号是否已存在
        if (baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())) > 0) {
            throw new RuntimeException("账号已存在");
        }

        // 3. 加密密码并保存
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("user");   // 注册默认普通用户
        user.setStatus(1);
        baseMapper.insert(user);

        // 4. 删除验证码缓存
        redisTemplate.delete(key);
    }

    @Override
    public String login(String username, String password, String role) {
        // 1. 查询用户
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("账号或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }

        // 3. 校验角色
        if (!user.getRole().equals(role)) {
            throw new RuntimeException("角色不匹配");
        }

        // 4. 校验用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已注销");
        }

        // 5. 生成JWT
        return jwtUtils.generateToken(user.getUsername(), user.getRole());
    }

    @Override
    public User getCurrentUser() {
        // 暂不实现，等Spring Security集成JWT过滤器后从SecurityContext获取
        return null;
    }
}