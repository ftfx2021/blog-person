package org.example.springboot.controller;


import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.UTF8;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;

@Slf4j
@RestController()
@CrossOrigin
@RequestMapping("mytest")
public class LearnTestController {

    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    //SSE测试

    @GetMapping(value = "/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> test(){
        return chatClient.prompt("你是一个百科全书")
                .user("你好，详细介绍一下非物质文化遗产")
                .stream()
                .content().map(
                        chunk->{

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("type","message");
                            jsonObject.put("content",chunk);
                            return jsonObject.toString();

                        }
                );



    }
    @GetMapping(value = "/sse2",produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<Map<String, Object>>> test2(){
        return chatClient.prompt("你是一个百科全书")
                .user("你好，详细介绍一下非物质文化遗产")
                .stream()
                .content().map(
                        chunk->{
                            return      ServerSentEvent.<Map<String,Object>>builder()
                                    .event("message")
                                    .data(Map.of("message",chunk))
                                    .build();
//                            JSONObject jsonObject = new JSONObject();
//                            jsonObject.put("type","message");
//                            jsonObject.put("content",chunk);
//                            return jsonObject.toString();

                        }
                );



    }
}
