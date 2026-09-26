package com.market.common;

import com.market.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {
    @Test
    void legacyAdminsAreMappedBySuperAdminFlag() {
        User user = new User();
        user.setRole("admin");
        user.setIsSuperAdmin(1);
        assertEquals(UserRole.SUPER_ADMIN, UserRole.effectiveRole(user));
        assertTrue(UserRole.isSuperAdmin(user));

        user.setIsSuperAdmin(0);
        assertEquals(UserRole.MARKET_ADMIN, UserRole.effectiveRole(user));
        assertTrue(UserRole.isMarketAdmin(user));
    }

    @Test
    void newRolesRemainDistinct() {
        User user = new User();
        user.setRole(UserRole.SUPER_ADMIN);
        assertTrue(UserRole.isSuperAdmin(user));
        user.setRole(UserRole.MARKET_ADMIN);
        assertFalse(UserRole.isSuperAdmin(user));
        assertTrue(UserRole.isAdmin(user));
        user.setRole(UserRole.VENDOR);
        assertFalse(UserRole.isAdmin(user));
    }
}
