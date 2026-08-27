package com.example.demo.application.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.EventPublisherPort;
import com.example.demo.infra.event.shared.command.PublishEventCommand;
import com.example.demo.infra.persistence.outbox.entity.OutboxMessage;
import com.example.demo.infra.persistence.outbox.entity.OutboxMessageHistory;
import com.example.demo.infra.persistence.outbox.repository.OutboxMessageHistoryRepository;
import com.example.demo.infra.persistence.outbox.repository.OutboxMessageRepository;
import com.example.demo.infra.persistence.outbox.vo.OutboxStatus;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 處理 Outbox 轉發的核心服務。
 *
 * <p>
 * 在此服務中執行 Transactional，配合 SKIP LOCKED 機制，
 * 可確保同一筆 Outbox 紀錄不會被其他節點重複撈取。
 * 成功發送 (SENT) 或確定失敗 (FAILED) 後直接搬移至歷史表並從主表刪除以保持極致輕量。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class OutboxRelayApplicationService {

	private final OutboxMessageRepository outboxMessageRepository;
	private final OutboxMessageHistoryRepository outboxMessageHistoryRepository;
	private final EventPublisherPort eventPublisherPort;

	/**
	 * 執行批次轉發。
	 *
	 * @param maxRetry    最大重試次數
	 * @param delayMillis 延遲讀取時間
	 */
	@Transactional
	public void processOutboxRelay(int maxRetry, long delayMillis) {
		Date timeout = new Date(System.currentTimeMillis() - delayMillis);

		// 使用 SKIP LOCKED 撈取最多 500 筆待處理資料
		List<OutboxMessage> outboxMessages = outboxMessageRepository
				.findTop500ByStatusAndOccurredAtBeforeOrderByOccurredAtAsc(OutboxStatus.INITIAL, timeout);

		if (outboxMessages.isEmpty()) {
			return;
		}

		List<PublishEventCommand> commands = new ArrayList<>();
		List<OutboxMessage> toUpdate = new ArrayList<>(); // 仍需留在主表重試
		List<OutboxMessage> toDelete = new ArrayList<>(); // 需從主表刪除
		List<OutboxMessageHistory> toArchive = new ArrayList<>(); // 需新增至歷史表

		for (OutboxMessage outboxMessage : outboxMessages) {
			// 超過最大重試次數 → 標記 FAILED 並搬移至歷史表
			if (outboxMessage.getRetryCount() >= maxRetry) {
				outboxMessage.fail("Retry limit exceeded");
				toArchive.add(OutboxMessageHistory.from(outboxMessage, null));
				toDelete.add(outboxMessage);
				continue;
			}

			// 組成 publish command
			commands.add(buildPublishCommand(outboxMessage));

			// 增加 retry 次數（若失敗時可記錄）
			outboxMessage.increaseRetry();
		}

		if (!commands.isEmpty()) {
			try {
				// 批次發送
				eventPublisherPort.republish(commands);
				
				// 發送成功，準備搬移至歷史表並刪除這些已成功發佈的訊息
				for (OutboxMessage outboxMessage : outboxMessages) {
					if (!toDelete.contains(outboxMessage) && !toUpdate.contains(outboxMessage)) {
						toArchive.add(OutboxMessageHistory.from(outboxMessage, OutboxStatus.SENT));
						toDelete.add(outboxMessage);
					}
				}
			} catch (Exception ex) {
				log.error("Batch publish failed, 將於下次排程補償", ex);
				
				// 發送失敗，不搬移也不刪除，僅保留 retryCount 的增加並留存於主表
				for (OutboxMessage outboxMessage : outboxMessages) {
					if (!toDelete.contains(outboxMessage) && !toUpdate.contains(outboxMessage)) {
						toUpdate.add(outboxMessage);
					}
				}
			}
		}

		// 統一處理資料庫變更
		if (!toUpdate.isEmpty()) {
			outboxMessageRepository.saveAll(toUpdate);
		}
		if (!toArchive.isEmpty()) {
			outboxMessageHistoryRepository.saveAll(toArchive);
		}
		if (!toDelete.isEmpty()) {
			outboxMessageRepository.deleteAll(toDelete);
			log.info("[Outbox-Relay] 完成轉發並清除，處理筆數={}", toDelete.size());
		}
	}

	private PublishEventCommand buildPublishCommand(OutboxMessage outboxMessage) {
		return PublishEventCommand.builder()
				.topic(outboxMessage.getTopic())
				.event(outboxMessage.getBody())
				.build();
	}
}
