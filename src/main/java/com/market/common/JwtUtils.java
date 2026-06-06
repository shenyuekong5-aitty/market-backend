package com.market.common;

import io.jsonwebtoken.*;                   // JWT 核心库，负责构建、解析、校验 Token
import io.jsonwebtoken.security.Keys;      // 用于生成安全的密钥
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;              // 密钥对象，用于签名和验证
import java.nio.charset.StandardCharsets;   // 指定字符编码为 UTF-8
import java.util.Date;

/**
 * JWT 工具类
 * 负责生成、解析和验证 JWT Token。
 * Token 中存储用户名和角色信息，用于后续登录态和权限控制。
 */
@Component  // 注册为 Spring Bean，方便在需要的地方注入使用
public class JwtUtils {

    // 签名密钥，用于保证 Token 内容不被篡改
    private final SecretKey key;

    // Token 的有效时长，单位：毫秒（例如：86400000 表示 24 小时）
    private final long expiration;

    /**
     * 构造函数，从配置文件中读取 JWT 的 secret 和过期时间
     * @param secret     配置文件中的 jwt.secret，用于生成签名密钥
     * @param expiration 配置文件中的 jwt.expiration，Token 过期时间（毫秒）
     */
    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration ) {
        // 使用 HMAC-SHA 算法，基于 secret 字符串生成一个安全的密钥
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     * @param username 用户名（可以是账号或手机号等标识）
     * @param role     用户角色（如 "ADMIN"、"VENDOR"、"USER"），用于后端接口权限校验
     * @return 生成的 JWT 字符串，客户端每次请求需携带在 Header 中
     */
    public String generateToken(String username, String role) {
        Date now = new Date();                              // 当前时间
        Date expiryDate = new Date(now.getTime() + expiration);  // 过期时间 = 当前时间 + 有效期
        return Jwts.builder()
                .subject(username)          // 设置主题，通常放用户标识（这里是用户名）
                .claim("role", role)        // 自定义声明，存入角色信息
                .issuedAt(now)              // 签发时间
                .expiration(expiryDate)     // 过期时间
                .signWith(key)              // 使用密钥签名，保证 Token 不被篡改
                .compact();                 // 生成最终的 Token 字符串
    }

    /**
     * 解析 Token，获取其中存储的所有声明（Claims）
     * @param token JWT 字符串
     * @return Claims 对象，类似 Map，可通过 get("key") 或 getSubject() 获取值
     *         例如：claims.getSubject() 得到用户名，claims.get("role") 得到角色
     * @throws JwtException 若 Token 无效、过期或签名错误，会抛出相应异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)              // 设置验证签名所用的密钥
                .build()                      // 构建解析器
                .parseSignedClaims(token)     // 解析 JWT，校验签名，返回 Jws<Claims>
                .getPayload();                // 从解析结果中获取 Claims 部分（即 Token 的有效载荷）
    }

    /**
     * 校验 Token 是否有效（未过期、签名正确）
     * @param token JWT 字符串
     * @return true 有效，false 无效（签名错误、过期、格式不对等）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);  // 能正常解析则说明有效
            return true;
        } catch (JwtException e) {
            // JwtException 是所有 JWT 相关异常的父类，包括过期、签名错误等
            return false;
        }
    }
}