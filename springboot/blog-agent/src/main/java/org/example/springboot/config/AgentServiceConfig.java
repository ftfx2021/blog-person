package org.example.springboot.config;

import org.example.springboot.chat.ChatClient;
import org.example.springboot.chat.LLMService;
import org.example.springboot.chat.RoutingLLMService;
import org.example.springboot.embedding.EmbeddingClient;
import org.example.springboot.embedding.EmbeddingService;
import org.example.springboot.embedding.RoutingEmbeddingService;
import org.example.springboot.model.ModelHealthStore;
import org.example.springboot.model.ModelRoutingExecutor;
import org.example.springboot.model.ModelSelector;
import org.example.springboot.rerank.RerankClient;
import org.example.springboot.rerank.RerankService;
import org.example.springboot.rerank.RoutingRerankService;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class AgentServiceConfig {

    /**
     * 流式 HTTP 客户端（Primary）
     */
    @Bean
    @Primary
    public OkHttpClient streamingHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ZERO)
                .callTimeout(Duration.ZERO)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 同步 HTTP 客户端
     */
    @Bean
    public OkHttpClient syncHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(45))
                .retryOnConnectionFailure(true)
                .build();
    }
}
