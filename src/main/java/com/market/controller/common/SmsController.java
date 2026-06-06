package com.market.controller.common;

import com.market.common.Result;
import com.market.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsService smsService;   // 注入短信发送服务

    /**
     * 注册/登录时发送验证码
     */
    @PostMapping("/send")
    public Result<String> sendCode(@RequestParam String phone) {
        try {
            String code = smsService.sendVerifyCode(phone);
            redisTemplate.opsForValue().set("sms:register:" + phone, code, 5, TimeUnit.MINUTES);
            System.out.println("验证码：" + code);
            return Result.success("验证码已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("短信发送失败：" + e.getMessage());
        }
    }

    /**
     * 修改手机号时发送验证码
     */
    @PostMapping("/send-change-phone")
    public Result<String> sendChangePhoneCode(@RequestParam String phone) {
        try {
            String code = smsService.sendVerifyCode(phone);
            redisTemplate.opsForValue().set("sms:change-phone:" + phone, code, 5, TimeUnit.MINUTES);
            System.out.println("换绑手机验证码：" + code);
            return Result.success("验证码已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("短信发送失败：" + e.getMessage());
        }
    }

    /**
     * 忘记密码时发送验证码
     */
    @PostMapping("/send-reset")
    public Result<String> sendResetCode(@RequestParam String phone) {
        try {
            String code = smsService.sendVerifyCode(phone);
            redisTemplate.opsForValue().set("sms:reset-password:" + phone, code, 5, TimeUnit.MINUTES);
            System.out.println("重置密码验证码：" + code);
            return Result.success("验证码已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("短信发送失败：" + e.getMessage());
        }
    }
}