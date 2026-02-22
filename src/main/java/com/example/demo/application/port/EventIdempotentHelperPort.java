package com.example.demo.application.port;

import com.example.demo.infra.event.shared.event.BaseEvent;

/**
 * 事件冪等處理輔助介面（Event Idempotent Helper Port）。
 *
 * <p>
 * 本介面負責在事件消費流程中執行冪等檢查，確保相同事件不會被重複處理。 冪等性是事件驅動系統中常見需求，用於避免重複發送或重複處理造成的資料不一致。
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 * <li>事件必須為 {@link BaseEvent} 的子類。</li>
 * <li>實現類別可透過事件 ID、版本號、或消息系統提供的唯一標記來判斷是否重複消費。</li>
 * </ul>
 * </p>
 */
public interface EventIdempotentHelperPort {

	/**
	 * 處理事件的冪等機制。
	 *
	 * <p>
	 * 在事件消費前呼叫，判斷事件是否已被處理過。若事件已處理，返回 {@code true} 表示重複消費， 消費者可選擇跳過該事件；若事件未處理過，返回
	 * {@code false} 並進行正常處理。
	 * </p>
	 *
	 * @param event 要檢查的事件，必須為 {@link BaseEvent} 的子類
	 * @return {@code true} 表示事件已被消費過（重複事件），{@code false} 表示事件未被處理
	 */
	boolean handleIdempotency(BaseEvent event);
}