package com.market.controller.common;

import com.market.common.Result;
import com.market.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class SmsControllerTest {
    private SmsController controller;
    private SmsService smsService;
    private ValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        controller = new SmsController();
        smsService = mock(SmsService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ReflectionTestUtils.setField(controller, "smsService", smsService);
        ReflectionTestUtils.setField(controller, "redisTemplate", redis);
    }

    @Test
    void storesEachCodeUnderItsOwnPurposeWithoutReturningTheCode() throws Exception {
        when(smsService.sendVerifyCode("13800138000")).thenReturn("123456");

        assertSuccessfulWithoutCode(controller.sendCode("13800138000"));
        assertSuccessfulWithoutCode(controller.sendChangePhoneCode("13800138000"));
        assertSuccessfulWithoutCode(controller.sendResetCode("13800138000"));

        verify(values).set("sms:register:13800138000", "123456", 5, TimeUnit.MINUTES);
        verify(values).set("sms:change-phone:13800138000", "123456", 5, TimeUnit.MINUTES);
        verify(values).set("sms:reset-password:13800138000", "123456", 5, TimeUnit.MINUTES);
    }

    private void assertSuccessfulWithoutCode(Result<String> result) {
        assertEquals(200, result.getCode());
        assertFalse(result.getData().contains("123456"));
    }
}
