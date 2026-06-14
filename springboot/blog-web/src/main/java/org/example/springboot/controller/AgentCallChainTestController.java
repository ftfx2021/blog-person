package org.example.springboot.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.springboot.chat.LLMService;
import org.example.springboot.chat.StreamCallback;
import org.example.springboot.chat.StreamCancellationHandle;
import org.example.springboot.common.Result;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.embedding.EmbeddingService;
import org.example.springboot.framework.ChatMessage;
import org.example.springboot.framework.ChatRequest;
import org.example.springboot.framework.RetrievedChunk;
import org.example.springboot.model.ModelHealthStore;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.model.ModelTarget;
import org.example.springboot.rerank.RerankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/agent-test")
public class AgentCallChainTestController {

    @Autowired(required = false)
    private LLMService llmService;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private RerankService rerankService;

    @Autowired
    private ModelSelector modelSelector;

    @Autowired
    private ModelHealthStore healthStore;

    @Autowired
    private AIModelProperties aiModelProperties;

    // ==================== 1. 模型选择器测试 ====================

    /**
     * 测试1：查看模型选择结果
     * 验证 ModelSelector.selectCandidates 是否返回有效列表
     * 预期结果：如果 filter(Objects::isNull) bug 存在，返回空列表
     */
    @GetMapping("/model-select/chat")
    public Result<?> testChatModelSelection() {
        List<ModelTarget> targets = modelSelector.selectChatModel(false);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("candidateCount", targets.size());
        info.put("isEmpty", targets.isEmpty());
        info.put("candidates", targets.stream().map(t -> Map.of(
                "id", t.id(),
                "provider", t.candidate().getProvider(),
                "model", t.candidate().getModel()
        )).toList());
        info.put("diagnosis", targets.isEmpty()
                ? "致命缺陷F-1：filter(Objects::isNull) 导致所有有效模型被过滤"
                : "模型选择正常");
        return Result.success(info);
    }

    /**
     * 测试1b：查看深度思考模型选择结果
     */
    @GetMapping("/model-select/chat-deep")
    public Result<?> testDeepThinkingModelSelection() {
        List<ModelTarget> targets = modelSelector.selectChatModel(true);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("candidateCount", targets.size());
        info.put("isEmpty", targets.isEmpty());
        info.put("candidates", targets.stream().map(t -> Map.of(
                "id", t.id(),
                "provider", t.candidate().getProvider(),
                "model", t.candidate().getModel()
        )).toList());
        return Result.success(info);
    }

    /**
     * 测试1c：查看 Embedding 模型选择结果
     */
    @GetMapping("/model-select/embedding")
    public Result<?> testEmbeddingModelSelection() {
        List<ModelTarget> targets = modelSelector.selectEmbeddingCandidates();
        return Result.success(Map.of(
                "candidateCount", targets.size(),
                "isEmpty", targets.isEmpty(),
                "candidates", targets.stream().map(t -> Map.of(
                        "id", t.id(),
                        "provider", t.candidate().getProvider(),
                        "model", t.candidate().getModel()
                )).toList()
        ));
    }

    /**
     * 测试1d：查看 Rerank 模型选择结果
     */
    @GetMapping("/model-select/rerank")
    public Result<?> testRerankModelSelection() {
        List<ModelTarget> targets = modelSelector.selectRerankCandidates();
        return Result.success(Map.of(
                "candidateCount", targets.size(),
                "isEmpty", targets.isEmpty(),
                "candidates", targets.stream().map(t -> Map.of(
                        "id", t.id(),
                        "provider", t.candidate().getProvider(),
                        "model", t.candidate().getModel()
                )).toList()
        ));
    }

    // ==================== 2. 熔断器测试 ====================

    /**
     * 测试2：查看熔断器状态
     */
    @GetMapping("/health/status")
    public Result<?> testHealthStoreStatus() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("message", "查看所有模型的熔断状态");
        info.put("note", "如果模型选择器返回空（F-1），此处不会有状态记录");
        return Result.success(info);
    }

    /**
     * 测试2b：手动触发熔断测试
     */
    @PostMapping("/health/mark-failure")
    public Result<?> testMarkFailure(@RequestParam String modelId) {
        healthStore.markFailure(modelId);
        return Result.success(Map.of(
                "action", "markFailure",
                "modelId", modelId,
                "isUnavailable", healthStore.isUnavailable(modelId)
        ));
    }

    /**
     * 测试2c：手动标记成功
     */
    @PostMapping("/health/mark-success")
    public Result<?> testMarkSuccess(@RequestParam String modelId) {
        healthStore.markSuccess(modelId);
        return Result.success(Map.of(
                "action", "markSuccess",
                "modelId", modelId,
                "isUnavailable", healthStore.isUnavailable(modelId)
        ));
    }

    // ==================== 3. 同步 Chat 测试 ====================

    /**
     * 测试3：同步 Chat 调用
     * 验证完整调用链：Controller → LLMService → ModelRoutingExecutor → ChatClient → HTTP
     */
    @PostMapping("/chat/sync")
    public Result<?> testSyncChat(@RequestParam(defaultValue = "你好，请用一句话介绍自己") String prompt) {
        if (llmService == null) {
            return Result.error("LLMService Bean 未注入，请检查 AgentServiceConfig");
        }
        log.info("========== 同步 Chat 测试开始 ==========");
        long start = System.currentTimeMillis();
        try {
            String response = llmService.chat(prompt);
            long cost = System.currentTimeMillis() - start;
            log.info("========== 同步 Chat 测试完成，耗时: {}ms ==========", cost);
            return Result.success(Map.of(
                    "prompt", prompt,
                    "response", response,
                    "costMs", cost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("同步 Chat 测试失败: {}", e.getMessage(), e);
            return Result.success(Map.of(
                    "success", false,
                    "prompt", prompt,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "costMs", cost,
                    "diagnosis", diagnoseError(e)
            ));
        }
    }

    /**
     * 测试3b：同步 Chat（带 thinking 参数）
     */
    @PostMapping("/chat/sync-thinking")
    public Result<?> testSyncChatWithThinking(@RequestParam(defaultValue = "1+1等于几") String prompt) {
        if (llmService == null) {
            return Result.error("LLMService Bean 未注入");
        }
        long start = System.currentTimeMillis();
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(ChatMessage.user(prompt)))
                    .thinking(true)
                    .build();
            String response = llmService.chat(request);
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "prompt", prompt,
                    "response", response,
                    "costMs", cost,
                    "thinking", true
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "costMs", cost,
                    "diagnosis", diagnoseError(e)
            ));
        }
    }

    // ==================== 4. 流式 Chat 测试 ====================

    /**
     * 测试4：流式 Chat（SSE）
     * 验证调用链：Controller → LLMService.streamChat → RoutingLLMService → ChatClient → SSE
     * 预期结果：如果 F-2 bug 存在，60秒后超时失败
     */
    @PostMapping("/chat/stream")
    public SseEmitter testStreamChat(@RequestParam(defaultValue = "你好，请用三句话介绍Java语言") String prompt) {
        SseEmitter emitter = new SseEmitter(120_000L);

        if (llmService == null) {
            emitter.completeWithError(new RuntimeException("LLMService Bean 未注入"));
            return emitter;
        }

        log.info("========== 流式 Chat 测试开始 ==========");

        StreamCancellationHandle handle = llmService.streamChat(prompt, new StreamCallback() {
            @Override
            public void onContent(String content) {
                try {
                    log.debug("收到内容: {}", content);
                    emitter.send(SseEmitter.event()
                            .name("content")
                            .data(content));
                } catch (Exception e) {
                    log.warn("发送 SSE 失败: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                log.info("========== 流式 Chat 测试完成 ==========");
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (Exception e) {
                    // ignore
                }
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式 Chat 测试失败: {}", error.getMessage(), error);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "error", error.getMessage(),
                                    "errorType", error.getClass().getSimpleName(),
                                    "diagnosis", diagnoseError(error)
                            )));
                } catch (Exception e) {
                    // ignore
                }
                emitter.completeWithError(error);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SseEmitter 超时，取消流式请求");
            handle.cancel();
        });

        emitter.onError(e -> {
            log.warn("SseEmitter 错误，取消流式请求");
            handle.cancel();
        });

        return emitter;
    }

    /**
     * 测试4b：流式 Chat（开启 thinking）
     */
    @PostMapping("/chat/stream-thinking")
    public SseEmitter testStreamChatWithThinking(@RequestParam(defaultValue = "1+1等于几") String prompt) {
        SseEmitter emitter = new SseEmitter(120_000L);

        if (llmService == null) {
            emitter.completeWithError(new RuntimeException("LLMService Bean 未注入"));
            return emitter;
        }

        log.info("========== 流式 Thinking Chat 测试开始 ==========");

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.user(prompt)))
                .thinking(true)
                .build();

        StreamCancellationHandle handle = llmService.streamChat(request, new StreamCallback() {
            @Override
            public void onThinking(String content) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("thinking")
                            .data(content));
                } catch (Exception e) {
                    log.warn("发送 thinking SSE 失败: {}", e.getMessage());
                }
            }

            @Override
            public void onContent(String content) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("content")
                            .data(content));
                } catch (Exception e) {
                    log.warn("发送 content SSE 失败: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                log.info("========== 流式 Thinking Chat 测试完成 ==========");
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (Exception e) {
                    // ignore
                }
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式 Thinking Chat 测试失败: {}", error.getMessage(), error);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "error", error.getMessage(),
                                    "errorType", error.getClass().getSimpleName(),
                                    "diagnosis", diagnoseError(error)
                            )));
                } catch (Exception e) {
                    // ignore
                }
                emitter.completeWithError(error);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SseEmitter 超时，取消 thinking 流式请求");
            handle.cancel();
        });

        emitter.onError(e -> {
            log.warn("SseEmitter 错误，取消 thinking 流式请求");
            handle.cancel();
        });

        return emitter;
    }

    // ==================== 5. Embedding 测试 ====================

    /**
     * 测试5：Embedding 向量化
     */
    @PostMapping("/embedding")
    public Result<?> testEmbedding(@RequestParam(defaultValue = "Hello world") String text) {
        if (embeddingService == null) {
            return Result.error("EmbeddingService Bean 未注入");
        }
        long start = System.currentTimeMillis();
        try {
            List<Float> vector = embeddingService.embed(text);
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "text", text,
                    "vectorSize", vector.size(),
                    "vectorPreview", vector.subList(0, Math.min(5, vector.size())),
                    "costMs", cost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "costMs", cost,
                    "diagnosis", diagnoseError(e)
            ));
        }
    }

    /**
     * 测试5b：批量 Embedding
     */
    @PostMapping("/embedding/batch")
    public Result<?> testBatchEmbedding(@RequestBody List<String> texts) {
        if (embeddingService == null) {
            return Result.error("EmbeddingService Bean 未注入");
        }
        long start = System.currentTimeMillis();
        try {
            List<List<Float>> vectors = embeddingService.embedBatch(texts);
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "inputCount", texts.size(),
                    "outputCount", vectors.size(),
                    "vectorSizes", vectors.stream().map(List::size).toList(),
                    "costMs", cost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "costMs", cost
            ));
        }
    }

    // ==================== 6. Rerank 测试 ====================

    /**
     * 测试6：Rerank 重排序
     */
    @PostMapping("/rerank")
    public Result<?> testRerank(@RequestParam String query,
                                 @RequestBody List<String> candidates,
                                 @RequestParam(defaultValue = "3") int topN) {
        if (rerankService == null) {
            return Result.error("RerankService Bean 未注入");
        }
        long start = System.currentTimeMillis();
        try {
            List<RetrievedChunk> chunks = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                chunks.add(new RetrievedChunk(i, candidates.get(i), 0f));
            }
            List<RetrievedChunk> result = rerankService.rerank(query, chunks, topN);
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "query", query,
                    "inputCount", candidates.size(),
                    "outputCount", result.size(),
                    "results", result.stream().map(RetrievedChunk::getText).toList(),
                    "costMs", cost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.success(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "costMs", cost
            ));
        }
    }

    // ==================== 7. 配置信息查看 ====================

    /**
     * 测试7：查看当前 AI 配置
     */
    @GetMapping("/config")
    public Result<?> testShowConfig() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("providers", aiModelProperties.getProviders().keySet());
        info.put("chatGroup", Map.of(
                "defaultModel", aiModelProperties.getChat().getDefaultModel(),
                "deepThinkingModel", aiModelProperties.getChat().getDeepThinkingModel(),
                "candidateCount", aiModelProperties.getChat().getCandidates().size()
        ));
        info.put("embeddingGroup", Map.of(
                "defaultModel", aiModelProperties.getEmbedding().getDefaultModel(),
                "candidateCount", aiModelProperties.getEmbedding().getCandidates().size()
        ));
        info.put("rerankGroup", Map.of(
                "defaultModel", aiModelProperties.getRerank().getDefaultModel(),
                "candidateCount", aiModelProperties.getRerank().getCandidates().size()
        ));
        info.put("selection", Map.of(
                "failureThreshold", aiModelProperties.getSelection().getFailureThreshold(),
                "openDurationMs", aiModelProperties.getSelection().getOpenDurationMs()
        ));
        return Result.success(info);
    }

    // ==================== 8. 一键全量测试 ====================

    /**
     * 测试8：一键运行所有诊断测试
     */
    @GetMapping("/diagnose")
    public Result<?> testFullDiagnosis() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 1. 配置检查
        report.put("step1_config", Map.of(
                "chatCandidates", aiModelProperties.getChat().getCandidates().size(),
                "embeddingCandidates", aiModelProperties.getEmbedding().getCandidates().size(),
                "rerankCandidates", aiModelProperties.getRerank().getCandidates().size()
        ));

        // 2. 模型选择检查
        List<ModelTarget> chatTargets = modelSelector.selectChatModel(false);
        List<ModelTarget> embedTargets = modelSelector.selectEmbeddingCandidates();
        List<ModelTarget> rerankTargets = modelSelector.selectRerankCandidates();

        report.put("step2_modelSelection", Map.of(
                "chatTargets", chatTargets.size(),
                "embeddingTargets", embedTargets.size(),
                "rerankTargets", rerankTargets.size(),
                "chatEmpty", chatTargets.isEmpty(),
                "F1_detected", chatTargets.isEmpty()
        ));

        // 3. Bean 注入检查
        report.put("step3_beans", Map.of(
                "LLMService", llmService != null ? llmService.getClass().getSimpleName() : "NULL",
                "EmbeddingService", embeddingService != null ? embeddingService.getClass().getSimpleName() : "NULL",
                "RerankService", rerankService != null ? rerankService.getClass().getSimpleName() : "NULL"
        ));

        // 4. 诊断结论
        List<String> issues = new ArrayList<>();
        if (chatTargets.isEmpty()) {
            issues.add("F-1: filter(Objects::isNull) 导致模型选择返回空");
        }
        if (llmService == null) {
            issues.add("LLMService Bean 未注册");
        }
        if (embeddingService == null) {
            issues.add("EmbeddingService Bean 未注册");
        }
        if (rerankService == null) {
            issues.add("RerankService Bean 未注册");
        }
        report.put("step4_diagnosis", issues.isEmpty() ? "未发现明显问题" : issues);
        report.put("step5_nextStep", issues.isEmpty()
                ? "可以尝试 POST /agent-test/chat/sync 测试实际调用"
                : "请先修复上述问题再测试实际调用");

        return Result.success(report);
    }

    // ==================== 辅助方法 ====================

    private String diagnoseError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) msg = "";
        if (msg.contains("targets is empty")) {
            return "致命缺陷F-1：模型选择返回空列表，filter(Objects::isNull) 方向写反";
        }
        if (msg.contains("大模型调用失败") || msg.contains("流式首包超时")) {
            return "可能致命缺陷F-2：流式调用探针未传递，导致永远超时";
        }
        if (msg.contains("API密钥缺失")) {
            return "配置问题：API Key 未配置";
        }
        if (msg.contains("提供商配置缺失") || msg.contains("Provider配置缺失")) {
            return "配置问题：Provider 配置缺失";
        }
        if (msg.contains("Provider baseUrl 不存在") || msg.contains("Provider endpoint 不存在")) {
            return "配置问题：Provider URL 或 Endpoint 缺失";
        }
        if (msg.contains("NullPointerException")) {
            return "可能缺陷M-2：getThinking() 返回 null 导致 NPE";
        }
        return "未知错误，请查看日志";
    }
}
