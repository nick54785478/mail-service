package com.example.demo.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.EventPublisherPort;
import com.example.demo.application.port.OutboxRelayPort;
import com.example.demo.application.shared.dto.OutboxRelayTask;
import com.example.demo.application.shared.command.PublishEventCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 處理 Outbox 轉發的核心服務。
 *
 * <p>
 * 在此服務中執行 Transactional，配合 Infra 層的 SKIP LOCKED 機制，
 * 可確保同一筆 Outbox 紀錄不會被其他節點重複撈取。
 * 成功發送 (SENT) 或確定失敗 (FAILED) 後直接搬移至歷史表並從主表刪除以保持極致輕量。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class OutboxRelayApplicationService {

	private final OutboxRelayPort outboxRelayPort;
	private final EventPublisherPort eventPublisherPort;

	/**
	 * 執行批次轉發。
	 *
	 * @param maxRetry    最大重試次數
	 * @param delayMillis 延遲讀取時間
	 */
	@Transactional
	public void processOutboxRelay(int maxRetry, long delayMillis) {
		List<OutboxRelayTask> tasks = outboxRelayPort.fetchPendingTasks(delayMillis, 500);

		if (tasks.isEmpty()) {
			return;
		}

		List<PublishEventCommand> commands = new ArrayList<>();
		List<OutboxRelayTask> toUpdate = new ArrayList<>(); // 仍需留在主表重試
		List<OutboxRelayTask> toArchiveFailed = new ArrayList<>(); // 需從主表刪除，轉入歷史(FAILED)
		List<OutboxRelayTask> toArchiveSent = new ArrayList<>(); // 需從主表刪除，轉入歷史(SENT)

		for (OutboxRelayTask task : tasks) {
			// 超過最大重試次數 → 標記 FAILED 並搬移至歷史表
			if (task.retryCount() >= maxRetry) {
				toArchiveFailed.add(task);
				continue;
			}
			// 組成 publish command
			commands.add(buildPublishCommand(task));
		}

		if (!commands.isEmpty()) {
			try {
				// 批次發送
				eventPublisherPort.publish(commands);
				
				// 發送成功，準備搬移至歷史表並刪除這些已成功發佈的訊息
				for (OutboxRelayTask task : tasks) {
					if (!toArchiveFailed.contains(task)) {
						toArchiveSent.add(task);
					}
				}
			} catch (Exception ex) {
				log.error("Batch publish failed, 將於下次排程補償", ex);
				
				// 發送失敗，不搬移也不刪除，僅保留 retryCount 的增加並留存於主表
				for (OutboxRelayTask task : tasks) {
					if (!toArchiveFailed.contains(task)) {
						toUpdate.add(task);
					}
				}
			}
		}

		// 統一處理變更
		if (!toUpdate.isEmpty()) {
			outboxRelayPort.incrementRetryCount(toUpdate);
		}
		if (!toArchiveFailed.isEmpty()) {
			outboxRelayPort.markAsFailedAndArchive(toArchiveFailed, "Retry limit exceeded");
		}
		if (!toArchiveSent.isEmpty()) {
			outboxRelayPort.markAsSentAndArchive(toArchiveSent);
		}
		
		int processedCount = toArchiveFailed.size() + toArchiveSent.size();
		if (processedCount > 0) {
			log.info("[Outbox-Relay] 完成轉發並清除，處理筆數={}", processedCount);
		}
	}

	private PublishEventCommand buildPublishCommand(OutboxRelayTask task) {
		return new PublishEventCommand(
				task.topic(),
				null,
				task.body()
		);
	}
}
