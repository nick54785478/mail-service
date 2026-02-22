package com.example.demo.infra.adapter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.EventTopicResolverPort;
import com.example.demo.infra.event.shared.event.BaseEvent;

import lombok.RequiredArgsConstructor;

/**
 * 事件 Topic 解析器實作（Adapter）。
 *
 * <pre>
 * 本實作基於系統啟動時建立的 {@code Map<Class<? extends BaseEvent>, String>} 映射表，
 * 依據事件類型查找對應的 Topic。
 * 
 * 此類屬於 Infrastructure Layer， 將具體的 Topic 映射邏輯封裝起來， 對外僅暴露
 * {@link EventTopicResolverPort} 介面。
 * </pre>
 */
@Component
@RequiredArgsConstructor
class EventTopicResolverAdapter implements EventTopicResolverPort {

	/**
	 * 事件類型 → Topic 映射表
	 */
	private final Map<Class<? extends BaseEvent>, String> topicMapping;

	/**
	 * 根據事件實例的實際類型查找對應的 Topic。
	 *
	 * @param event 事件實例
	 * @return 對應的 Topic 名稱，若未配置則回傳 {@code null}
	 */
	@Override
	public String resolveTopic(BaseEvent event) {
		return topicMapping.get(event.getClass());
	}
}