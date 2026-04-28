package org.example.springboot.listener.rabbitmq;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RabbitListenerLazyStarter {
    @Resource
    private RabbitListenerEndpointRegistry registry;

    @EventListener(ApplicationReadyEvent.class)
    public void startLater() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(10000); // 延迟10秒，可配置化
                registry.start();
                log.info("Rabbit 监听器已延后启动");
            } catch (Exception e) {
                log.error("Rabbit 监听器启动失败", e);
            }
        }) ;

    }
}
