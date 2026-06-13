package org.example.springboot.agent.learn;

import lombok.Data;
import org.example.springboot.dto.LearnAgentAttachmentDTO;
import org.example.springboot.dto.LearnAgentAskResponse;
import org.example.springboot.dto.StepTraceDTO;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习版任务流上下文
 */
@Data
public class WorkflowContext {

    /**
     * 用户问题
     */
    private String question;

    /**
     * 历史对话
     */
    private List<String> history = new ArrayList<>();

    /**
     * 本轮附件（上传后引用）
     */
    private List<LearnAgentAttachmentDTO> attachments = new ArrayList<>();

    /**
     * 附件转换后的工具参数信息
     */
    private List<Map<String, Object>> attachmentToolSources = new ArrayList<>();

    /**
     * 相似检索数量
     */
    private int topK;

    /**
     * 检索到的文档
     */
    private List<Document> docs = new ArrayList<>();

    /**
     * 生成的答案
     */
    private String answer;

    /**
     * 任务识别结果
     */
    private Map<String, Object> mission = new LinkedHashMap<>();

    /**
     * 环境扫描结果
     */
    private Map<String, Object> scene = new LinkedHashMap<>();

    /**
     * 思考规划
     */
    private List<String> plan = new ArrayList<>();

    /**
     * 执行动作结果
     */
    private Map<String, Object> action = new LinkedHashMap<>();

    /**
     * 观察迭代结果
     */
    private List<String> observations = new ArrayList<>();

    /**
     * 步骤追踪
     */
    private List<StepTraceDTO> stepTraces = new ArrayList<>();

    /**
     * 开始时间（毫秒）
     */
    private long startTime;

    /**
     * 工具调用事件监听
     */
    private LearnToolRegistry.ToolExecutionListener toolExecutionListener = LearnToolRegistry.ToolExecutionListener.NO_OP;

    /**
     * 编排模式
     */
    private String orchestrationMode;

    /**
     * 子AGENT决策轨迹
     */
    private List<Map<String, Object>> orchestrationDecisions = new ArrayList<>();

    /**
     * 最终响应对象
     */
    private LearnAgentAskResponse response;
}
