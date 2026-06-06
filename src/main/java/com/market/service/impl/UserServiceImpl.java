package com.market.service.impl;

// ===== 1. Java 标准库 =====
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ===== 2. 第三方库 (Spring、MyBatis-Plus 等) =====
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.UpdateProfileRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// ===== 3. 项目内部模块 =====
import com.market.common.JwtUtils;
import com.market.dto.SecurityCheckResult;
import com.market.entity.User;
import com.market.mapper.UserMapper;
import com.market.service.UserService;

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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new RuntimeException("用户未登录");
    }

    @Override
    public User getByUsername(String username) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public SecurityCheckResult performSecurityCheck(User user) {
        if (user == null) {
            throw new RuntimeException("用户不存在或 Token 无效");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        int totalScore = 100;

        // ========== 1. 密码安全检测（基于更新时间） ==========
        Map<String, Object> pwdItem = new HashMap<>();
        pwdItem.put("id", "pwd");
        pwdItem.put("label", "密码安全");

        if (user.getUpdateTime() != null) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(user.getUpdateTime(), LocalDateTime.now());
            if (daysSinceUpdate > 90) {
                pwdItem.put("result", "超过90天未更换密码");
                pwdItem.put("status", "warning");
                totalScore -= 15;
            } else {
                pwdItem.put("result", "密码近期已更新（" + daysSinceUpdate + "天前）");
                pwdItem.put("status", "success");
            }
        } else {
            // 没有更新时间记录，视为未修改过密码
            pwdItem.put("result", "从未修改密码（建议立即修改）");
            pwdItem.put("status", "warning");
            totalScore -= 20;
        }
        items.add(pwdItem);

        // ========== 2. 角色权限检测 ==========
        Map<String, Object> roleItem = new HashMap<>();
        roleItem.put("id", "role");
        roleItem.put("label", "当前角色");
        String role = user.getRole();
        if ("admin".equals(role)) {
            roleItem.put("result", "集市管理员（最高权限）");
        } else if ("vendor".equals(role)) {
            roleItem.put("result", "小贩（摊位经营权限）");
        } else {
            roleItem.put("result", "普通用户（浏览购买权限）");
        }
        roleItem.put("status", "success");
        items.add(roleItem);

        // ========== 3. 账号状态检测 ==========
        Map<String, Object> statusItem = new HashMap<>();
        statusItem.put("id", "status");
        statusItem.put("label", "账号状态");
        if (user.getStatus() == 1) {
            statusItem.put("result", "正常使用中");
            statusItem.put("status", "success");
        } else {
            statusItem.put("result", "已注销");
            statusItem.put("status", "warning");
            totalScore -= 30;
        }
        items.add(statusItem);

        // ========== 4. 资料完整度检测 ==========
        Map<String, Object> infoItem = new HashMap<>();
        infoItem.put("id", "info");
        infoItem.put("label", "资料完整度");
        boolean hasAvatar = user.getAvatar() != null && !user.getAvatar().isEmpty();
        boolean hasNickname = user.getNickname() != null && !user.getNickname().isEmpty();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();

        int filledCount = (hasAvatar ? 1 : 0) + (hasNickname ? 1 : 0) + (hasPhone ? 1 : 0);
        if (filledCount == 3) {
            infoItem.put("result", "已完善（头像、昵称、手机号）");
            infoItem.put("status", "success");
        } else if (filledCount >= 1) {
            infoItem.put("result", "部分完善（" + filledCount + "/3 项已填）");
            infoItem.put("status", "warning");
            totalScore -= 10;
        } else {
            infoItem.put("result", "未完善（建议补充个人资料）");
            infoItem.put("status", "warning");
            totalScore -= 20;
        }
        items.add(infoItem);

        // ========== 5. 登录活跃度检测（基于注册时间） ==========
        Map<String, Object> activeItem = new HashMap<>();
        activeItem.put("id", "active");
        activeItem.put("label", "账户活跃度");
        if (user.getCreateTime() != null) {
            long daysSinceCreate = ChronoUnit.DAYS.between(user.getCreateTime(), LocalDateTime.now());
            if (daysSinceCreate < 30) {
                activeItem.put("result", "新用户（注册不到30天）");
            } else {
                activeItem.put("result", "老用户（已注册" + daysSinceCreate + "天）");
            }
            activeItem.put("status", "success");
        } else {
            activeItem.put("result", "无法获取注册时间");
            activeItem.put("status", "warning");
            totalScore -= 5;
        }
        items.add(activeItem);

        // ========== 组装返回结果 ==========
        SecurityCheckResult result = new SecurityCheckResult();
        result.setScore(Math.max(totalScore, 0));
        result.setItems(items);
        result.setMessage(totalScore >= 90 ? "账号整体状态良好" : "存在安全隐患，建议优化");
        return result;
    }

    @Override
    public User updateProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentUser();

        // 1. 直接更新的基础字段
        currentUser.setNickname(request.getNickname());
        currentUser.setAvatar(request.getAvatar());
        currentUser.setGender(request.getGender());

        // 2. 处理手机号修改（需双重验证）
        if (request.getNewPhone() != null && !request.getNewPhone().isEmpty()) {
            // 验证当前密码
            if (request.getConfirmPassword() == null ||
                    !passwordEncoder.matches(request.getConfirmPassword(), currentUser.getPassword())) {
                throw new RuntimeException("密码错误，无法修改手机号");
            }

            // 验证短信验证码
            String codeKey = "sms:change-phone:" + request.getNewPhone();
            String savedCode = redisTemplate.opsForValue().get(codeKey);
            if (savedCode == null || !savedCode.equals(request.getPhoneCode())) {
                throw new RuntimeException("验证码错误或已过期");
            }

            // 检查新手机号是否已被占用
            if (baseMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getNewPhone())
                    .ne(User::getId, currentUser.getId())) > 0) {
                throw new RuntimeException("该手机号已被其他用户绑定");
            }

            currentUser.setPhone(request.getNewPhone());
            redisTemplate.delete(codeKey);  // 删除已使用的验证码
        }

        // 3. 更新修改时间
        currentUser.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(currentUser);
        return currentUser;
    }
}