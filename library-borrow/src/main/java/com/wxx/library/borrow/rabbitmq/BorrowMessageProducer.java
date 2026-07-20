package com.wxx.library.borrow.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class BorrowMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 统一的消息发送方法：发送前为每条消息生成全局唯一的 messageId，
     * 供消费端做幂等去重（防止重复消费），并将消息设为持久化。
     */
    private void send(String exchange, String routingKey, Map<String, Object> payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        rabbitTemplate.convertAndSend(exchange, routingKey, json, message -> {
            message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            message.getMessageProperties().setContentEncoding("UTF-8");
            // 消息持久化，配合队列持久化保证 broker 重启不丢消息
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }

    /**
     * 发送“逾期检查”延迟消息（借书/续借时调用）。
     * 消息先进入延迟队列，停留够 TTL 后经死信机制转发到逾期处理队列，
     * 由消费者在到期时查库判断该笔借阅是否真的逾期未还。
     *
     * @param borrowId 借阅记录ID（消费端凭此查库判断状态，作为唯一权威数据源）
     */
    public void sendOverdueCheckDelay(Long borrowId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("borrowId", borrowId);
            message.put("sendTime", System.currentTimeMillis());

            send(RabbitMQConfig.OVERDUE_DELAY_EXCHANGE,
                    RabbitMQConfig.OVERDUE_DELAY_ROUTING_KEY,
                    message);
            log.info("发送逾期检查延迟消息成功: borrowId={}", borrowId);
        } catch (Exception e) {
            log.error("发送逾期检查延迟消息失败: borrowId={}", borrowId, e);
        }
    }

    /**
     * 发送逾期提醒消息（直接投递到逾期处理队列，用于即时提醒场景）
     */
    public void sendOverdueRemind(Long userId, String phone, String bookName, Long borrowId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("phone", phone);
            message.put("bookName", bookName);
            message.put("borrowId", borrowId);
            message.put("remindTime", System.currentTimeMillis());

            send(RabbitMQConfig.OVERDUE_EXCHANGE,
                    RabbitMQConfig.OVERDUE_ROUTING_KEY,
                    message);
            log.info("发送逾期提醒消息成功: userId={}, bookName={}", userId, bookName);
        } catch (Exception e) {
            log.error("发送逾期提醒消息失败", e);
        }
    }

    /**
     * 发送借阅事件消息
     *
     * @param borrowRecordId 借阅记录ID（消费端入库时关联到具体借阅记录，便于后续数据分析）
     */
    public void sendBorrowEvent(Long userId, Long bookId, Long borrowRecordId, String eventType) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("bookId", bookId);
            message.put("borrowRecordId", borrowRecordId);
            message.put("eventType", eventType); // borrow/return/renew
            message.put("timestamp", System.currentTimeMillis());

            send(RabbitMQConfig.BORROW_EVENT_EXCHANGE,
                    RabbitMQConfig.BORROW_EVENT_ROUTING_KEY,
                    message);
            log.info("发送借阅事件消息成功: userId={}, eventType={}", userId, eventType);
        } catch (Exception e) {
            log.error("发送借阅事件消息失败", e);
        }
    }
}
