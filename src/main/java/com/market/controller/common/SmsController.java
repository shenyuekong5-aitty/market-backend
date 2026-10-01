package com.market.controller.common;

import com.market.common.Result;
import com.market.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private static final Logger log = LoggerFactory.getLogger(SmsController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsService smsService;   // 注入短信发送服务

    /**
     * 注册/登录时发送验证码
     */
    @PostMapping("/send")
    public Result<String> sendCode(@RequestParam String phone) {
        return sendAndStoreCode(phone, "sms:register:");
    }

    /**
     * 修改手机号时发送验证码
     */
    @PostMapping("/send-change-phone")
    public Result<String> sendChangePhoneCode(@RequestParam String phone) {
        return sendAndStoreCode(phone, "sms:change-phone:");
    }

    /**
     * 忘记密码时发送验证码
     */
    @PostMapping("/send-reset")
    public Result<String> sendResetCode(@RequestParam String phone) {
        return sendAndStoreCode(phone, "sms:reset-password:");
    }

    private Result<String> sendAndStoreCode(String phone, String redisPrefix) {
        try {
            String code = smsService.sendVerifyCode(phone);
            redisTemplate.opsForValue().set(redisPrefix + phone, code, 5, TimeUnit.MINUTES);
            return Result.success("验证码已发送");
        } catch (Exception e) {
            log.warn("短信验证码发送或保存失败：type={}, error={}", redisPrefix, e.getClass().getSimpleName());
            return Result.error("短信发送失败，请稍后重试");
        }
    }
}
