package org.example.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务流步骤追踪信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepTraceDTO {

    /**
     * 步骤编码
     */
    private String stepCode;

    /**
     * 步骤状态（SUCCESS/FAILED）
     */
    private String status;

    /**
     * 步骤输入摘要
     */
    private String inputSummary;

    /**
     * 步骤输出摘要
     */
    private String outputSummary;

    /**
     * 步骤耗时（毫秒）
     */
    private Long costMs;

    /**
     * 失败信息
     */
    private String errorMessage;
}

