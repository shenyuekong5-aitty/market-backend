package com.market.service;

import com.market.entity.User;
import com.market.mapper.UserMapper;
import com.market.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDefaultAvatarTest {

    private final UserMapper mapper = mock(UserMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final UserServiceImpl service = new UserServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("sms:register:13800138000")).thenReturn("123456");
        when(mapper.selectCount(any())).thenReturn(0L);
        when(encoder.encode("password")).thenReturn("encoded");
    }

    @Test
    void registrationUsesDefaultAvatarWhenNoImageWasSelected() {
        User user = new User();
        user.setUsername("new-user");
        user.setPhone("13800138000");
        user.setPassword("password");
        user.setRole("user");

        service.register(user, "123456");

        assertEquals("/images/default-avatar.svg", user.getAvatar());
        verify(mapper).insert(user);
        verify(redis).delete("sms:register:13800138000");
    }
}
