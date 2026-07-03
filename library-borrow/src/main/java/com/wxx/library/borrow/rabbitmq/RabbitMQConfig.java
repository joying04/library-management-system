package com.wxx.library.borrow.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==================== 逾期提醒队列 ====================
    
    public static final String OVERDUE_EXCHANGE = "library.overdue.exchange";
    public static final String OVERDUE_QUEUE = "library.overdue.queue";
    public static final String OVERDUE_ROUTING_KEY = "library.overdue.remind";

    @Bean
    public DirectExchange overdueExchange() {
        return new DirectExchange(OVERDUE_EXCHANGE, true, false);
    }

    @Bean
    public Queue overdueQueue() {
        return QueueBuilder.durable(OVERDUE_QUEUE).build();
    }

    @Bean
    public Binding overdueBinding() {
        return BindingBuilder.bind(overdueQueue())
                .to(overdueExchange())
                .with(OVERDUE_ROUTING_KEY);
    }

    // ==================== 借阅事件队列 ====================
    
    public static final String BORROW_EVENT_EXCHANGE = "library.borrow.event.exchange";
    public static final String BORROW_EVENT_QUEUE = "library.borrow.event.queue";
    public static final String BORROW_EVENT_ROUTING_KEY = "library.borrow.event";

    @Bean
    public DirectExchange borrowEventExchange() {
        return new DirectExchange(BORROW_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue borrowEventQueue() {
        return QueueBuilder.durable(BORROW_EVENT_QUEUE).build();
    }

    @Bean
    public Binding borrowEventBinding() {
        return BindingBuilder.bind(borrowEventQueue())
                .to(borrowEventExchange())
                .with(BORROW_EVENT_ROUTING_KEY);
    }
}
