package org.example.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 企业级配置类
 * 
 * 🎯 核心职责：
 * 1. 统一认证授权管理 - Spring Security统一处理所有安全相关功能
 * 2. JWT过滤器集成 - 自定义JWT认证过滤器集成到Spring Security过滤器链
 * 3. 无状态会话管理 - 适合微服务和分布式架构
 * 4. 方法级安全支持 - 支持@PreAuthorize等注解
 * 5. 一处配置全局生效 - 消除重复配置，统一维护
 * 
 * 🚀 架构优化：
 * - 解决循环依赖问题：通过延迟注入和职责分离
 * - 提高代码可维护性：清晰的依赖关系
 * - 符合Spring最佳实践：避免复杂的Bean依赖关系
 * 
 * @author system
 * @date 2025-01-27
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级安全，支持@PreAuthorize等注解
public class SecurityConfig {

    /**
     * 公开访问路径配置（按功能模块分组）
     * 这些路径不需要JWT验证，可以直接访问
     */
    
    // ==================== 系统基础路径 ====================
    private static final String[] SYSTEM_PATHS = {
        "/", "/health", "/favicon.ico"
    };
    
    // ==================== API文档路径 ====================
    private static final String[] DOC_PATHS = {
        "/doc.html", "/webjars/**", "/v3/api-docs/**", 
        "/swagger-ui/**", "/swagger-resources/**"
    };
    
    // ==================== 认证相关接口 ====================
    private static final String[] AUTH_PATHS = {
        "/api/user/login",           // 用户登录
        "/api/user/register",        // 用户注册
        "/api/user/forget",          // 忘记密码
        "/api/user/add",             // 用户注册（添加）
        "/api/user/auth",            // 匿名认证
        "/api/user/role"             // 匿名认证
    };
    
    // ==================== 用户公开接口 ====================
    private static final String[] USER_PUBLIC_PATHS = {
        "/api/user/{id}",            // 用户公开信息
        "/api/user/username/{username}" // 根据用户名查询
    };
    
    // ==================== 文章公开接口（博客前台） ====================
    private static final String[] ARTICLE_PUBLIC_PATHS = {
        "/api/article/page",         // 文章分页列表
        "/api/article/{id}",         // 文章详情
        "/api/article/recommend",    // 推荐文章
        "/api/article/hot",          // 热门文章
        "/api/article/recent",       // 最近文章
        "/api/article/search",       // 文章搜索
        "/api/article/category/{categoryId}", // 分类下的文章
        "/api/article/tag/{tagId}",  // 标签下的文章
        "/api/article/{id}/password/required", // 检查文章是否需要密码
        "/api/article/{id}/like/users",   // 文章点赞用户
        "/api/article/{id}/collect/users" // 文章收藏用户
    };
    
    // ==================== 分类/标签公开接口 ====================
    private static final String[] CATEGORY_TAG_PUBLIC_PATHS = {
        "/api/category",             // 分类列表
        "/api/category/tree",        // 分类树
        "/api/category/top-level",   // 顶级分类
        "/api/category/{id}",        // 分类详情
        "/api/tag",                  // 标签列表
        "/api/tag/{id}"              // 标签详情
    };
    
    // ==================== 评论公开接口 ====================
    private static final String[] COMMENT_PUBLIC_PATHS = {
        "/api/comment/article/{articleId}", // 文章评论列表
        "/api/comment/recent"        // 最近评论
    };
    
    // ==================== 博客配置/友链/时间线公开接口 ====================
    private static final String[] BLOG_INFO_PUBLIC_PATHS = {
        "/api/config",               // 博客配置
        "/api/config/{key}",         // 单个配置项
        "/api/friendLinks/visible",  // 可见友链
        "/api/timeline/visible",     // 可见时间线
        "/api/sentence/random/homepage", // 随机首页句子
        "/api/sentence/page",        // 句子分页
        "/api/sentence/{id}",        // 句子详情
        "/api/dashboard/stats"       // 仪表盘统计（首页展示）
    };
    
    // ==================== 商品公开接口 ====================
    private static final String[] PRODUCT_PUBLIC_PATHS = {
        "/api/product/page",         // 商品分页列表
        "/api/product/{id}",         // 商品详情
        "/api/product/{id}/images",  // 商品图片
        "/api/product/{id}/view",    // 增加浏览量
        "/api/product-category",     // 商品分类列表
        "/api/product-category/enabled", // 启用的分类
        "/api/product-category/{id}" // 分类详情
    };
    
    // ==================== 知识库公开接口 ====================
    private static final String[] KNOWLEDGE_PUBLIC_PATHS = {
        "/api/knowledge/**"          // 知识库问答（全部公开）
    };
    // ==================== 其它公开接口 ====================
    private static final String[] OTHER_PUBLIC_PATHS = {
        "/api/mytest/**",          // 测试
        "/api/ws/**"          // 测试
    };
    
    // ==================== 静态资源路径 ====================
    private static final String[] STATIC_PATHS = {
        "/static/**", "/files/**", "/file/**", "/*.html"
    };
    
    /**
     * 合并所有公开路径
     */
    private static final String[] PUBLIC_PATHS = mergeArrays(
        SYSTEM_PATHS, DOC_PATHS, AUTH_PATHS, USER_PUBLIC_PATHS,
        ARTICLE_PUBLIC_PATHS, CATEGORY_TAG_PUBLIC_PATHS, COMMENT_PUBLIC_PATHS,
        BLOG_INFO_PUBLIC_PATHS, PRODUCT_PUBLIC_PATHS, KNOWLEDGE_PUBLIC_PATHS,OTHER_PUBLIC_PATHS, STATIC_PATHS
    );
    
    /**
     * 合并多个字符串数组
     */
    private static String[] mergeArrays(String[]... arrays) {
        return java.util.Arrays.stream(arrays)
            .flatMap(java.util.Arrays::stream)
            .toArray(String[]::new);
    }

    /**
     * 密码编码器Bean
     * 
     * 🎯 职责分离：
     * - 独立定义，避免与其他Bean形成循环依赖
     * - 使用BCrypt加密算法，安全性高
     * - 全局共享，其他服务可以直接注入使用
     * 
     * @return PasswordEncoder BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT认证过滤器Bean
     * 
     * 🎯 解决循环依赖：
     * - 通过@Bean方式创建，而不是@Resource注入
     * - Spring容器会自动处理依赖关系
     * - 避免SecurityConfig直接依赖JwtAuthenticationFilter
     * 
     * @return JwtAuthenticationFilter JWT认证过滤器实例
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * 配置Spring Security过滤器链
     * 
     * 核心功能：
     * 1. 禁用CSRF - 适合API服务
     * 2. 无状态会话 - 适合JWT认证
     * 3. 路径权限配置 - 公开路径vs受保护路径
     * 4. JWT过滤器集成 - 自定义认证逻辑
     * 
     *  安全策略：
     * - 默认所有请求需要认证
     * - 公开路径允许匿名访问
     * - JWT过滤器在用户名密码认证之前执行
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护（API服务通常不需要）
            .csrf(csrf -> csrf.disable())
            
            // 配置会话管理为无状态（适合JWT）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开路径，允许匿名访问
                .requestMatchers(PUBLIC_PATHS).permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}