package org.example.springboot.listener.rabbitmq;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RabbitListenerLazyStarter {
    @Resource
    private RabbitListenerEndpointRegistry registry;

    @Value("${rabbit.listener.lazy-start.enabled:true}")
    private boolean enabled;

    @Value("${rabbit.listener.lazy-start.delay-seconds:10}")
    private long delaySeconds;

    @EventListener(ApplicationReadyEvent.class)
    public void startLater() {
        if (!enabled) {
            log.info("Rabbit 监听器延迟启动已关闭");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delaySeconds * 1000);
                registry.start();
                log.info("Rabbit 监听器已延后启动");
            } catch (Exception e) {
                log.error("Rabbit 监听器启动失败", e);
            }
        }) ;

    }
}
