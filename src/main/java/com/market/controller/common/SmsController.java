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
}