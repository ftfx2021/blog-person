package org.example.springboot.task;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.service.FileManagementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件清理定时任务
 * 每天凌晨2点自动清理过期的临时文件
 * 
 * @author system
 */
@Slf4j
@Component
public class FileCleanupTask {

    @Resource
    private FileManagementService fileManagementService;

    /**
     * 清理过期临时文件
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTempFiles() {
        try {
            log.info("开始执行定时任务：清理过期临时文件");
            int deletedCount = fileManagementService.cleanupExpiredTempFiles();
            log.info("定时任务执行完成：清理了{}个过期临时文件", deletedCount);
        } catch (Exception e) {
            log.error("定时任务执行失败：清理过期临时文件", e);
        }
    }
}
