package org.example.springboot.dto;

import lombok.Data;

import java.util.List;

/**
 * 学习版通用智能体问答请求
 */
@Data
public class LearnAgentAskRequest {

    /**
     * 用户问题（必填）
     */
    private String question;

    /**
     * 检索文档数量（可选，默认3，限制1-10）
     */
    private Integer topK;

    /**
     * 历史对话（可选，用于环境扫描）
     */
    private List<String> history;

    /**
     * 本轮会话附件（可选）
     * 仅支持 PDF 类型，不入知识库，仅用于当前智能体任务流
     */
    private List<LearnAgentAttachmentDTO> attachments;
}
