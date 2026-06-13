package org.example.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.example.springboot.agent.learn.AgentWorkflowEventListener;
import org.example.springboot.agent.learn.LearningAgentWorkflowException;
import org.example.springboot.agent.learn.LearningAgentWorkflowService;
import org.example.springboot.common.Result;
import org.example.springboot.config.rabbitmq.ArticleRabbitMqConfig;
import org.example.springboot.dto.LearnAgentAttachmentDTO;
import org.example.springboot.dto.LearnAgentAskRequest;
import org.example.springboot.dto.LearnAgentAskResponse;
import org.example.springboot.dto.KnowledgeAskDTO;
import org.example.springboot.dto.KnowledgeAskResponseDTO;
import org.example.springboot.dto.KnowledgeStatsDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.enums.ArticleVectorizedStatus;
import org.example.springboot.mapper.ArticleMapper;
import org.springframework.ai.document.Document;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 知识库问答接口
 * 提供基于RAG的知识库问答功能
 */
@Slf4j
@Tag(name = "知识库问答接口")
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private LearningAgentWorkflowService learningAgentWorkflowService;
    
    @Resource
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Operation(summary = "知识库问答")
    @PostMapping("/ask")
    public Result<KnowledgeAskResponseDTO> ask(@RequestBody KnowledgeAskDTO dto) {
        log.info("收到知识库问答请求: {}", dto.getQuestion());
        
        String question = dto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }
        
        try {
            // 获取相关文档
            int topK = dto.getTopK() != null ? dto.getTopK() : 5;
            List<Document> relevantDocs = knowledgeBaseService.search(question, topK);
            
            // 基于已检索的文档生成AI答案（避免重复搜索）
            String answer = knowledgeBaseService.askWithDocs(question, relevantDocs);
            
            // 构建响应
            List<KnowledgeAskResponseDTO.DocumentSource> sources = relevantDocs.stream()
                    .map(doc -> KnowledgeAskResponseDTO.DocumentSource.builder()
                            .source(doc.getMetadata().getOrDefault("source", "未知").toString())
                            .content(truncateContent(doc.getText(), 200))
                            .score(doc.getScore() != null ? doc.getScore() : 0.0)
                            .build())
                    .collect(Collectors.toList());
            
            KnowledgeAskResponseDTO response = KnowledgeAskResponseDTO.builder()
                    .answer(answer)
                    .sources(sources)
                    .question(question)
                    .build();
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("知识库问答失败: {}", e.getMessage(), e);
            return Result.error("问答失败: " + e.getMessage());
        }
    }

    @Operation(summary = "学习版通用智能体问答（同步任务流）")
    @PostMapping("/agent/ask")
    public Result<LearnAgentAskResponse> agentAsk(@RequestBody LearnAgentAskRequest dto) {
        if (dto == null || dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
            return Result.error("400", "问题不能为空");
        }
        String attachmentError = validatePdfAttachments(dto.getAttachments());
        if (attachmentError != null) {
            return Result.error("400", attachmentError);
        }

        try {
            LearnAgentAskResponse response = learningAgentWorkflowService.execute(
                    dto.getQuestion().trim(),
                    dto.getTopK(),
                    dto.getHistory(),
                    dto.getAttachments()
            );
            return Result.success(response);
        } catch (LearningAgentWorkflowException e) {
            log.error("学习版智能体流程执行失败: {}", e.getMessage(), e);
            return Result.error("500", e.getMessage(), e.getPartialResponse());
        } catch (Exception e) {
            log.error("学习版智能体问答失败: {}", e.getMessage(), e);
            return Result.error("500", "学习版智能体执行失败: " + e.getMessage());
        }
    }

    @Operation(summary = "学习版通用智能体问答（流式任务追踪）")
    @PostMapping(value = "/agent/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentAskStream(@RequestBody LearnAgentAskRequest dto, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        if (dto == null || dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
            return Flux.just(
                    toEvent("error", Map.of("message", "问题不能为空")),
                    toEvent("done", null)
            );
        }
        String attachmentError = validatePdfAttachments(dto.getAttachments());
        if (attachmentError != null) {
            return Flux.just(
                    toEvent("error", Map.of("message", attachmentError)),
                    toEvent("done", null)
            );
        }

        return Flux.create(sink -> CompletableFuture.runAsync(() -> executeAgentStream(dto, sink)));
    }

    @Operation(summary = "流式知识库问答（SSE）")
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAsk(@RequestBody KnowledgeAskDTO dto) {
        log.info("收到流式知识库问答请求: {}", dto.getQuestion());
        
        String question = dto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("{\"type\":\"error\",\"data\":\"问题不能为空\"}");
        }
        
        try {
            // 获取相关文档
            int topK = dto.getTopK() != null ? dto.getTopK() : 5;
            List<Document> relevantDocs = knowledgeBaseService.search(question, topK);
            
            // 先发送来源信息（去重，包含articleId）
            Map<String, Map<String, Object>> uniqueSourcesMap = new HashMap<>();
            for (Document doc : relevantDocs) {
                String source = doc.getMetadata().getOrDefault("source", "未知").toString();
                if (!uniqueSourcesMap.containsKey(source)) {
                    Map<String, Object> sourceInfo = new HashMap<>();
                    sourceInfo.put("source", source);
                    sourceInfo.put("articleId", doc.getMetadata().getOrDefault("articleId", "").toString());
                    uniqueSourcesMap.put(source, sourceInfo);
                }
            }
            
            // 逐个发送来源信息（包含articleId用于跳转）
            Flux<String> sourcesFlux = Flux.fromIterable(uniqueSourcesMap.values())
                    .map(sourceInfo -> {
                        try {
                            Map<String, Object> sourceData = new HashMap<>();
                            sourceData.put("type", "source");
                            sourceData.put("data", sourceInfo);
                            return objectMapper.writeValueAsString(sourceData);
                        } catch (Exception e) {
                            return "{\"type\":\"error\",\"data\":\"序列化来源失败\"}";
                        }
                    });
            
            // 流式内容
            Flux<String> contentFlux = knowledgeBaseService.streamAskWithDocs(question, relevantDocs)
                    .map(chunk -> {
                        Map<String, Object> jsonData = new HashMap<>();
                        jsonData.put("type", "content");
                        jsonData.put("data", chunk);
                        try {
                            return objectMapper.writeValueAsString(jsonData);
                        } catch (Exception e) {
                            log.error("JSON序列化失败", e);
                            return "{\"type\":\"error\",\"data\":\"序列化失败\"}";
                        }
                    });
            
            // 结束标记
            Flux<String> doneFlux = Flux.defer(() -> {
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("type", "done");
                doneData.put("data", null);
                try {
                    return Flux.just(objectMapper.writeValueAsString(doneData));
                } catch (Exception e) {
                    return Flux.just("{\"type\":\"done\",\"data\":null}");
                }
            });
            
            return sourcesFlux.concatWith(contentFlux).concatWith(doneFlux)
                    .doOnError(error -> log.error("流式问答发生错误", error));

        } catch (Exception e) {
            log.error("流式知识库问答失败: {}", e.getMessage(), e);

            return Flux.just("{\"type\":\"error\",\"data\":\"" + e.getMessage() + "\"}");
        }
    }

    @Operation(summary = "搜索知识库")
    @GetMapping("/search")
    public Result<List<KnowledgeAskResponseDTO.DocumentSource>> search(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") Integer topK) {
        log.info("搜索知识库: {}", question);
        
        try {
            List<Document> docs = knowledgeBaseService.search(question, topK);
            
            List<KnowledgeAskResponseDTO.DocumentSource> sources = docs.stream()
                    .map(doc -> KnowledgeAskResponseDTO.DocumentSource.builder()
                            .source(doc.getMetadata().getOrDefault("source", "未知").toString())
                            .content(doc.getText())
                            .score(doc.getScore() != null ? doc.getScore() : 0.0)
                            .build())
                    .collect(Collectors.toList());
            
            return Result.success(sources);
            
        } catch (Exception e) {
            log.error("搜索知识库失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "加载知识库")
    @PostMapping("/load")
    public Result<String> loadKnowledge() {
        log.info("手动加载知识库");
        
        try {
            knowledgeBaseService.loadAllMarkdownFiles();
            return Result.success("知识库加载成功");
            
        } catch (Exception e) {
            log.error("加载知识库失败: {}", e.getMessage(), e);
            return Result.error("加载失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "向量化单篇文章")
    @PostMapping("/vectorize/{articleId}")
    public Result<Integer> vectorizeArticle(@PathVariable String articleId) {
        log.info("向量化单篇文章: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
              return   Result.error("文章不存在");
            }
            article.setVectorizedStatus(ArticleVectorizedStatus.NO.getCode());
            article.setVectorizedProcessAt(null);
            article.setLastVectorizedSuccessTime(null);
            article.setVectorizedRetryCount(0);
            article.setVectorizedErrorReason(null);
            articleMapper.updateById(article);
            rabbitTemplate.convertAndSend(ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME, ArticleRabbitMqConfig.ARTICLE_VECTORIZED_ROUTING_KEY, article);
            return Result.success("已加入向量化队列",null);
            
        } catch (Exception e) {
            log.error("向量化文章失败: {}", e.getMessage(), e);
            return Result.error("向量化失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取知识库状态")
    @GetMapping("/stats")
    public Result<KnowledgeStatsDTO> getStats() {
        try {
            Map<String, Object> stats = knowledgeBaseService.getBasicStats();
            
            KnowledgeStatsDTO dto = KnowledgeStatsDTO.builder()
                    .loaded(true) // Simplified - always true if we can query
                    .totalFiles((Integer) stats.getOrDefault("totalFiles", 0))
                    .fileNames((List<String>) stats.get("fileNames"))
                    .error((String) stats.get("error"))
                    .build();
            
            return Result.success(dto);
            
        } catch (Exception e) {
            log.error("获取知识库状态失败: {}", e.getMessage(), e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private void executeAgentStream(LearnAgentAskRequest dto, FluxSink<String> sink) {
        String question = dto.getQuestion().trim();
        AgentWorkflowEventListener listener = new AgentWorkflowEventListener() {
            @Override
            public void onStepStart(StepStartEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("stepCode", event.stepCode());
                data.put("inputSummary", event.inputSummary());
                data.put("stepIndex", event.stepIndex());
                data.put("totalSteps", event.totalSteps());
                sink.next(toEvent("step_start", data));
            }

            @Override
            public void onStepFinish(StepFinishEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("stepCode", event.stepCode());
                data.put("status", event.status());
                data.put("inputSummary", event.inputSummary());
                data.put("outputSummary", event.outputSummary());
                data.put("costMs", event.costMs());
                data.put("errorMessage", event.errorMessage());
                data.put("stepIndex", event.stepIndex());
                data.put("totalSteps", event.totalSteps());
                sink.next(toEvent("step_finish", data));
            }

            @Override
            public void onToolStart(ToolStartEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("toolName", event.toolName());
                data.put("callId", event.callId());
                data.put("argsPreview", event.argsPreview());
                sink.next(toEvent("tool_start", data));
            }

            @Override
            public void onToolFinish(ToolFinishEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("toolName", event.toolName());
                data.put("callId", event.callId());
                data.put("status", event.status());
                data.put("costMs", event.costMs());
                data.put("outputPreview", event.outputPreview());
                data.put("errorMessage", event.errorMessage());
                sink.next(toEvent("tool_finish", data));
            }

            @Override
            public void onSubAgentDecision(SubAgentDecisionEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("subAgentCode", event.subAgentCode());
                data.put("decision", event.decision());
                data.put("reason", event.reason());
                sink.next(toEvent("subagent_decision", data));
            }

            @Override
            public void onSubAgentStart(SubAgentStartEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("subAgentCode", event.subAgentCode());
                sink.next(toEvent("subagent_start", data));
            }

            @Override
            public void onSubAgentFinish(SubAgentFinishEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("subAgentCode", event.subAgentCode());
                data.put("status", event.status());
                data.put("costMs", event.costMs());
                data.put("errorMessage", event.errorMessage());
                sink.next(toEvent("subagent_finish", data));
            }

            @Override
            public void onSubAgentSkip(SubAgentSkipEvent event) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("subAgentCode", event.subAgentCode());
                data.put("reason", event.reason());
                sink.next(toEvent("subagent_skip", data));
            }
        };

        try {
            LearnAgentAskResponse response = learningAgentWorkflowService.execute(
                    question,
                    dto.getTopK(),
                    dto.getHistory(),
                    dto.getAttachments(),
                    listener
            );
            sink.next(toEvent("final", response));
        } catch (LearningAgentWorkflowException e) {
            log.error("学习版智能体流式执行失败: {}", e.getMessage(), e);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", e.getMessage());
            data.put("partialResponse", e.getPartialResponse());
            sink.next(toEvent("error", data));
        } catch (Exception e) {
            log.error("学习版智能体流式执行异常: {}", e.getMessage(), e);
            sink.next(toEvent("error", Map.of("message", "学习版智能体执行失败: " + e.getMessage())));
        } finally {
            sink.next(toEvent("done", null));
            sink.complete();
        }
    }

    private String validatePdfAttachments(List<LearnAgentAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        for (LearnAgentAttachmentDTO attachment : attachments) {
            if (attachment == null) {
                return "附件信息不能为空";
            }
            if (attachment.getFileUrl() == null || attachment.getFileUrl().isBlank()) {
                return "附件 fileUrl 不能为空";
            }
            if (!isPdfAttachment(attachment)) {
                return "仅支持 PDF 附件: " + safeFileName(attachment);
            }
        }
        return null;
    }

    private boolean isPdfAttachment(LearnAgentAttachmentDTO attachment) {
        if (attachment == null) {
            return false;
        }
        if (attachment.getMimeType() != null && attachment.getMimeType().toLowerCase().contains("pdf")) {
            return true;
        }
        if (attachment.getFileName() != null && attachment.getFileName().toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return attachment.getFileUrl() != null && attachment.getFileUrl().toLowerCase().endsWith(".pdf");
    }

    private String safeFileName(LearnAgentAttachmentDTO attachment) {
        if (attachment == null) {
            return "未知文件";
        }
        if (attachment.getFileName() != null && !attachment.getFileName().isBlank()) {
            return attachment.getFileName();
        }
        return attachment.getFileUrl() == null ? "未知文件" : attachment.getFileUrl();
    }

    private String toEvent(String type, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("data", data);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("流式事件序列化失败: type={}, error={}", type, e.getMessage(), e);
            return "{\"type\":\"error\",\"data\":{\"message\":\"事件序列化失败\"}}";
        }
    }
}
