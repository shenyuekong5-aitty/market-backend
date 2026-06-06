package com.market.controller.common;

import com.market.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/send")
    public Result<String> sendCode(@RequestParam String phone) {
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("sms:register:" + phone, code, 5, TimeUnit.MINUTES);
        System.out.println("验证码：" + code);
        return Result.success("验证码已发送");
    }

    @PostMapping("/send-change-phone")
    public Result<String> sendChangePhoneCode(@RequestParam String phone) {
        // 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        // 存入 Redis，有效期5分钟，key 前缀为 sms:change-phone:
        redisTemplate.opsForValue().set("sms:change-phone:" + phone, code, 5, TimeUnit.MINUTES);
        System.out.println("更换手机号验证码：" + code);   // 开发时打印，上线后替换为短信服务
        return Result.success("验证码已发送");
    }
}