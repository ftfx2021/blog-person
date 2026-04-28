package org.example.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 企业级统一配置
 * 
 * 🎯 统一职责：
 * 1. API前缀配置
 * 2. 静态资源映射配置  
 * 3. API文档资源配置
 * 
 * 🚀 架构优势：
 * - 统一管理所有Web相关配置
 * - 避免多个配置类冲突
 * - 职责清晰，易于维护
 * - 符合单一配置原则
 * 
 * @author system
 * @date 2025-01-27
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置API路径前缀
     * 
     * 为所有带有@RestController注解的控制器类自动添加"/api"前缀
     * 这样可以将API接口与其他Web资源（如静态资源、文档）区分开
     * 
     *  工作原理：
     * - UserController: /user/* → /api/user/*
     * - FileController: /file/* → /api/file/*  
     * - EmailController: /email/* → /api/email/*
     * 
     * 排除规则：
     * - Swagger/Knife4j相关接口不添加前缀
     * - 通过包名判断排除springfox、swagger、doc相关包
     * 
     * @param configurer 路径匹配配置器
     * Spring MVC 路由 (RequestMappingHandlerMapping)：
     * 专门管 @RestController、@Controller 里的 @GetMapping、@PostMapping 等注解。
     * 你的 configurer.addPathPrefix("/api", ...) 就是作用在这个组件上的，它会自动给你的 Controller 接口加上前缀。
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", clazz ->
                clazz.isAnnotationPresent(RestController.class) &&
                        !clazz.getPackage().getName().contains("springfox") &&
                        !clazz.getPackage().getName().contains("swagger") &&
                        !clazz.getPackage().getName().contains("doc")
        );
    }

    /**
     * 统一配置静态资源映射
     * 
     * 🎯 配置目标：
     * 1. 静态资源访问路径
     * 2. 文件上传目录访问
     * 3. API文档相关资源
     * 4. 避免与API路径冲突
     * 
     * 📁 资源映射规则：
     * - /static/** → classpath:/static/ (项目静态资源)
     * - /files/** → file:./files/ (文件上传目录)
     * - /doc.html → Knife4j文档首页
     * - /webjars/** → Maven webjars资源
     * - /swagger-ui/** → Swagger UI资源
     * - /v3/api-docs/** → OpenAPI文档
     * 
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 静态资源配置 - 项目自定义静态文件
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 缓存1小时
        
        // 2. 文件上传目录配置 - 用户上传文件访问
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./files/")
                .setCachePeriod(86400); // 缓存24小时
        
        // 2. API文档资源配置 - Knife4j/Swagger相关
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
                
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
                
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
                
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
                
        registry.addResourceHandler("/v3/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
