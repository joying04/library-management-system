package com.wxx.library.borrow.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class BorrowMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送逾期提醒消息
     */
    public void sendOverdueRemind(Long userId, String phone, String bookName, Long borrowId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("phone", phone);
            message.put("bookName", bookName);
            message.put("borrowId", borrowId);
            message.put("remindTime", System.currentTimeMillis());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.OVERDUE_EXCHANGE,
                RabbitMQConfig.OVERDUE_ROUTING_KEY,
                objectMapper.writeValueAsString(message)
            );
            log.info("发送逾期提醒消息成功: userId={}, bookName={}", userId, bookName);
        } catch (Exception e) {
            log.error("发送逾期提醒消息失败", e);
        }
    }

    /**
     * 发送借阅事件消息
     */
    public void sendBorrowEvent(Long userId, Long bookId, String eventType) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("bookId", bookId);
            message.put("eventType", eventType); // borrow/return/renew
            message.put("timestamp", System.currentTimeMillis());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.BORROW_EVENT_EXCHANGE,
                RabbitMQConfig.BORROW_EVENT_ROUTING_KEY,
                objectMapper.writeValueAsString(message)
            );
            log.info("发送借阅事件消息成功: userId={}, eventType={}", userId, eventType);
        } catch (Exception e) {
            log.error("发送借阅事件消息失败", e);
        }
    }
}
