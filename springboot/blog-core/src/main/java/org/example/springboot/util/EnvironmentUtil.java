package org.example.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * 环境工具类
 * 提供环境检测和配置获取功能
 */
@Slf4j
@Component
public class EnvironmentUtil {
    
    @Resource
    private Environment environment;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @PostConstruct
    public void init() {
        log.info("环境工具类初始化完成，当前环境: {}", activeProfile);
    }
    
    /**
     * 是否为开发环境
     */
    public boolean isDev() {
        return "dev".equals(activeProfile) || environment.acceptsProfiles("dev");
    }
    
    /**
     * 是否为测试环境
     */
    public boolean isTest() {
        return "test".equals(activeProfile) || environment.acceptsProfiles("test");
    }
    
    /**
     * 是否为生产环境
     */
    public boolean isProd() {
        return "prod".equals(activeProfile) || environment.acceptsProfiles("prod");
    }
    
    /**
     * 获取当前环境名称
     */
    public String getCurrentEnvironment() {
        return activeProfile;
    }
    
    /**
     * 获取环境配置值
     */
    public String getProperty(String key) {
        return environment.getProperty(key);
    }
    
    /**
     * 获取环境配置值，带默认值
     */
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
    
    /**
     * 检查是否启用了某个功能
     */
    public boolean isFeatureEnabled(String featureName) {
        String key = "app.features." + featureName + ".enabled";
        return Boolean.parseBoolean(getProperty(key, "false"));
    }
    
    /**
     * 获取数据库URL
     */
    public String getDatabaseUrl() {
        return getProperty("spring.datasource.url");
    }
    
    /**
     * 获取服务器端口
     */
    public String getServerPort() {
        return getProperty("server.port", "8080");
    }
    
    /**
     * 打印环境信息
     */
    public void printEnvironmentInfo() {
        log.info("=== 环境信息 ===");
        log.info("当前环境: {}", getCurrentEnvironment());
        log.info("是否开发环境: {}", isDev());
        log.info("是否测试环境: {}", isTest());
        log.info("是否生产环境: {}", isProd());
        log.info("服务器端口: {}", getServerPort());
        log.info("数据库URL: {}", getDatabaseUrl());
        log.info("===============");
    }
}
