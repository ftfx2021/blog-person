package org.example.springboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;

@Configuration
public class MyESClientConfig extends ElasticsearchConfiguration {
    @Value("${spring.elasticsearch.host:localhost}")
    private String host;

    @Value("${spring.elasticsearch.port:9200}")
    private int port;

    @Value("${spring.elasticsearch.connection-timeout:1}")
    private int connectTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30}")
    private int socketTimeout;
    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(host, String.valueOf(port))
//                .connectedTo("localhost:9200")  //亦可直接使用url
                .withConnectTimeout(connectTimeout* 1000L)
                .withSocketTimeout(socketTimeout* 1000L)

//                .withBasicAuth("username", "password") //如果有
                .build();
    }

    /**
     * 配置字段命名策略(重写爷爷类的方法)
     * 将 Java 驼峰命名 (userName) 转换为 ES 蛇形命名 (user_name)
     */
//    @Override
//    protected FieldNamingStrategy fieldNamingStrategy() {
//        return new SnakeCaseFieldNamingStrategy();
//    }
}