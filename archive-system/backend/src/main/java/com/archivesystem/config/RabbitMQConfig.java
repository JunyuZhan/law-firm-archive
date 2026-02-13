package com.archivesystem.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 
 * 队列设计：
 * - archive.receive.queue: 档案接收处理队列
 * - archive.receive.dlq: 死信队列（处理失败的消息）
 * - archive.callback.queue: 回调通知队列
 */
@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String ARCHIVE_EXCHANGE = "archive.exchange";
    public static final String ARCHIVE_DLX_EXCHANGE = "archive.dlx.exchange";
    
    // 队列名称
    public static final String ARCHIVE_RECEIVE_QUEUE = "archive.receive.queue";
    public static final String ARCHIVE_RECEIVE_DLQ = "archive.receive.dlq";
    public static final String ARCHIVE_CALLBACK_QUEUE = "archive.callback.queue";
    
    // 路由键
    public static final String ARCHIVE_RECEIVE_ROUTING_KEY = "archive.receive";
    public static final String ARCHIVE_CALLBACK_ROUTING_KEY = "archive.callback";
    public static final String ARCHIVE_DLQ_ROUTING_KEY = "archive.dlq";
    
    /**
     * 主交换机
     */
    @Bean
    public DirectExchange archiveExchange() {
        return ExchangeBuilder.directExchange(ARCHIVE_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange archiveDlxExchange() {
        return ExchangeBuilder.directExchange(ARCHIVE_DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 档案接收队列（配置死信转发）
     */
    @Bean
    public Queue archiveReceiveQueue() {
        return QueueBuilder.durable(ARCHIVE_RECEIVE_QUEUE)
                .withArgument("x-dead-letter-exchange", ARCHIVE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ARCHIVE_DLQ_ROUTING_KEY)
                .build();
    }
    
    /**
     * 死信队列
     */
    @Bean
    public Queue archiveReceiveDlq() {
        return QueueBuilder.durable(ARCHIVE_RECEIVE_DLQ)
                .build();
    }
    
    /**
     * 回调通知队列
     */
    @Bean
    public Queue archiveCallbackQueue() {
        return QueueBuilder.durable(ARCHIVE_CALLBACK_QUEUE)
                .build();
    }
    
    /**
     * 绑定：档案接收队列 -> 主交换机
     */
    @Bean
    public Binding archiveReceiveBinding() {
        return BindingBuilder.bind(archiveReceiveQueue())
                .to(archiveExchange())
                .with(ARCHIVE_RECEIVE_ROUTING_KEY);
    }
    
    /**
     * 绑定：死信队列 -> 死信交换机
     */
    @Bean
    public Binding archiveDlqBinding() {
        return BindingBuilder.bind(archiveReceiveDlq())
                .to(archiveDlxExchange())
                .with(ARCHIVE_DLQ_ROUTING_KEY);
    }
    
    /**
     * 绑定：回调队列 -> 主交换机
     */
    @Bean
    public Binding archiveCallbackBinding() {
        return BindingBuilder.bind(archiveCallbackQueue())
                .to(archiveExchange())
                .with(ARCHIVE_CALLBACK_ROUTING_KEY);
    }
    
    /**
     * 消息转换器（JSON）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // 开启消息确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败处理
                System.err.println("消息发送失败: " + cause);
            }
        });
        return template;
    }
    
    /**
     * 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}
