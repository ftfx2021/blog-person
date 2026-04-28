package org.example.springboot.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private  final CopyOnWriteArrayList<WebSocketSession>  sessions = new CopyOnWriteArrayList<>();
    // 1. 客户端建立连接时触发
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("新的客户端链接了，ID:{}",session.getId());
        session.sendMessage(new TextMessage("服务器：连接成功！你可以开始说话了。"));


    }
    // 2. 接收到客户端消息时触发
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientMsg = message.getPayload();
        log.info("客户端来消息：{}",clientMsg);

        String reply = "服务器已经收到你的消息了:"+clientMsg;
        session.sendMessage(new TextMessage(reply));
        for (WebSocketSession webSocketSession : sessions) {
            webSocketSession.sendMessage(new TextMessage(session.getId()+"说："+reply));
        }


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("客户端断开连接！ID: " + session.getId());
    }


}
