package com.market.common;

import com.market.entity.User;

/** 统一的账号身份；兼容迁移前 role=admin 的数据。 */
public final class UserRole {
    public static final String SUPER_ADMIN = "super_admin";
    public static final String MARKET_ADMIN = "market_admin";
    public static final String VENDOR = "vendor";
    public static final String USER = "user";

    private UserRole() {}

    public static String effectiveRole(User user) {
        if (user == null) return null;
        if ("admin".equals(user.getRole())) {
            return Integer.valueOf(1).equals(user.getIsSuperAdmin()) ? SUPER_ADMIN : MARKET_ADMIN;
        }
        return user.getRole();
    }

    public static boolean isSuperAdmin(User user) {
        return SUPER_ADMIN.equals(effectiveRole(user));
    }

    public static boolean isMarketAdmin(User user) {
        return MARKET_ADMIN.equals(effectiveRole(user));
    }

    public static boolean isAdmin(User user) {
        return isSuperAdmin(user) || isMarketAdmin(user);
    }
}
