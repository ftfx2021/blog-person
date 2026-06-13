package org.example.springboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.config.JwtConfig;
import org.example.springboot.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * JWT工具类 - 用于JWT token的生成、验证和解析
 *
 * 主要功能：
 * 1. 生成JWT token（使用userId、username、roleType作为标识）
 * 2. 验证JWT token有效性
 * 3. 解析token中的用户信息
 * 4. 从请求中获取当前用户信息
 *
 * 安全特性：
 * - 使用固定密钥进行签名
 * - 支持token过期时间配置
 * - 完善的异常处理和日志记录
 * - 标准的用户认证系统
 */
@Slf4j
@Component
public class JwtTokenUtils {

    private static JwtConfig jwtConfig;


    @Autowired
    public void setJwtConfig(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Token发行者
     */
    private static final String ISSUER = "jx-blog";

    /**
     * 生成JWT token
     * @param userId 用户ID
     * @param username 用户名
     * @param roleType 角色代码
     * @return JWT token
     */
    public static String generateToken(Long userId, String username, String roleType) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            Date expireDate = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());

            return JWT.create()
                    .withClaim("userId", userId)
                    .withClaim("username", username)
                    .withClaim("roleType", roleType)
                    .withExpiresAt(expireDate)
                    .withIssuedAt(new Date())
                    .withIssuer(ISSUER)
                    .sign(algorithm);
        } catch (Exception e) {
            log.error("生成JWT token失败", e);
            throw new RuntimeException("生成JWT token失败", e);
        }
    }

    /**
     * 验证JWT token有效性
     * @param token JWT token
     * @return 解码后的JWT
     * @throws JWTVerificationException token验证失败
     */
    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    /**
     * 从token中获取用户ID
     * @param token JWT token
     * @return 用户ID
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static Long getUserIdFromToken(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * 从token中获取用户名
     * @param token JWT token
     * @return 用户名
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getUsernameFromToken(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("username").asString();
    }

    /**
     * 从token中获取角色代码
     * @param token JWT token
     * @return 角色代码
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getRoleTypeFromToken(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("roleType").asString();
    }

    /**
     * 检查token是否过期
     * @param token JWT token
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从请求中提取token
     * @param request HTTP请求
     * @return token字符串
     */
    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        return null;
    }

    /**
     * 从请求中获取当前用户ID
     * @param request HTTP请求
     * @return 用户ID
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static Long getCurrentUserIdFromRequest(HttpServletRequest request) throws JWTVerificationException {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 从请求中获取当前用户名
     * @param request HTTP请求
     * @return 用户名
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getCurrentUsernameFromRequest(HttpServletRequest request) throws JWTVerificationException {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return getUsernameFromToken(token);
        }
        return null;
    }

    /**
     * 从请求中获取当前用户角色
     * @param request HTTP请求
     * @return 角色代码
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getCurrentRoleFromRequest(HttpServletRequest request) throws JWTVerificationException {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return getRoleTypeFromToken(token);
        }
        return null;
    }

    /**
     * 刷新token（生成新的token）
     * @param oldToken 旧的token
     * @return 新的token，失败返回null
     */
    public static String refreshToken(String oldToken) {
        try {
            Long userId = getUserIdFromToken(oldToken);
            String username = getUsernameFromToken(oldToken);
            String roleType = getRoleTypeFromToken(oldToken);

            if (userId != null && username != null && roleType != null) {
                return generateToken(userId, username, roleType);
            }
        } catch (Exception e) {
            log.error("刷新token失败", e);
        }
        return null;
    }

    /**
     * 获取当前请求的用户ID（从RequestContextHolder获取）
     * @return 当前用户ID
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static Long getCurrentUserId() throws JWTVerificationException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getCurrentUserIdFromRequest(request);
        }
        return null;
    }

    /**
     * 获取当前请求的用户名（从RequestContextHolder获取）
     * @return 当前用户名
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getCurrentUsername() throws JWTVerificationException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getCurrentUsernameFromRequest(request);
        }
        return null;
    }

    /**
     * 获取当前请求的用户角色（从RequestContextHolder获取）
     * @return 当前用户角色
     * @throws JWTVerificationException token验证失败时抛出异常
     */
    public static String getCurrentUserRole() throws JWTVerificationException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getCurrentRoleFromRequest(request);
        }
        return null;
    }

    /**
     * 获取当前请求的用户信息（从请求属性中获取）
     * 注意：此方法依赖于JwtAuthenticationFilter设置的请求属性
     * @return 当前用户对象，获取失败返回null
     */
    public static User getCurrentUser() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return (User) request.getAttribute("currentUser");
            }
        } catch (Exception e) {
            log.error("获取当前用户对象失败", e);
        }
        return null;
    }
    


}
