package com.wxx.library.borrow.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxx.library.borrow.mapper.BorrowEventMapper;
import com.wxx.library.common.entity.BorrowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息消费者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BorrowMessageConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BorrowEventMapper borrowEventMapper;

    /**
     * 监听逾期提醒队列
     */
    @RabbitListener(queues = RabbitMQConfig.OVERDUE_QUEUE)
    public void handleOverdueRemind(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> messageData = objectMapper.readValue(body, Map.class);
            
            Long userId = Long.valueOf(messageData.get("userId").toString());
            String phone = (String) messageData.get("phone");
            String bookName = (String) messageData.get("bookName");
            String messageId = message.getMessageProperties().getMessageId();
            
            log.info("收到逾期提醒消息: userId={}, phone={}, bookName={}, messageId={}", userId, phone, bookName, messageId);
            
            // 将逾期提醒事件入库
            BorrowEvent event = new BorrowEvent();
            event.setUserId(userId);
            event.setEventType("OVERDUE");
            event.setEventData(body);
            event.setMessageId(messageId);
            event.setConsumeTime(LocalDateTime.now());
            event.setStatus(1);
            borrowEventMapper.insert(event);
            
            // TODO: 实际业务中可以发送短信/邮件/App推送
            // smsService.sendOverdueRemind(phone, bookName);
            log.info("逾期提醒消息处理完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("处理逾期提醒消息失败", e);
            // 记录失败事件
            try {
                String body = new String(message.getBody());
                BorrowEvent event = new BorrowEvent();
                event.setEventType("OVERDUE");
                event.setEventData(body);
                event.setStatus(2); // 2-处理失败
                event.setConsumeTime(LocalDateTime.now());
                borrowEventMapper.insert(event);
            } catch (Exception ex) {
                log.error("记录失败事件异常", ex);
            }
        }
    }

    /**
     * 监听借阅事件队列
     */
    @RabbitListener(queues = RabbitMQConfig.BORROW_EVENT_QUEUE)
    public void handleBorrowEvent(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> messageData = objectMapper.readValue(body, Map.class);
            
            Long userId = Long.valueOf(messageData.get("userId").toString());
            Long bookId = Long.valueOf(messageData.get("bookId").toString());
            String eventType = (String) messageData.get("eventType");
            Long borrowRecordId = messageData.get("borrowRecordId") != null ? 
                    Long.valueOf(messageData.get("borrowRecordId").toString()) : null;
            String messageId = message.getMessageProperties().getMessageId();
            
            log.info("收到借阅事件消息: userId={}, bookId={}, eventType={}, messageId={}", userId, bookId, eventType, messageId);
            
            // 将借阅事件入库（用于后续数据分析、用户画像、图书推荐等）
            BorrowEvent event = new BorrowEvent();
            event.setUserId(userId);
            event.setBookId(bookId);
            event.setBorrowRecordId(borrowRecordId);
            event.setEventType(eventType);
            event.setEventData(body);
            event.setMessageId(messageId);
            event.setConsumeTime(LocalDateTime.now());
            event.setStatus(1);
            borrowEventMapper.insert(event);
            
            // TODO: 实际业务中可以做以下操作：
            // 1. 更新用户借阅行为画像
            // 2. 更新图书热度统计
            // 3. 触发推荐算法
            // 4. 发送统计事件到数据仓库
            log.info("借阅事件消息处理完成: userId={}, eventType={}", userId, eventType);
            
        } catch (Exception e) {
            log.error("处理借阅事件消息失败", e);
            // 记录失败事件
            try {
                String body = new String(message.getBody());
                BorrowEvent event = new BorrowEvent();
                event.setEventType("UNKNOWN");
                event.setEventData(body);
                event.setStatus(2); // 2-处理失败
                event.setConsumeTime(LocalDateTime.now());
                borrowEventMapper.insert(event);
            } catch (Exception ex) {
                log.error("记录失败事件异常", ex);
            }
        }
    }
}
