package com.wxx.library.borrow.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.wxx.library.borrow.mapper.BorrowEventMapper;
import com.wxx.library.borrow.mapper.BorrowMapper;
import com.wxx.library.common.entity.Book;
import com.wxx.library.common.entity.BorrowEvent;
import com.wxx.library.common.entity.BorrowRecord;
import com.wxx.library.common.entity.User;
import com.wxx.library.common.feign.BookFeignClient;
import com.wxx.library.common.feign.UserFeignClient;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息消费者
 *
 * 统一采用「手动 ACK + 幂等消费」模式：
 * 1. 手动 ACK：处理成功才 basicAck 确认；处理失败 basicNack 且不重回队列（requeue=false），
 *    避免毒消息（永远处理失败的消息）在队列中无限重试导致死循环。失败详情已落库便于排查。
 * 2. 幂等消费：处理前用 messageId 查 borrow_event 是否已成功处理过，
 *    若已处理则直接确认并跳过，防止因网络抖动/重投导致的重复消费。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BorrowMessageConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BorrowEventMapper borrowEventMapper;
    private final BorrowMapper borrowMapper;
    private final UserFeignClient userFeignClient;
    private final BookFeignClient bookFeignClient;

    /**
     * 监听逾期提醒队列。
     * 消息来源有两类：① 延迟队列 TTL 到期后死信转发而来的「逾期检查」消息；② 直接发送的即时提醒消息。
     * 到期后查库判断该笔借阅是否真的逾期未还，只有真正逾期才记录并（可扩展）发送提醒。
     */
    @RabbitListener(queues = RabbitMQConfig.OVERDUE_QUEUE)
    public void handleOverdueRemind(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        try {
            // 幂等校验：已成功处理过则直接确认跳过
            if (isProcessed(messageId)) {
                log.warn("逾期消息重复投递，已跳过: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String body = new String(message.getBody());
            Map<String, Object> messageData = objectMapper.readValue(body, Map.class);
            Long borrowId = messageData.get("borrowId") != null
                    ? Long.valueOf(messageData.get("borrowId").toString()) : null;

            log.info("收到逾期检查消息: borrowId={}, messageId={}", borrowId, messageId);

            // 查库判断是否真的逾期（借阅记录是唯一权威数据源）
            BorrowRecord record = borrowId != null ? borrowMapper.selectById(borrowId) : null;
            if (record == null) {
                log.info("借阅记录不存在，忽略逾期检查: borrowId={}", borrowId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            boolean stillBorrowing = record.getStatus() != null && record.getStatus() == 1;
            boolean overdue = stillBorrowing
                    && record.getExpectedReturnTime() != null
                    && LocalDateTime.now().isAfter(record.getExpectedReturnTime());

            if (!overdue) {
                // 已归还，或已续借导致应还时间延后（此时会有新的延迟消息负责后续检查），无需提醒
                log.info("借阅未逾期，跳过提醒: borrowId={}, status={}", borrowId, record.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 真正逾期：补全用户手机号与书名后记录逾期事件
            String phone = null;
            String bookName = null;
            try {
                Result<User> userResult = userFeignClient.getUserById(record.getUserId());
                if (userResult != null && ResultCode.SUCCESS.getCode().equals(userResult.getCode()) && userResult.getData() != null) {
                    phone = userResult.getData().getPhone();
                }
                Result<Book> bookResult = bookFeignClient.getBookByIdForFeign(record.getBookId());
                if (bookResult != null && ResultCode.SUCCESS.getCode().equals(bookResult.getCode()) && bookResult.getData() != null) {
                    bookName = bookResult.getData().getName();
                }
            } catch (Exception e) {
                log.warn("补全逾期提醒信息失败，仍继续记录事件: borrowId={}", borrowId, e);
            }

            BorrowEvent event = new BorrowEvent();
            event.setUserId(record.getUserId());
            event.setBookId(record.getBookId());
            event.setBorrowRecordId(record.getId());
            event.setEventType("OVERDUE");
            event.setEventData(body);
            event.setMessageId(messageId);
            event.setConsumeTime(LocalDateTime.now());
            event.setStatus(1);
            borrowEventMapper.insert(event);

            // TODO: 实际业务中可以发送短信/邮件/App推送
            // smsService.sendOverdueRemind(phone, bookName);
            log.info("逾期提醒处理完成: borrowId={}, phone={}, bookName={}", borrowId, phone, bookName);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理逾期消息失败: messageId={}", messageId, e);
            recordFailedEvent(message, messageId, "OVERDUE");
            // 处理失败，不重回队列，避免毒消息死循环（失败详情已落库）
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 监听借阅事件队列
     */
    @RabbitListener(queues = RabbitMQConfig.BORROW_EVENT_QUEUE)
    public void handleBorrowEvent(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        try {
            // 幂等校验
            if (isProcessed(messageId)) {
                log.warn("借阅事件消息重复投递，已跳过: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String body = new String(message.getBody());
            Map<String, Object> messageData = objectMapper.readValue(body, Map.class);

            Long userId = Long.valueOf(messageData.get("userId").toString());
            Long bookId = Long.valueOf(messageData.get("bookId").toString());
            String eventType = (String) messageData.get("eventType");
            Long borrowRecordId = messageData.get("borrowRecordId") != null ?
                    Long.valueOf(messageData.get("borrowRecordId").toString()) : null;

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

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理借阅事件消息失败: messageId={}", messageId, e);
            recordFailedEvent(message, messageId, "UNKNOWN");
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 幂等校验：根据 messageId 判断该消息是否已被成功处理过（status=1）
     */
    private boolean isProcessed(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<BorrowEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowEvent::getMessageId, messageId)
                .eq(BorrowEvent::getStatus, 1);
        return borrowEventMapper.selectCount(wrapper) > 0;
    }

    /**
     * 记录处理失败的事件（status=2），便于后续人工排查或补偿
     */
    private void recordFailedEvent(Message message, String messageId, String eventType) {
        try {
            BorrowEvent event = new BorrowEvent();
            event.setEventType(eventType);
            event.setEventData(new String(message.getBody()));
            event.setMessageId(messageId);
            event.setStatus(2); // 2-处理失败
            event.setConsumeTime(LocalDateTime.now());
            borrowEventMapper.insert(event);
        } catch (Exception ex) {
            log.error("记录失败事件异常", ex);
        }
    }
}
