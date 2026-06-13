package org.example.springboot.listener;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.service.es.ArticleEsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch初始化监听器
 * 系统启动完成后自动同步数据到ES
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "es",name = "init-on-startup",havingValue = "true")
public class ElasticsearchInitListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ArticleEsService articleEsService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("========== Elasticsearch初始化开始 ==========");
        
        try {
            // 1. 检查索引是否存在
            if (!articleEsService.indexExist()) {
                log.info("ES索引不存在，开始创建索引并同步数据...");
                articleEsService.createIndex();
                articleEsService.syncAllFromDatabase();
                log.info("ES索引创建并同步完成");
            } else {
                log.info("ES索引已存在，跳过初始化");
                // 可选：如果需要每次启动都同步，取消下面的注释
                // log.info("开始增量同步数据...");
                 articleEsService.syncAllFromDatabase();
            }

            log.info("========== Elasticsearch初始化完成 ==========");

        } catch (Exception e) {
            log.error("Elasticsearch初始化失败: {}", e.getMessage(), e);
            // 注意：这里不抛出异常，避免影响系统启动
            log.warn("ES初始化失败，但系统将继续启动。请检查ES连接配置。");
        }
    }
}
