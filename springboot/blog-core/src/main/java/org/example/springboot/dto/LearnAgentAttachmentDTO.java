package org.example.springboot.dto;

import lombok.Data;

/**
 * 学习版智能体附件信息
 */
@Data
public class LearnAgentAttachmentDTO {

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件访问路径（通常为 /files/temp/...）
     */
    private String fileUrl;

    /**
     * MIME 类型
     */
    private String mimeType;
}
