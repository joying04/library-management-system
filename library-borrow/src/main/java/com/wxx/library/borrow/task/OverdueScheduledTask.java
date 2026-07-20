package com.wxx.library.borrow.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxx.library.borrow.mapper.BorrowEventMapper;
import com.wxx.library.borrow.mapper.BorrowMapper;
import com.wxx.library.borrow.rabbitmq.BorrowMessageProducer;
import com.wxx.library.common.entity.BorrowEvent;
import com.wxx.library.common.entity.BorrowRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 逾期兜底定时任务。
 *
 * <p>设计定位：与「TTL + 死信队列」实现的逾期检查延迟队列形成「双保险」。
 * <ul>
 *   <li>延迟队列（主）：借书/续借时投递延迟消息，到期精准触发逾期检查，保证<b>及时性</b>；</li>
 *   <li>本定时任务（兜底）：每天扫一遍库，捞回那些因<b>消息丢失、服务重启期间过期、长延迟不可靠</b>
 *       等原因被延迟队列漏掉的逾期记录，保证<b>可靠性</b>。</li>
 * </ul>
 *
 * <p>幂等保证：发消息前先查 borrow_event 是否已存在该借阅记录的、已成功处理的 OVERDUE 事件。
 * 若已存在（说明延迟队列已处理过），则跳过，避免对同一条逾期记录重复记录/重复提醒。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OverdueScheduledTask {

    private final BorrowMapper borrowMapper;
    private final BorrowEventMapper borrowEventMapper;
    private final BorrowMessageProducer messageProducer;

    /**
     * 每天凌晨 2 点扫描逾期记录并兜底发送逾期提醒。
     * cron 可通过配置项 library.overdue-scan.cron 覆盖（演示时可调成每 30 秒一次：0/30 * * * * ?）。
     */
    @Scheduled(cron = "${library.overdue-scan.cron:0 0 2 * * ?}")
    public void scanOverdue() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[逾期兜底任务] 开始扫描逾期记录, time={}", now);

        // 1. 扫描所有「借阅中(status=1) 且 已过应归还时间」的记录
        LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BorrowRecord::getStatus, 1)
                .lt(BorrowRecord::getExpectedReturnTime, now);
        List<BorrowRecord> overdueList = borrowMapper.selectList(queryWrapper);

        if (overdueList == null || overdueList.isEmpty()) {
            log.info("[逾期兜底任务] 未扫描到逾期记录，结束");
            return;
        }

        int sent = 0;
        int skipped = 0;
        for (BorrowRecord record : overdueList) {
            // 2. 幂等去重：延迟队列已处理并记录过 OVERDUE 事件的，跳过，避免重复
            if (hasOverdueEvent(record.getId())) {
                skipped++;
                continue;
            }
            // 3. 兜底补发逾期提醒消息（phone/bookName 由消费端查库权威补全，此处传 null 即可）
            messageProducer.sendOverdueRemind(record.getUserId(), null, null, record.getId());
            sent++;
        }

        log.info("[逾期兜底任务] 扫描完成: 逾期总数={}, 兜底补发={}, 已处理跳过={}",
                overdueList.size(), sent, skipped);
    }

    /**
     * 判断该借阅记录是否已存在「已成功处理」的逾期事件（status=1 表示已处理）。
     */
    private boolean hasOverdueEvent(Long borrowRecordId) {
        LambdaQueryWrapper<BorrowEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowEvent::getBorrowRecordId, borrowRecordId)
                .eq(BorrowEvent::getEventType, "OVERDUE")
                .eq(BorrowEvent::getStatus, 1);
        return borrowEventMapper.selectCount(wrapper) > 0;
    }
}
