package com.example.demo.application.port;

import com.example.demo.infra.event.shared.event.BaseEvent;

/**
 * 事件日誌管理介面（Event Log Manager Port）。
 *
 * <p>
 * 本介面定義系統中事件日誌的基本操作，用於追蹤事件的生成與發布狀態。 事件日誌可用於審計、重放、異常追蹤或後續分析。
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 * <li>所有事件皆應為 {@link BaseEvent} 的子類。</li>
 * <li>Topic 名稱通常對應消息系統中的 Queue/Topic，但不限定必須為 Kafka，可用於任何訊息傳輸系統。</li>
 * </ul>
 * </p>
 */
public interface OutboxManagerPort {

	/**
	 * 建立新的事件日誌記錄。
	 *
	 * <p>
	 * 在事件送出前呼叫，用於在系統內保存事件的初始狀態。 日誌內容通常包含事件類型、序列化後的 JSON 資料、時間戳等。
	 * </p>
	 *
	 * @param topic 事件發佈的目標 Topic 或 Queue 名稱
	 * @param event 要記錄的事件，必須為 {@link BaseEvent} 的子類
	 */
	void generateOutboxMessage(String topic, BaseEvent event);

	/**
	 * 更新事件日誌狀態，標記事件已成功發佈。
	 *
	 * <p>
	 * 在事件成功送出至消息系統後呼叫，用於更新日誌狀態（如已發佈、發佈失敗等）。 可用於追蹤事件生命周期，或作為事件重試機制的依據。
	 * </p>
	 *
	 * @param event 已發佈的事件，必須為 {@link BaseEvent} 的子類
	 */
	void updateStatusAfterPublished(BaseEvent event);
}
