package com.example.demo.application.port;

import java.util.List;
import com.example.demo.application.shared.dto.OutboxRelayTask;

/**
 * Outbox 轉發介面 (Outbox Relay Port)。
 *
 * <p>
 * 此介面負責定義 Outbox 輪詢與轉發過程中，與資料庫或底層儲存互動的行為合約。
 * 透過此介面，Application Service 可以與具體的資料庫實作解耦，
 * 確保核心商業邏輯的純粹性 (Clean Architecture)。
 * </p>
 */
public interface OutboxRelayPort {

	/**
	 * 撈取待處理的 Outbox 轉發任務。
	 *
	 * <p>
	 * 實作端應確保撈取機制具備執行緒安全性 (例如使用資料庫的 SKIP LOCKED 語法)，
	 * 以避免多節點叢集部署時，同時處理到同一筆任務。
	 * </p>
	 *
	 * @param delayMillis 延遲讀取時間 (毫秒)，用於避免撈取到尚未 commit 的紀錄
	 * @param limit       每次最多撈取的任務筆數
	 * @return 待處理的轉發任務列表
	 */
	List<OutboxRelayTask> fetchPendingTasks(long delayMillis, int limit);

	/**
	 * 標記任務為發送失敗，並歸檔至歷史紀錄。
	 *
	 * <p>
	 * 當任務達到最大重試次數，或發生不可恢復的錯誤時呼叫此方法。
	 * 實作端應將原紀錄從主表移除，並將紀錄與失敗原因搬移至歷史表中供後續追查。
	 * </p>
	 *
	 * @param tasks  欲標記失敗的任務列表
	 * @param reason 失敗的具體原因或例外訊息
	 */
	void markAsFailedAndArchive(List<OutboxRelayTask> tasks, String reason);

	/**
	 * 標記任務為發送成功，並歸檔至歷史紀錄。
	 *
	 * <p>
	 * 當事件成功發布至 Message Broker (如 Kafka 等) 後呼叫此方法。
	 * 實作端應將原紀錄從主表中刪除以保持極致輕量，並視需求留存於歷史表中。
	 * </p>
	 *
	 * @param tasks 欲標記成功的任務列表
	 */
	void markAsSentAndArchive(List<OutboxRelayTask> tasks);

	/**
	 * 增加任務的重試次數，並繼續保留於待處理清單中。
	 *
	 * <p>
	 * 當事件發布發生暫時性失敗，且尚未達到最大重試次數時呼叫此方法。
	 * 實作端應將指定任務的重試計數器加一，以便下次輪詢排程可以再次嘗試發送。
	 * </p>
	 *
	 * @param tasks 欲增加重試次數的任務列表
	 */
	void incrementRetryCount(List<OutboxRelayTask> tasks);
}
