package com.example.demo.iface.schedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.log.aggregate.EventLog;
import com.example.demo.application.domain.log.aggregate.vo.EventLogSendQueueStatus;
import com.example.demo.application.port.DistributeLockManagerPort;
import com.example.demo.application.port.EventPublisherPort;
import com.example.demo.infra.event.shared.command.PublishEventCommand;
import com.example.demo.infra.persistence.EventLogRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h2>EventRepublishJob</h2>
 *
 * <p>
 * Quartz Job - 事件重發布排程。
 * </p>
 *
 * <p>
 * 職責：
 * <ul>
 * <li>透過分布式鎖確保同時間僅有一個節點執行</li>
 * <li>撈取超過 TIMEOUT 且狀態為 INITIAL 的 EventLog</li>
 * <li>進行批次重發布</li>
 * <li>更新 EventLog 狀態與 retry 次數</li>
 * </ul>
 * </p>
 *
 * <p>
 * 設計原則：
 * <ul>
 * <li>EventLog 只負責紀錄與狀態轉換</li>
 * <li>真正的 publish 由 EventPublisherPort 負責</li>
 * <li>失敗不立即標 FAILED，交由下次排程補償</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution // Quartz 層級避免同一 JVM 內併發
public class EventRepublishJob implements Job {

	private final EventLogRepository eventLogRepository;
	private final EventPublisherPort eventPublisherPort;
	private final DistributeLockManagerPort distributeLockManager;

	/**
	 * 最大重試次數
	 */
	private static final int MAX_RETRY = 5;

	/**
	 * 事件超時時間（5 分鐘）
	 */
	private static final long TIMEOUT_MILLIS = 300000L;

	/**
	 * 分布式鎖 key
	 */
	private static final String LOCK_KEY = "event-republish";

	/**
	 * 鎖存活時間
	 */
	private static final Duration LOCK_DURATION = Duration.ofSeconds(5);

	@Override
	public void execute(JobExecutionContext context) {

		log.info("[Event-republish] 排程開始");

		String ownerId = UUID.randomUUID().toString();

		// 嘗試取得分布式鎖（跨節點控制）
		if (!acquireDistributeLock(LOCK_KEY, ownerId, LOCK_DURATION)) {
			log.debug("未取得鎖，略過本次執行");
			return;
		}

		try {
			republish();
		} finally {
			releaseLockSafely(LOCK_KEY, ownerId);
		}

		log.info("[Event-republish] 排程結束");
	}

	/**
	 * 重發布邏輯核心
	 */
	private void republish() {

		Date timeout = new Date(System.currentTimeMillis() - TIMEOUT_MILLIS);

		// 撈取需要補償的事件
		List<EventLog> eventLogs = eventLogRepository.findByStatusAndOccurredAtBefore(EventLogSendQueueStatus.INITIAL,
				timeout);

		if (eventLogs.isEmpty()) {
			log.debug("無需重發布事件");
			return;
		}

		List<PublishEventCommand> commands = new ArrayList<>();
		List<EventLog> toUpdate = new ArrayList<>();

		for (EventLog eventLog : eventLogs) {

			// 超過最大重試次數 → 標記 FAILED
			if (eventLog.getRetryCount() >= MAX_RETRY) {
				eventLog.fail("Retry limit exceeded");
				toUpdate.add(eventLog);
				continue;
			}

			// 組成 publish command
			commands.add(buildPublishCommand(eventLog));

			// 增加 retry 次數（狀態仍為 INITIAL）
			eventLog.increaseRetry();
			toUpdate.add(eventLog);
		}

		// 批次發送
		publishBatch(commands, toUpdate);
	}

	/**
	 * 建立 PublishEventCommand
	 * 
	 * @param eventLog {@link EventLog}
	 */
	private PublishEventCommand buildPublishCommand(EventLog eventLog) {
		return PublishEventCommand.builder().topic(eventLog.getTopic()).event(eventLog.getBody()).build();
	}

	/**
	 * 批次發送事件
	 *
	 * @param commands 待發布命令
	 * @param toUpdate 需更新狀態的 EventLog
	 */
	private void publishBatch(List<PublishEventCommand> commands, List<EventLog> toUpdate) {

		if (commands.isEmpty()) {
			// 可能全部都超過 retry 上限
			eventLogRepository.saveAll(toUpdate);
			return;
		}

		try {
			eventPublisherPort.republish(commands);

			// 發送成功 → 標記 SENT
			toUpdate.forEach(EventLog::publish);

		} catch (Exception ex) {
			log.error("Batch publish failed, 將於下次排程補償", ex);
			return; // 不標 FAILED，交由下次排程
		}

		// 統一批次更新 DB（減少 IO 次數）
		eventLogRepository.saveAll(toUpdate);

		log.info("[Event-republish] 完成補償，處理筆數={}", toUpdate.size());
	}

	/**
	 * 嘗試取得分布式鎖
	 *
	 * @param lockKey  鎖 key
	 * @param ownerId  當前節點唯一識別
	 * @param duration 鎖存活時間
	 * @return 是否成功取得鎖
	 */
	private boolean acquireDistributeLock(String lockKey, String ownerId, Duration duration) {
		try {
			log.debug("嘗試取得分布式鎖: {}", lockKey);
			return distributeLockManager.acquireLock(lockKey, ownerId, duration);
		} catch (Exception e) {
			log.warn("取得分布式鎖失敗，可能已有其他節點執行");
			return false;
		}
	}

	/**
	 * 安全釋放鎖
	 *
	 * <p>
	 * 延遲 1 秒後釋放鎖，避免其他節點在當前節點尚未完全完成 commit / flush 時搶鎖成功。
	 * </p>
	 */
	private void releaseLockSafely(String lockKey, String ownerId) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("延遲釋放鎖被中斷", e);
		}

		log.debug("釋放鎖: {}", lockKey);
		distributeLockManager.releaseLock(lockKey, ownerId);
	}
}