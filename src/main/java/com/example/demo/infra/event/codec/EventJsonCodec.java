package com.example.demo.infra.event.codec;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.demo.infra.event.shared.event.BaseEvent;
import com.example.demo.infra.event.shared.mixin.BaseEventMixIn;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * 事件 JSON 編解碼器 (Event JSON Codec)。
 *
 * <p>
 * 本類負責將事件物件與 JSON 字串之間互相轉換，支援：
 * <ul>
 * <li>序列化：將 Java 事件物件轉換為 JSON 字串</li>
 * <li>反序列化：將 JSON 字串還原為對應的事件物件</li>
 * </ul>
 * </p>
 *
 * <p>
 * 特性：
 * <ul>
 * <li>自動支援 Java 8 日期時間類型 (LocalDateTime, LocalDate 等)</li>
 * <li>支援事件多型 (Polymorphic Type)，透過 {@link BaseEventMixIn} 定義 type 屬性</li>
 * <li>動態註冊事件子類，從外部注入的事件映射表自動完成 Jackson 子類註冊</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class EventJsonCodec {

	/**
	 * Jackson ObjectMapper 實例，用於序列化與反序列化
	 */
	private final ObjectMapper eventMapper;

	/**
	 * 建構子。
	 *
	 * <p>
	 * 透過注入的事件類別與對應主題映射表，初始化 ObjectMapper 並動態註冊所有事件子類。
	 * </p>
	 *
	 * @param topicMapping 事件類別與訊息傳輸主題名稱的映射表，用於動態註冊子類以支援多型反序列化。 Key 為事件類別
	 *                     (Class&lt;? extends BaseEvent&gt;)， Value 為對應的主題名稱 (例如
	 *                     Queue 或 Topic 名稱)。
	 */
	public EventJsonCodec(@Qualifier("eventTopicMapping") Map<Class<? extends BaseEvent>, String> topicMapping) {

		// 初始化 ObjectMapper，支援 Java 8 時間類型
		this.eventMapper = JsonMapper.builder().addModule(new JavaTimeModule()) // 註冊 Java 8 時間模組
				.addMixIn(BaseEvent.class, BaseEventMixIn.class) // 註冊多型 MixIn
				.build();

		// 動態註冊事件子類
		if (topicMapping != null) {
			topicMapping.keySet().forEach(clazz -> {
				this.eventMapper.registerSubtypes(clazz); // Jackson 將使用類別名稱作為 type 值
				log.info("Dynamic register Jackson subtype: {}", clazz.getSimpleName());
			});
		}

		log.info("EventJsonCodec initialized with {} events", topicMapping != null ? topicMapping.size() : 0);
	}

	/**
	 * 將 JSON 字串反序列化為指定類型的事件物件。
	 *
	 * <p>
	 * 若 JSON 無法解析或與目標類型不匹配，將返回 {@code null}。
	 * </p>
	 *
	 * @param target JSON 字串
	 * @param clazz  欲轉換的目標類型
	 * @param <T>    目標類型泛型
	 * @return 對應的 Java 物件，若失敗則回傳 {@code null}
	 */
	public <T> T unserialize(String target, Class<T> clazz) {
		try {
			return eventMapper.readValue(target, clazz);
		} catch (Exception e) {
			log.error("JSON 反序列化失敗: {}", target, e);
			return null;
		}
	}

	/**
	 * 將事件物件序列化為 JSON 字串。
	 *
	 * <p>
	 * 若物件為 {@code null} 或序列化失敗，將返回 {@code null}。
	 * </p>
	 *
	 * @param target 欲序列化的事件物件
	 * @return 對應的 JSON 字串，若失敗則回傳 {@code null}
	 */
	public String serialize(Object target) {
		try {
			return target == null ? null : eventMapper.writeValueAsString(target);
		} catch (Exception e) {
			log.error("JSON 序列化失敗: {}", target, e);
			return null;
		}
	}
}