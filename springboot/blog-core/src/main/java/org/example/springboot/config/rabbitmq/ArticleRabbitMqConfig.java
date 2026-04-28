package org.example.springboot.config.rabbitmq;



import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArticleRabbitMqConfig {
    /**
     * 采用Topic 模式（主题模式）：向量化不仅仅是“生成向量”，它本质上是 “主数据库（MySQL）与向量数据库的数据同步”。
     * 既然是同步，就必须处理数据的全生命周期：新增、修改、删除。
     */

    //1、覆盖默认序列化工具，使用 JSON
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    //2、定义常量

    public static final String ARTICLE_QUEUE_NAME = "article.queue";
    public static final String ARTICLE_EXCHANGE_NAME = "article.exchange";
    public static final String ARTICLE_CREATE_ROUTING_KEY = "article.create";
    public static final String ARTICLE_UPDATE_ROUTING_KEY = "article.update";
    public static final String ARTICLE_DELETE_ROUTING_KEY = "article.delete";
    public static final String ARTICLE_PUBLISH_ROUTING_KEY = "article.publish";
    public static final String ARTICLE_VECTORIZED_ROUTING_KEY = "article.vectorized";


    //3、定义队列
    @Bean
    public Queue vectorQueue() {
        return new Queue(ARTICLE_QUEUE_NAME, true); //durable:持久化，重启MQ队列还在
    }

    //4、定义交换机
    @Bean
    public Exchange vectorExchange() {
        return new TopicExchange(ARTICLE_EXCHANGE_NAME);
    }

    //5、把队列绑定到交换机
//    @Bean
//    public Binding vectorBinding() {
//        return BindingBuilder.bind(vectorQueue())
//                .to(vectorExchange())
//                .with(ARTICLE_CREATE_ROUTING_KEY)
//                .noargs();
//    }

    //一次绑多个路由键
    @Bean
    public Declarables vectorBindings() {
        return new Declarables(
                BindingBuilder.bind(vectorQueue()).to(vectorExchange()).with(ARTICLE_CREATE_ROUTING_KEY).noargs(),
                BindingBuilder.bind(vectorQueue()).to(vectorExchange()).with(ARTICLE_DELETE_ROUTING_KEY).noargs(),
                BindingBuilder.bind(vectorQueue()).to(vectorExchange()).with(ARTICLE_UPDATE_ROUTING_KEY).noargs(),
                BindingBuilder.bind(vectorQueue()).to(vectorExchange()).with(ARTICLE_VECTORIZED_ROUTING_KEY).noargs(),
                BindingBuilder.bind(vectorQueue()).to(vectorExchange()).with(ARTICLE_PUBLISH_ROUTING_KEY).noargs()

        );
    }




}
