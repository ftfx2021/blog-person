package org.example.springboot.listener.rabbitmq;


import lombok.extern.slf4j.Slf4j;
import org.example.springboot.AiService.rag.KnowledgeBaseService;
import org.example.springboot.config.rabbitmq.ArticleRabbitMqConfig;
import org.example.springboot.entity.Article;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleListener {


    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    private static final int MAX_INIT_WAIT_SECONDS = 60; // 最多等待60秒
    private static final int INIT_CHECK_INTERVAL_MS = 1000; // 每秒检查一次

    @RabbitListener(
//            queues = ArticleRabbitMqConfig.ARTICLE_QUEUE_NAME,//方式1：接受该队列所有消息
            bindings =  @QueueBinding( //方式2：接受队列的指定消息
                    value = @Queue(name=ArticleRabbitMqConfig.ARTICLE_QUEUE_NAME),
                    exchange = @Exchange(name = ArticleRabbitMqConfig.ARTICLE_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
                    key = {ArticleRabbitMqConfig.ARTICLE_CREATE_ROUTING_KEY, ArticleRabbitMqConfig.ARTICLE_DELETE_ROUTING_KEY, ArticleRabbitMqConfig.ARTICLE_UPDATE_ROUTING_KEY}

            ) )
    public void processArticle(Article article, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String key) {
        // 等待知识库服务初始化完成
        if (!waitForServiceInitialization()) {
            log.error("知识库服务初始化超时，无法处理文章消息: articleId={}, routingKey={}", 
                     article.getId(), key);
            // 这里可以选择抛出异常让消息重新入队，或者记录到死信队列
            throw new RuntimeException("知识库服务未就绪，消息将重新入队");
        }
        
        if(key.equals(ArticleRabbitMqConfig.ARTICLE_CREATE_ROUTING_KEY)
                ||key.equals(ArticleRabbitMqConfig.ARTICLE_UPDATE_ROUTING_KEY)
        ||key.equals(ArticleRabbitMqConfig.ARTICLE_PUBLISH_ROUTING_KEY)){
            log.info("开始处理创建/更新文章消息：{}",article.getId());
                Integer chunkSize = knowledgeBaseService.syncArticle(article.getId());
                log.info("文章：id={},title={}，向量化结束，创建了{}个分片", article.getId(), article.getTitle(), chunkSize);

        }

        if(key.equals(ArticleRabbitMqConfig.ARTICLE_DELETE_ROUTING_KEY)){
            log.info("开始处理删除文章消息：{}",article.getId());
            knowledgeBaseService.deleteVectorByArticleId(article.getId());
            log.info("删除向量库文章结束：{}",article.getId());
        }

        if (key.equals(ArticleRabbitMqConfig.ARTICLE_VECTORIZED_ROUTING_KEY)){
            log.info("开始处理文章向量化消息：{}",article.getId());
            Integer chunkSize = knowledgeBaseService.syncArticle(article.getId());
            log.info("文章：id={},title={}，向量化结束，创建了{}个分片", article.getId(), article.getTitle(), chunkSize);
        }
    }
    
    /**
     * 等待知识库服务初始化完成
     * @return true 如果服务已初始化，false 如果超时
     */
    private boolean waitForServiceInitialization() {
        if (knowledgeBaseService.isInitialized()) {
            return true;
        }
        
        log.info("知识库服务尚未初始化，等待初始化完成...");
        int waitedSeconds = 0;
        
        while (waitedSeconds < MAX_INIT_WAIT_SECONDS) {
            try {
                Thread.sleep(INIT_CHECK_INTERVAL_MS);
                waitedSeconds++;
                
                if (knowledgeBaseService.isInitialized()) {
                    log.info("知识库服务已初始化，继续处理消息（等待了 {} 秒）", waitedSeconds);
                    return true;
                }
                
                if (waitedSeconds % 5 == 0) {
                    log.info("仍在等待知识库服务初始化... ({}/{} 秒)", waitedSeconds, MAX_INIT_WAIT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待知识库服务初始化被中断");
                return false;
            }
        }
        
        return false;
    }




}
