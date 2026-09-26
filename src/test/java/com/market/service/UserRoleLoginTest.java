package com.market.service;

import com.market.common.JwtUtils;
import com.market.common.UserRole;
import com.market.entity.User;
import com.market.mapper.UserMapper;
import com.market.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserRoleLoginTest {
    private final UserMapper mapper = mock(UserMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtUtils jwt = mock(JwtUtils.class);
    private final UserServiceImpl service = new UserServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "jwtUtils", jwt);
        when(encoder.matches(eq("valid-password"), any())).thenReturn(true);
    }

    @Test
    void marketAdminCannotLogInAsSuperAdmin() {
        User user = user(UserRole.MARKET_ADMIN, 0);
        when(mapper.selectOne(any())).thenReturn(user);

        assertEquals("角色不匹配", assertThrows(RuntimeException.class,
                () -> service.login("manager", "valid-password", UserRole.SUPER_ADMIN)).getMessage());
        verifyNoInteractions(jwt);
    }

    @Test
    void superAdminTokenContainsDistinctRole() {
        User user = user(UserRole.SUPER_ADMIN, 1);
        when(mapper.selectOne(any())).thenReturn(user);
        when(jwt.generateToken("manager", UserRole.SUPER_ADMIN)).thenReturn("signed-token");

        assertEquals("signed-token", service.login("manager", "valid-password", UserRole.SUPER_ADMIN));
    }

    private User user(String role, int superAdmin) {
        User user = new User();
        user.setUsername("manager");
        user.setPassword("hash");
        user.setRole(role);
        user.setIsSuperAdmin(superAdmin);
        user.setStatus(1);
        return user;
    }
}
