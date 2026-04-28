package org.example.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 学习版通用智能体问答响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnAgentAskResponse {

    /**
     * 智能体回答
     */
    private String answer;

    /**
     * 检索来源
     */
    private List<SourceItem> sources;

    /**
     * 步骤追踪
     */
    private List<StepTraceDTO> steps;

    /**
     * 总耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 执行上下文（任务识别/环境扫描/计划/动作/观察）
     */
    private Map<String, Object> executionContext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceItem {
        private String source;
        private String content;
        private Double score;
    }
}
