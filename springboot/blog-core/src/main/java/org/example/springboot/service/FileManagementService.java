package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件管理服务 - 全新重构版本
 * 
 * 文件存储策略：
 * 1. 业务文件（OneToOne）：/业务类型/业务id/xxx.jpg
 * 2. 业务文件（OneToMany）：/业务类型/业务id/xxx.jpg + JSON字段
 * 3. 非结构化数据（富文本）：/业务类型/业务id/xxx.jpg
 * 4. 临时文件：/temp/xxx.jpg（24小时自动清理）
 * 
 * @author system
 */
@Slf4j
@Service
public class FileManagementService {

    @Value("${file.upload.path:/files}")
    private String uploadBasePath;

    private static final String TEMP_DIR = "temp";
    private static final Pattern TEMP_URL_PATTERN = Pattern.compile("/files/temp/([^\"\\s]+)");

    /**
     * 上传文件到临时目录
     * 用于所有类型的文件上传第一步
     */
    public String uploadToTemp(MultipartFile file) {
        try {
            log.info("上传文件到临时目录: {}", file.getOriginalFilename());
            
            // 基础验证
            FileUtil.validateBasicFile(file);
            
            // 保存到temp目录
            String tempPath = FileUtil.saveFile(file, TEMP_DIR, null);
            
            if (StrUtil.isBlank(tempPath)) {
                throw new BusinessException("文件保存失败");
            }
            
            log.info("文件已保存到临时目录: {}", tempPath);
            return tempPath;
            
        } catch (Exception e) {
            log.error("上传文件到临时目录失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件到临时目录
     */
    public List<String> uploadMultipleToTemp(MultipartFile[] files) {
        List<String> tempPaths = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                String tempPath = uploadToTemp(file);
                tempPaths.add(tempPath);
            } catch (Exception e) {
                log.error("批量上传中某个文件失败: {}", file.getOriginalFilename(), e);
                // 继续处理其他文件
            }
        }
        
        return tempPaths;
    }

    /**
     * 将临时文件移动到正式目录（OneToOne场景）
     * 
     * @param tempUrl 临时文件URL
     * @param businessType 业务类型（如：user_avatar, article_cover）
     * @param businessId 业务ID
     * @return 正式文件URL（带/files/前缀）
     */
    public String moveTempToFormal(String tempUrl, String businessType, String businessId) {
        try {
            log.info("移动临时文件到正式目录: tempUrl={}, businessType={}, businessId={}", 
                    tempUrl, businessType, businessId);
            
            if (StrUtil.isBlank(tempUrl)) {
                throw new BusinessException("临时文件URL不能为空");
            }
            
            // 提取临时文件路径
            String tempPath = extractPathFromUrl(tempUrl);
            
            // 构建目标路径
            String targetDir = String.format("bussiness/%s/%s", businessType, businessId);
            String fileName = getFileNameFromPath(tempPath);
            String targetPath = String.format("%s/%s", targetDir, fileName);
            
            // 移动文件
            moveFile(tempPath, targetPath);
            
            // 返回完整的URL路径
            String fullUrl = "/files/" + targetPath;
            log.info("文件已移动到正式目录: {}", fullUrl);
            return fullUrl;
            
        } catch (Exception e) {
            log.error("移动临时文件失败: {}", e.getMessage(), e);
            throw new BusinessException("文件移动失败: " + e.getMessage());
        }
    }

    /**
     * 批量将临时文件移动到正式目录（OneToMany场景）
     * 
     * @param tempUrls 临时文件URL列表
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 正式文件路径列表
     */
    public List<String> moveTempToFormalBatch(List<String> tempUrls, String businessType, String businessId) {
        List<String> formalPaths = new ArrayList<>();
        
        if (tempUrls == null || tempUrls.isEmpty()) {
            return formalPaths;
        }
        
        for (String tempUrl : tempUrls) {
            try {
                String formalPath = moveTempToFormal(tempUrl, businessType, businessId);
                formalPaths.add(formalPath);
            } catch (Exception e) {
                log.error("批量移动文件时某个文件失败: {}", tempUrl, e);
                // 继续处理其他文件
            }
        }
        
        return formalPaths;
    }

    /**
     * 处理富文本内容中的临时文件（非结构化数据场景）
     * 
     * @param htmlContent 富文本内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 处理后的富文本内容（临时URL已替换为正式URL）
     */
    public String processRichTextFiles(String htmlContent, String businessType, String businessId) {
        try {
            log.info("处理富文本中的临时文件: businessType={}, businessId={}", businessType, businessId);
            
            if (StrUtil.isBlank(htmlContent)) {
                return htmlContent;
            }
            
            // 提取所有临时文件URL
            List<String> tempUrls = extractTempUrls(htmlContent);
            
            if (tempUrls.isEmpty()) {
                log.info("富文本中没有临时文件");
                return htmlContent;
            }
            
            log.info("富文本中发现{}个临时文件", tempUrls.size());
            
            // 移动文件并替换URL
            String processedContent = htmlContent;
            for (String tempUrl : tempUrls) {
                try {
                    String formalUrl = moveTempToFormal(tempUrl, businessType, businessId);
                    processedContent = processedContent.replace(tempUrl, formalUrl);
                    log.info("替换临时URL: {} -> {}", tempUrl, formalUrl);
                } catch (Exception e) {
                    log.error("处理临时文件失败: {}", tempUrl, e);
                    // 继续处理其他文件
                }
            }
            
            return processedContent;
            
        } catch (Exception e) {
            log.error("处理富文本文件失败: {}", e.getMessage(), e);
            throw new BusinessException("处理富文本文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除单个文件（通过URL）
     * 
     * @param fileUrl 文件URL（如：/files/bussiness/article_cover/123/xxx.jpg）
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            if (StrUtil.isBlank(fileUrl)) {
                log.debug("文件URL为空，无需删除");
                return;
            }
            
            log.info("删除文件: {}", fileUrl);
            
            // 提取文件路径
            String filePath = extractPathFromUrl(fileUrl);
            String fullPath = Paths.get(uploadBasePath, filePath).toString();
            
            File file = new File(fullPath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    log.info("文件已删除: {}", fileUrl);
                } else {
                    log.warn("文件删除失败: {}", fileUrl);
                }
            } else {
                log.debug("文件不存在，无需删除: {}", fileUrl);
            }
            
        } catch (Exception e) {
            log.error("删除文件失败: fileUrl={}, error={}", fileUrl, e.getMessage());
            // 不抛出异常，避免影响业务更新
        }
    }

    /**
     * 删除业务目录下的所有文件
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    public void deleteBusinessFiles(String businessType, String businessId) {
        try {
            log.info("删除业务文件: businessType={}, businessId={}", businessType, businessId);
            
            String businessDir = String.format("bussiness/%s/%s", businessType, businessId);
            String fullPath = Paths.get(uploadBasePath, businessDir).toString();
            
            File dir = new File(fullPath);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectory(dir);
                log.info("业务目录已删除: {}", businessDir);
            } else {
                log.info("业务目录不存在，无需删除: {}", businessDir);
            }
            
        } catch (Exception e) {
            log.error("删除业务文件失败: businessType={}, businessId={}", businessType, businessId, e);
            // 不抛出异常，避免影响业务删除
        }
    }

    /**
     * 清理过期的临时文件（24小时）
     * 
     * @return 清理的文件数量
     */
    public int cleanupExpiredTempFiles() {
        try {
            log.info("开始清理过期临时文件");
            
            String tempDirPath = Paths.get(uploadBasePath, TEMP_DIR).toString();
            File tempDir = new File(tempDirPath);
            
            if (!tempDir.exists() || !tempDir.isDirectory()) {
                log.info("临时目录不存在");
                return 0;
            }
            
            long now = System.currentTimeMillis();
            long expirationTime = 24 * 60 * 60 * 1000; // 24小时
            int deletedCount = 0;
            
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        long fileAge = now - file.lastModified();
                        if (fileAge > expirationTime) {
                            if (file.delete()) {
                                deletedCount++;
                                log.debug("删除过期临时文件: {}", file.getName());
                            }
                        }
                    }
                }
            }
            
            log.info("清理过期临时文件完成，删除{}个文件", deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("清理过期临时文件失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从URL中提取文件路径
     */
    private String extractPathFromUrl(String url) {
        if (url.startsWith("/files/")) {
            return url.substring(7); // 移除 "/files/" 前缀
        } else if (url.startsWith("files/")) {
            return url.substring(6); // 移除 "files/" 前缀
        }
        return url;
    }

    /**
     * 从路径中获取文件名
     */
    private String getFileNameFromPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    /**
     * 移动文件
     */
    private void moveFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(uploadBasePath, sourcePath);
        Path target = Paths.get(uploadBasePath, targetPath);
        
        // 确保目标目录存在
        Files.createDirectories(target.getParent());
        
        // 移动文件
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("文件已移动: {} -> {}", sourcePath, targetPath);
    }

    /**
     * 提取富文本中的所有临时文件URL
     */
    private List<String> extractTempUrls(String htmlContent) {
        List<String> tempUrls = new ArrayList<>();
        Matcher matcher = TEMP_URL_PATTERN.matcher(htmlContent);
        
        while (matcher.find()) {
            String tempUrl = "/files/temp/" + matcher.group(1);
            tempUrls.add(tempUrl);
        }
        
        return tempUrls;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
