package com.wxx.library.borrow.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * 延迟队列的 TTL（消息存活时间，单位毫秒）。
     * 业务含义：一条“逾期检查”消息在延迟队列中停留多久后被投递到逾期处理队列。
     * 生产环境应等于借阅时长（BORROW_DAYS=30 天 = 2592000000ms）；
     * 本地演示/测试可在配置中调小（如 60000ms）以便快速观察效果。
     */
    @Value("${library.rabbitmq.overdue-delay-ms:2592000000}")
    private long overdueDelayMs;

    // ==================== 逾期提醒队列（真正的处理队列，同时作为延迟队列的死信目标）====================
    
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

    // ==================== 逾期检查延迟队列（TTL + 死信队列 DLX 实现延迟消息）====================
    // 原理：消息先进入本延迟队列，该队列没有消费者，消息在此“躺”够 TTL 后过期，
    //      过期消息通过配置的死信交换机(DLX)自动转发到上面的 OVERDUE_EXCHANGE，
    //      从而实现“延迟 N 毫秒后触发逾期检查”的效果。无需依赖 RabbitMQ 延迟插件。

    public static final String OVERDUE_DELAY_EXCHANGE = "library.overdue.delay.exchange";
    public static final String OVERDUE_DELAY_QUEUE = "library.overdue.delay.queue";
    public static final String OVERDUE_DELAY_ROUTING_KEY = "library.overdue.delay";

    @Bean
    public DirectExchange overdueDelayExchange() {
        return new DirectExchange(OVERDUE_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue overdueDelayQueue() {
        return QueueBuilder.durable(OVERDUE_DELAY_QUEUE)
                // 队列级 TTL：所有消息统一延迟，天然避免“队头阻塞”问题
                .withArgument("x-message-ttl", overdueDelayMs)
                // 消息过期后转发到的死信交换机（即真正的逾期处理交换机）
                .withArgument("x-dead-letter-exchange", OVERDUE_EXCHANGE)
                // 死信转发时使用的路由键
                .withArgument("x-dead-letter-routing-key", OVERDUE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding overdueDelayBinding() {
        return BindingBuilder.bind(overdueDelayQueue())
                .to(overdueDelayExchange())
                .with(OVERDUE_DELAY_ROUTING_KEY);
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
