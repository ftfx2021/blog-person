package org.example.springboot.AiService.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.config.rabbitmq.ArticleRabbitMqConfig;
import org.example.springboot.dto.VectorMetadataDTO;
import org.example.springboot.dto.KnowledgeBaseStatsDTO;
import org.example.springboot.entity.Article;
import org.example.springboot.enums.ArticleVectorizedStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ArticleMapper;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 个人知识库服务
 * 读取resources下所有md文件，存储到Milvus，提供问答功能
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    @Autowired
    @Lazy
    private VectorStore vectorStore;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private List<McpSyncClient> mcpSyncClients;
//    @Autowired
//    private List<McpAsyncClient> mcpAsyncClients;

    @Value("${knowledge.auto-load:true}")
    private boolean autoLoad;
    
    @Value("${knowledge.startup-delay-seconds:10}")
    private int startupDelaySeconds;

    private volatile boolean isInitialized = false;
    private final AtomicBoolean milvusCollectionReady = new AtomicBoolean(false);



    private static final String METADATA_ARTICLE_ID = "articleId";
    private static final String METADATA_TITLE = "title";
    private static final String METADATA_CATEGORY_ID = "categoryId";
    private static final String METADATA_TOTAL_CHUNKS = "totalChunks";
    private static final String METADATA_CHUNK_INDEX = "chunkIndex";
    private static final String METADATA_CHUNK_ID = "chunkId";






    /**
     * 系统启动完成后异步加载知识库（确保所有依赖都已初始化）
     * 使用 ApplicationReadyEvent 确保：
     * 1. Spring 容器完全启动
     * 2. 所有 Bean 都已初始化（包括 RabbitMQ、Milvus）
     * 3. 数据库连接池已就绪
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoLoad && !isInitialized) {
            log.info("========== 应用启动完成，知识库将在后台异步加载数据库文章 ==========");
            CompletableFuture.runAsync(() -> {
                try {
                    // 延迟启动，确保所有外部依赖（RabbitMQ、Milvus）完全就绪
                    if (startupDelaySeconds > 0) {
                        log.info("等待 {} 秒以确保所有依赖服务就绪...", startupDelaySeconds);
                        Thread.sleep(startupDelaySeconds * 1000L);
                    }
                    
                    // 验证依赖服务是否可用
                    if (!checkDependenciesReady()) {
                        log.error("依赖服务未就绪，跳过知识库自动加载");
                        return;
                    }

                    // 若集合不存在则自动创建，避免首次写入时报错
                    ensureMilvusCollectionExists(false);
                    
                    // 标记为已初始化
                    isInitialized = true;
                    
                    // 启动时使用增量加载，只加载未向量化的文章
                    // loadArticlesFromDatabase(true);
                    log.info("知识库初始化完成，已准备好处理向量化请求");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("知识库异步加载被中断: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("知识库异步加载失败: {}", e.getMessage(), e);
                }
            });
        } else if (!autoLoad) {
            log.info("知识库自动加载已禁用，请调用 POST /knowledge/load 手动加载");
            isInitialized = true; // 即使不自动加载，也标记为已初始化
        }
    }
    
    /**
     * 检查依赖服务是否就绪
     * @return true 如果所有依赖都就绪
     */
    private boolean checkDependenciesReady() {
        boolean allReady = true;
        
        // 1. 检查 VectorStore (Milvus)
        try {
            if (vectorStore == null) {
                log.error("VectorStore (Milvus) 未初始化");
                allReady = false;
            } else {
                log.info("✓ VectorStore (Milvus) 已就绪");
            }
        } catch (Exception e) {
            log.error("VectorStore (Milvus) 检查失败: {}", e.getMessage());
            allReady = false;
        }
        
        // 2. 检查 RabbitTemplate (RabbitMQ)
        try {
            if (rabbitTemplate == null) {
                log.error("RabbitTemplate (RabbitMQ) 未初始化");
                allReady = false;
            } else {
                // 尝试获取连接工厂来验证连接
                rabbitTemplate.getConnectionFactory().createConnection();
                log.info("✓ RabbitMQ 连接已就绪");
            }
        } catch (Exception e) {
            log.error("RabbitMQ 连接检查失败: {}", e.getMessage());
            allReady = false;
        }
        
        // 3. 检查数据库连接
        try {
            if (articleMapper == null) {
                log.error("ArticleMapper (数据库) 未初始化");
                allReady = false;
            } else {
                // 执行一个简单的查询来验证数据库连接
                articleMapper.selectCount(new LambdaQueryWrapper<Article>().last("LIMIT 1"));
                log.info("✓ 数据库连接已就绪");
            }
        } catch (Exception e) {
            log.error("数据库连接检查失败: {}", e.getMessage());
            allReady = false;
        }
        
        return allReady;
    }
    
    /**
     * 检查服务是否已初始化
     * @return true 如果服务已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }


    /**
     * 从数据库加载已发布的文章到向量数据库（全量加载）
     */
    public void loadArticlesFromDatabase() {
         loadArticlesFromDatabase(false);
    }
    
    /**
     * 从数据库加载已发布的文章到向量数据库
     * @param incrementalOnly 是否只加载未向量化的文章（增量加载）
     */
    public void loadArticlesFromDatabase(boolean incrementalOnly) {
        log.info("开始从数据库{}加载文章到知识库...", incrementalOnly ? "增量" : "全量");
        
        try {
            // 查询已发布的文章（status=1）
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getStatus, 1);  // 已发布
            
            // 增量模式：只加载未向量化/向量化失败的文章
            if (incrementalOnly) {
                queryWrapper.and(w -> w.isNull(Article::getVectorizedStatus)
                        .or().ne(Article::getVectorizedStatus, ArticleVectorizedStatus.SUCCESS));
            }
            
            queryWrapper.orderByDesc(Article::getCreateTime);
            
            List<Article> articles = articleMapper.selectList(queryWrapper);
            
            if (articles.isEmpty()) {
                log.warn("数据库中没有已发布的文章");
                return;
            }
            
            log.info("从数据库查询到 {} 篇已发布文章", articles.size());
            
            for (Article article : articles) {
               log.info("逐个将文章加入向量化队列");
               rabbitTemplate.convertAndSend(
                       ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,
                       ArticleRabbitMqConfig.ARTICLE_VECTORIZED_ROUTING_KEY,
                       article);
            }
            

            
        } catch (Exception e) {
            log.error("从数据库加载文章失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载知识库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从HTML内容中提取纯文本
     */
    private String extractTextFromHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        try {
            return Jsoup.parse(html).text();
        } catch (Exception e) {
            log.warn("解析HTML失败: {}", e.getMessage());
            return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        }
    }
    
    /**
     * 重新加载知识库（供手动调用）
     */
    public void loadAllMarkdownFiles() {
         loadArticlesFromDatabase();
    }

    /**
     * 在知识库中搜索相关文档
     */
    public List<Document> search(String question, int topK) {
        log.info("搜索知识库，问题: {}", question);

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .build();

        List<Document> results = executeWithCollectionAutoCreate(
                () -> vectorStore.similaritySearch(request),
                "similaritySearch"
        );
        if (results != null) {
            log.info("找到 {} 个相关文档", results.size());
        }

        return results;
    }

    /**
     * 基于知识库进行问答（会自动搜索文档）
     */
    public String ask(String question) {
        return ask(question, 5);
    }
    
    /**
     * 基于知识库进行问答（指定topK）
     */
    public String ask(String question, int topK) {
        List<Document> relevantDocs = search(question, topK);
        return askWithDocs(question, relevantDocs);
    }
    
    /**
     * 基于已检索的文档进行问答（避免重复搜索）
     */
    public String askWithDocs(String question, List<Document> relevantDocs) {
        log.info("知识库问答，问题: {}", question);
        
        if (relevantDocs == null || relevantDocs.isEmpty()) {
            return "抱歉，在知识库中没有找到相关信息。";
        }
        
        String prompt = buildPrompt(question, relevantDocs);


        String answer = chatClient.prompt()
                .user(prompt)
                .tools()
                .call()
                .content();
        
        log.info("问答完成");
        return answer;
    }
    
    /**
     * 流式问答（基于已检索的文档）
     */
    public reactor.core.publisher.Flux<String> streamAskWithDocs(String question, List<Document> relevantDocs) {
        log.info("知识库流式问答，问题: {}", question);
        
        if (relevantDocs == null || relevantDocs.isEmpty()) {
            return reactor.core.publisher.Flux.just("抱歉，在知识库中没有找到相关信息。");
        }
        
        String prompt = buildPrompt(question, relevantDocs);
        
        // 按客户端名称去重，避免同一个 MCP 服务器的工具被注册多次
        Map<String, McpSyncClient> uniqueClients = new LinkedHashMap<>();
        for (McpSyncClient client : mcpSyncClients) {
            String clientName = client.getClientInfo().name();
            if (!uniqueClients.containsKey(clientName)) {
                uniqueClients.put(clientName, client);
                log.info("注册 MCP 客户端: {}", clientName);
            } else {
                log.warn("跳过重复的 MCP 客户端: {}", clientName);
            }
        }
        
        List<McpSyncClient> deduplicatedClients = new ArrayList<>(uniqueClients.values());
        log.info("去重后 MCP 客户端数量: {}", deduplicatedClients.size());
        
        SyncMcpToolCallbackProvider syncMcpToolCallbackProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(deduplicatedClients)
                .build();

        // 流式调用AI生成答案
        return chatClient.prompt()
                .user(prompt)
                .toolCallbacks(syncMcpToolCallbackProvider.getToolCallbacks())
                .stream()
                .content();
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String question, List<Document> relevantDocs) {
        String context = relevantDocs.stream()
                .map(doc -> {
                    String source = doc.getMetadata().getOrDefault("source", "未知").toString();
                    return "【来源: " + source + "】\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));
        
        return String.format(
                """
                        你是一个全能助手。请根据知识库/Mcp/Tool回答用户的问题。
                        如果mcp调用错误，请告诉用户为什么错误。
                        回答时请引用信息来源。
                        
                        知识库内容：
                        %s
                        
                        用户问题：%s
                        
                        请用中文回答：""",
            context,
            question
        );
    }

    /**
     * 重置文章的向量化状态（文章更新时调用）
     * @param articleId 文章ID
     */
    public void resetVectorizedStatus(String articleId) {
        log.info("重置文章向量化状态: {}", articleId);
        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            article.setVectorizedStatus(ArticleVectorizedStatus.NO.getCode());
            article.setVectorizedErrorReason(null);
            article.setVectorizedProcessAt(null);
            article.setLastVectorizedSuccessTime(null);
            articleMapper.updateById(article);
        }
    }
    
    /**
     * 批量重置文章的向量化状态
     * @param articleIds 文章ID列表
     */
    public void resetVectorizedStatus(List<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) return;
        log.info("批量重置文章向量化状态: {} 篇", articleIds.size());
        for (String articleId : articleIds) {
            resetVectorizedStatus(articleId);
        }
    }


    
    /**
     * 向量化单篇文章
     * @param articleId 文章ID
     * @return 生成的文档块数量
     */
    public int syncArticle(String articleId) {
        log.info("开始向量化单篇文章: {}", articleId);

        // 1. 获取文章 (为了读取内容)
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            log.error("文章不存在: {}", articleId);
            return 0; // 或者抛出异常，视业务而定
        }

        // 2. 检查业务状态
        // 如果文章不是发布状态(1)，则不应该向量化。
        // 甚至应该尝试删除向量库里的旧数据（防止用户撤回发布后还能搜到）
        if (!Integer.valueOf(1).equals(article.getStatus())) {
            log.warn("文章状态非发布状态 [{}], 跳过并清理向量库", article.getStatus());
            // [关键] 确保向量库里没有脏数据
            deleteVectorByArticleId(articleId);
            return 0;
        }

        if(article.getVectorizedStatus().equals(ArticleVectorizedStatus.SUCCESS.getCode())||article.getVectorizedStatus().equals(ArticleVectorizedStatus.ON_GOING.getCode())){
            log.warn("文章{}已经向量化或正在向量化，跳过", article.getId());
            return 0;
        }

        // 3. 标记 "处理中" (使用局部更新，防止覆盖用户修改的内容)
        // SQL: UPDATE articles SET vectorized_status=1, vectorized_process_at=NOW(), vectorized_retry_count=retry_count+1 WHERE id=...
        boolean updateStart = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .set(Article::getVectorizedStatus, ArticleVectorizedStatus.ON_GOING.getCode())
                .set(Article::getVectorizedProcessAt, LocalDateTime.now())
                // 乐观锁或重试计数：这里假设是重试计数，直接+1
                .setSql("vectorized_retry_count = vectorized_retry_count + 1")
        ) > 0;

        if (!updateStart) {
            log.warn("更新文章状态失败，可能文章已被删除");
            return 0;
        }

        try {
            // 4. 准备内容 (修复了之前的逻辑覆盖问题)
            String contentRaw = article.getContent(); // 优先取 Markdown
            if (contentRaw == null || contentRaw.trim().isEmpty()) {
                // Markdown 为空才取 HTML
                contentRaw = extractTextFromHtml(article.getHtmlContent());
            }

            if (contentRaw == null || contentRaw.trim().isEmpty()) {
                throw new RuntimeException("文章内容(Markdown/HTML)均为空");
            }

            // 5. 构建文档内容
            StringBuilder docContent = new StringBuilder();
            docContent.append("标题：").append(article.getTitle()).append("\n\n");
            if (article.getSummary() != null && !article.getSummary().trim().isEmpty()) {
                docContent.append("摘要：").append(article.getSummary()).append("\n\n");
            }
            docContent.append("正文：\n").append(contentRaw);

            // 6. 准备 Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(METADATA_ARTICLE_ID, article.getId()); // [关键] 用于后续删除
            metadata.put(METADATA_TITLE, article.getTitle());  // 存标题方便反查
            metadata.put(METADATA_CATEGORY_ID, article.getCategoryId());

            // 7. 分块 (Split)
            Document doc = new Document(docContent.toString(), metadata);
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(List.of(doc));

            if (splitDocs == null || splitDocs.isEmpty()) {
                // 方案 A：抛出异常（阻断流程，适合事务回滚）
                articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, articleId)
                        .set(Article::getVectorizedStatus, ArticleVectorizedStatus.ERROR.getCode())
                        // 截取错误信息防止数据库字段超长
                        .set(Article::getVectorizedErrorReason,"文章内容为空或无法生成有效分片，操作终止")

                );

                return 0;

                // 方案 B：返回特定错误码（如果你的方法有返回值）
                // return Result.fail("内容为空");
            }

            for(int i = 0; i < splitDocs.size(); i++) {
                Document document = splitDocs.get(i);
                document.getMetadata().putIfAbsent(METADATA_TOTAL_CHUNKS, splitDocs.size());
                document.getMetadata().putIfAbsent(METADATA_CHUNK_INDEX, i);
                document.getMetadata().putIfAbsent(METADATA_CHUNK_ID, document.getId());
            }


            // 8. [关键步骤] 幂等性处理：先删后写
            // 必须确保删除该文章ID下的所有旧切片，防止数据重复叠加
            deleteVectorByArticleId(articleId);

            // 9. 写入向量库
            log.info("文章 [{}] 分块完成，共 {} 个块，准备写入向量库", article.getTitle(), splitDocs.size());
            executeWithCollectionAutoCreate(() -> {
                vectorStore.add(splitDocs);
                return null;
            }, "addDocuments");

            // 10. 标记 "成功" (局部更新)
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .set(Article::getVectorizedStatus, ArticleVectorizedStatus.SUCCESS.getCode())
                    .set(Article::getLastVectorizedSuccessTime, LocalDateTime.now())
                    .set(Article::getVectorizedErrorReason, "") // 清空之前的错误信息
            );

            log.info("文章 [{}] 向量化流程结束", article.getTitle());
            return splitDocs.size();

        } catch (Exception e) {
            log.error("向量化文章失败 id={}: {}", articleId, e.getMessage(), e);

            // 11. 标记 "失败" (局部更新)
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .set(Article::getVectorizedStatus, ArticleVectorizedStatus.ERROR.getCode())
                    // 截取错误信息防止数据库字段超长
                    .set(Article::getVectorizedErrorReason,
                            e.getMessage() != null && e.getMessage().length() > 500 ?
                                    e.getMessage().substring(0, 500) : e.getMessage())
            );

            return 0;
        }
    }

    /**
     * 辅助方法：从向量库中删除指定文章的所有切片
     * 具体实现取决于你使用的 VectorStore 类型 (ES/Milvus/Pinecone)
     */
    public void deleteVectorByArticleId(String articleId) {
        try {

            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq("articleId", articleId)
                    .build();
            executeWithCollectionAutoCreate(() -> {
                vectorStore.delete(expression);
                return null;
            }, "deleteByArticleId");
        } catch (Exception e) {
            log.error("尝试清理旧向量失败，虽然不影响写入，但会产生垃圾数据: {}", e.getMessage());
        }
    }

    /**
     * 执行向量库操作，若集合不存在则自动创建并重试一次
     */
    private <T> T executeWithCollectionAutoCreate(MilvusOperation<T> operation, String operationName) {
        ensureMilvusCollectionExists(false);
        try {
            return operation.execute();
        } catch (Exception e) {
            if (!isCollectionNotFoundException(e)) {
                throw asRuntimeException(e);
            }
            log.warn("Milvus集合不存在，准备自动创建并重试。operation={}, error={}", operationName, resolveRootMessage(e));
            ensureMilvusCollectionExists(true);
            try {
                return operation.execute();
            } catch (Exception retryException) {
                throw asRuntimeException(retryException);
            }
        }
    }

    /**
     * 确保 Milvus 集合存在：不存在时自动创建
     */
    private void ensureMilvusCollectionExists(boolean force) {
        if (!force && milvusCollectionReady.get()) {
            return;
        }
        synchronized (milvusCollectionReady) {
            if (!force && milvusCollectionReady.get()) {
                return;
            }
            if (!(vectorStore instanceof MilvusVectorStore milvusVectorStore)) {
                milvusCollectionReady.set(true);
                return;
            }
            try {
                Method createCollectionMethod = MilvusVectorStore.class.getDeclaredMethod("createCollection");
                createCollectionMethod.setAccessible(true);
                createCollectionMethod.invoke(milvusVectorStore);
                milvusCollectionReady.set(true);
                log.info("Milvus集合检查完成（不存在时已自动创建）");
            } catch (Exception e) {
                milvusCollectionReady.set(false);
                if (force) {
                    throw asRuntimeException(e);
                }
                log.warn("Milvus集合检查失败，后续将按需重试自动创建: {}", resolveRootMessage(e));
            }
        }
    }

    private boolean isCollectionNotFoundException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("can't find collection")
                        || lower.contains("collection not found")
                        || lower.contains("cannot find collection")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private RuntimeException asRuntimeException(Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(e);
    }

    private String resolveRootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = throwable == null ? "" : throwable.getMessage();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message == null ? "" : message;
    }

    @FunctionalInterface
    private interface MilvusOperation<T> {
        T execute() throws Exception;
    }

    private Map<String, Object> getArticleMetaDataByArticleId(String articleId) {
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("articleId", articleId)
                .build();
        SearchRequest request = SearchRequest.builder()
                .filterExpression(expression).query("").topK(1).build();
        List<Document> documents = vectorStore.similaritySearch(request);
        if(documents == null || documents.isEmpty()) return null;
        Document document = documents.get(0);

        //元数据
       return document.getMetadata();

    }

    /**
     * 根据文章id和chunkIndex获取分片内容
     * @param articleId
     * @param chunkIndex
     */
    private Document getByChunkIndex(String articleId,int chunkIndex) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression build = b.and(
                b.eq(METADATA_ARTICLE_ID, articleId),
                b.eq(METADATA_CHUNK_INDEX, chunkIndex)
        ).build();
        SearchRequest request = SearchRequest.builder().filterExpression(build).build();
        List<Document> documents = vectorStore.similaritySearch(request);
        if(documents == null || documents.isEmpty()) return null;
       return documents.get(0);


    }

    /**
     * 根据chunkId获取分片内容
     * @param chunkId
     */
    private Document getByChunkId(int chunkId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq(METADATA_CHUNK_ID, chunkId).build();
        SearchRequest request = SearchRequest.builder().filterExpression(expression).build();
        List<Document> documents = vectorStore.similaritySearch(request);
        if(documents == null || documents.isEmpty()) return null;
        return documents.get(0);


    }




    /**
     * 获取文章的向量元数据
     * @param articleId 文章ID
     * @return 向量元数据列表，按chunk_index排序
     * @throws ServiceException 当Milvus连接失败时
     */
    public List<VectorMetadataDTO> getArticleVectorMetadata(String articleId) {
        log.info("获取文章向量元数据: {}", articleId);
        
        try {
            // 使用FilterExpressionBuilder查询指定文章的所有向量块
            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq(METADATA_ARTICLE_ID, articleId)
                    .build();
            
            SearchRequest request = SearchRequest.builder()
                    .filterExpression(expression)
                    .query("") // 空查询，只用于过滤
                    .topK(1000) // 设置足够大的值以获取所有块
                    .build();
            
            List<Document> documents = vectorStore.similaritySearch(request);
            
            // 如果没有找到向量数据，返回空列表
            if (documents == null || documents.isEmpty()) {
                log.info("文章 {} 没有向量数据", articleId);
                return new ArrayList<>();
            }
            
            log.info("找到 {} 个向量块", documents.size());
            
            // 转换为VectorMetadataDTO列表
            List<VectorMetadataDTO> metadataList = documents.stream()
                    .map(doc -> {
                        VectorMetadataDTO dto = new VectorMetadataDTO();
                        Map<String, Object> metadata = doc.getMetadata();
                        
                        // 提取元数据字段
                        dto.setArticleId(metadata.get(METADATA_ARTICLE_ID)==null?null:metadata.get(METADATA_ARTICLE_ID).toString());
                        dto.setTitle(metadata.get(METADATA_TITLE)==null?null:metadata.get(METADATA_TITLE).toString());
                        dto.setCategoryId(metadata.get(METADATA_CATEGORY_ID)==null?null:((Double)metadata.get(METADATA_CATEGORY_ID)).longValue());
                        dto.setTotalChunks(metadata.get(METADATA_TOTAL_CHUNKS)==null?null:((Double)metadata.get( METADATA_TOTAL_CHUNKS)).longValue());
                        dto.setChunkIndex(metadata.get(METADATA_CHUNK_INDEX)==null?null: ((Double)metadata.get( METADATA_CHUNK_INDEX)).longValue());
                        dto.setChunkId(metadata.get(METADATA_CHUNK_ID)==null?null:metadata.get( METADATA_CHUNK_ID).toString());
                        
                        // 生成内容预览（前100个字符）
                        String content = doc.getText();
                        if (content != null && content.length() > 100) {
                            dto.setContentPreview(content.substring(0, 100) + "...");
                        } else {
                            dto.setContentPreview(content);
                        }
                        
                        return dto;
                    })
                    .sorted(Comparator.comparing(VectorMetadataDTO::getChunkIndex, 
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            
            return metadataList;
            
        } catch (Exception e) {
            log.error("获取文章向量元数据失败: {}", e.getMessage(), e);
            throw new ServiceException("503", "向量数据库暂时不可用: " + e.getMessage());
        }
    }
    

    


    /**
     * 获取知识库统计信息（用于文章向量化监控）
     * @return 知识库统计DTO，包含各状态文章数量和文章列表
     */
    public KnowledgeBaseStatsDTO getStats() {
        log.info("开始获取知识库统计信息");
        
        try {
            KnowledgeBaseStatsDTO statsDTO = new KnowledgeBaseStatsDTO();
            
            // 查询所有已发布的文章（status=1）
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getStatus, 1);
            List<Article> publishedArticles = articleMapper.selectList(queryWrapper);
            
            // 计算总发布文章数
            statsDTO.setTotalPublishedArticles(publishedArticles.size());
            
            // 按向量化状态分类统计
            int successCount = 0;
            int inProgressCount = 0;
            int failedCount = 0;
            int pendingCount = 0;
            
            List<KnowledgeBaseStatsDTO.ArticleVectorizationStatus> articleStatuses = new ArrayList<>();
            
            for (Article article : publishedArticles) {
                Integer vectorizedStatus = article.getVectorizedStatus();
                
                // 统计各状态数量
                if (vectorizedStatus == null || vectorizedStatus == 0) {
                    // NULL 或 0 都算作待向量化
                    pendingCount++;
                } else if (vectorizedStatus == 1) {
                    // 处理中
                    inProgressCount++;
                } else if (vectorizedStatus == 2) {
                    // 成功
                    successCount++;
                } else if (vectorizedStatus == -1) {
                    // 失败
                    failedCount++;
                }
                
                // 构建文章状态对象
                KnowledgeBaseStatsDTO.ArticleVectorizationStatus articleStatus = 
                        new KnowledgeBaseStatsDTO.ArticleVectorizationStatus();
                articleStatus.setArticleId(article.getId());
                articleStatus.setTitle(article.getTitle());
                articleStatus.setVectorizedStatus(vectorizedStatus);
                articleStatus.setVectorizedErrorReason(article.getVectorizedErrorReason());
                articleStatus.setLastVectorizedSuccessTime(article.getLastVectorizedSuccessTime());
                
                articleStatuses.add(articleStatus);
            }
            
            // 设置统计数据
            statsDTO.setSuccessfullyVectorized(successCount);
            statsDTO.setInProgressVectorization(inProgressCount);
            statsDTO.setFailedVectorization(failedCount);
            statsDTO.setPendingVectorization(pendingCount);
            statsDTO.setArticleStatuses(articleStatuses);
            
            log.info("知识库统计完成 - 总数: {}, 成功: {}, 处理中: {}, 失败: {}, 待处理: {}", 
                    statsDTO.getTotalPublishedArticles(),
                    successCount, inProgressCount, failedCount, pendingCount);
            
            return statsDTO;
            
        } catch (Exception e) {
            log.error("获取知识库统计信息失败: {}", e.getMessage(), e);
            throw new ServiceException("500", "统计信息查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取知识库基本状态（旧版本，用于KnowledgeController）
     * @deprecated 使用 getStats() 获取详细统计信息
     */
    @Deprecated
    public Map<String, Object> getBasicStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 查询数据库中已发布文章数量
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getStatus, 1);
            Long totalArticles = articleMapper.selectCount(queryWrapper);

            stats.put("totalFiles", totalArticles.intValue());
            stats.put("loadedArticles", totalArticles.intValue());

            // 获取文章标题列表
            queryWrapper.select(Article::getTitle);
            List<Article> articles = articleMapper.selectList(queryWrapper);
            stats.put("fileNames", articles.stream()
                    .map(Article::getTitle)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return stats;
    }
}
