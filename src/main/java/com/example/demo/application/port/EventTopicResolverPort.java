package com.example.demo.application.port;

import com.example.demo.infra.event.shared.event.BaseEvent;

/**
 * 事件 Topic 解析介面（Event Topic Resolver Port）。
 *
 * <pre>
 * 本介面負責根據事件物件解析其對應的訊息傳輸 Topic。 
 * Application Layer 僅依賴此介面，不關心 Topic 的實際來源（例如設定檔、資料庫或動態計算）。
 * </pre>
 *
 * <p>
 * 設計目的：
 * <ul>
 * <li>隔離 Topic 映射的基礎設施實作細節</li>
 * <li>避免 Application 直接依賴 Map 或設定檔</li>
 * <li>提升系統擴充性與可測試性</li>
 * </ul>
 * </p>
 */
public interface EventTopicResolverPort {

	/**
	 * 根據事件實例解析對應的 Topic。
	 *
	 * <p>
	 * 預設實作通常會依據事件類型進行查詢， 但未來也可根據事件屬性（如 tenantId、version 等） 動態決定 Topic。
	 * </p>
	 *
	 * @param event 事件實例，必須為 {@link BaseEvent} 的子類
	 * @return 對應的 Topic 名稱；若不存在則回傳 {@code null}
	 */
	String resolveTopic(BaseEvent event);
}