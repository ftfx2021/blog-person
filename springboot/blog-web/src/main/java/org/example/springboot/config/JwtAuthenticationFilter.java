package org.example.springboot.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.example.springboot.entity.User;

import org.example.springboot.enums.AccountStatus;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 * 1. 继承OncePerRequestFilter确保每个请求只执行一次
 * 2. 集成Spring Security认证机制
 * 3. 统一的token验证和用户上下文设置
 * 4. 完善的异常处理和日志记录
 * 5. 标准的用户认证系统，支持角色权限
 *
 * @author system
 * @date 2025-01-13
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JWT认证过滤器处理请求：{} {}", method, requestUri);

        try {
            // 1. 提取JWT token
            String token = JwtTokenUtils.extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {

                // 2. 验证token并获取用户信息
                Long userId = JwtTokenUtils.getUserIdFromToken(token);
                String username = JwtTokenUtils.getUsernameFromToken(token);
                String roleCode = JwtTokenUtils.getRoleTypeFromToken(token);

                if (userId != null && StringUtils.hasText(username) && StringUtils.hasText(roleCode)) {
                    // 检查token是否过期
                    if (JwtTokenUtils.isTokenExpired(token)) {
                        log.warn("JWT token已过期，用户ID：{}，用户名：{}", userId, username);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 3. 查询用户信息（可选，用于验证用户是否仍然存在和有效）
                    User user = userService.getUserByIdInternal(userId);

                    if (user != null && user.getStatus().equals(AccountStatus.NORMAL.getValue())) { // 用户存在且状态正常
                        // 4. 创建Spring Security认证对象
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + roleCode)
                        );

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        authorities
                                );

                        // 5. 设置认证信息到Spring Security上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 6. 设置用户信息到请求属性（方便Controller使用）
                        request.setAttribute("currentUser", user);
                        request.setAttribute("currentUserId", userId);
                        request.setAttribute("currentUsername", username);
                        request.setAttribute("currentUserRole", roleCode);

                        log.debug("JWT认证成功，用户ID：{}，用户名：{}，角色：{}", userId, username, roleCode);
                    } else {
                        log.warn("JWT验证失败：用户不存在或已被禁用，用户ID：{}，用户名：{}", userId, username);
                        // 清理认证上下文，防止安全问题
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.warn("JWT验证失败：无法从token中解析完整的用户信息");
                    // 清理认证上下文
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("未找到token，跳过JWT认证");
            }
        } catch (JWTVerificationException e) {
            log.warn("JWT验证失败：{}，清理认证上下文", e.getMessage());
            // JWT验证失败时清理认证上下文
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("JWT认证过程中发生异常，请求：{} {}，异常：{}，清理认证上下文", method, requestUri, e.getMessage(), e);
            // 发生异常时清理认证上下文，确保安全
            SecurityContextHolder.clearContext();
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

} 