package com.example.demo.config.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 訊息傳輸主題配置類。
 *
 * <p>
 * 本類別映射應用程式設定檔 (application.properties 或 application.yml) 中 以 {@code messaging}
 * 為前綴的屬性，用於管理各個業務事件對應的訊息傳輸主題名稱 （例如 Kafka Topic、RabbitMQ Queue 等）。
 * </p>
 *
 * <p>
 * 範例 YAML 配置：
 * </p>
 * 
 * <pre>{@code
 * messaging:
 *   topics:
 *     create-order: topic.order.v1
 *     send-mail: topic.mail.v1
 * }</pre>
 *
 * <p>
 * 透過 Spring Boot 的 {@code @ConfigurationProperties}，設定檔中的
 * {@code messaging.topics} 將自動綁定到 {@link #topics} 屬性。
 * </p>
 */
@ConfigurationProperties(prefix = "messaging")
public class TopicProperties {

	/**
	 * 事件標籤與訊息傳輸主題名稱的映射表。
	 *
	 * <ul>
	 * <li><b>Key:</b> 業務事件標籤 (例如：create-order、send-mail)</li>
	 * <li><b>Value:</b> 訊息系統上的實際主題名稱 (例如：topic.order.v1、queue.mail.v1)</li>
	 * </ul>
	 *
	 * <p>
	 * 注意：
	 * <ul>
	 * <li>若此欄位為 {@code null}，表示設定檔中可能缺少 {@code messaging.topics} 節點或層級縮排有誤。</li>
	 * <li>此欄位會被 Spring Boot 自動透過 {@link #setTopics(Map)} 進行屬性綁定。</li>
	 * </ul>
	 * </p>
	 */
	private Map<String, String> topics;

	/**
	 * 取得所有已配置的訊息傳輸主題映射表 (會抓 topics)。
	 * <p>
	 * 如: messaging.topics.send-mail
	 * </p>
	 *
	 * @return 包含所有事件標籤與主題對應的 {@link Map}，若未設定則可能為 {@code null}
	 */
	public Map<String, String> getTopics() {
		return topics;
	}

	/**
	 * 設定訊息傳輸主題映射表。
	 *
	 * <p>
	 * 此 Setter 為 Spring Boot {@code @ConfigurationProperties} 綁定屬性所必需。
	 * 框架會透過此方法將讀取到的設定檔資料注入到實體中。
	 * </p>
	 *
	 * @param topics 事件標籤與對應主題名稱的 {@link Map}
	 */
	public void setTopics(Map<String, String> topics) {
		this.topics = topics;
	}
}