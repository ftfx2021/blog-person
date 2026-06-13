package org.example.springboot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig  implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandler webSocketHandler;
    //WebSocket 路由 (SimpleUrlHandlerMapping)：
    //WebSocket 的 WebSocketHandlerRegistry 走的是另一套完全独立的映射机制。
    // 它不会去读取你配置的 addPathPrefix。它秉持的是“所见即所得”的原则——你代码里写了什么字符串，它就注册什么路径。
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/api/ws/chat").setAllowedOrigins("*");
    }
}
