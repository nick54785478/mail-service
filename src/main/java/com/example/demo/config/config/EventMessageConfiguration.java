package com.example.demo.config.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.config.properties.TopicProperties;
import com.example.demo.application.shared.event.BaseEvent;
import com.example.demo.application.shared.event.MailSendRequestedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 系統事件消息配置類 (Event Message Configuration)。
 *
 * <p>
 * 本類別負責在系統啟動時，將業務事件與 {@link TopicProperties} 中定義的實際消息 Topic 進行映射。 
 * 這個映射表會被注入到其他元件中使用。
 * </p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TopicProperties.class)
public class EventMessageConfiguration {

	/**
	 * 事件類別 → Topic 映射表
	 */
	private final Map<Class<? extends BaseEvent>, String> topicMapping = new HashMap<>();

	/**
	 * 生成事件類別到 Topic 的映射表 Bean。
	 *
	 * <p>
	 * 此 Bean 在 Spring Context 初始化時被創建，用於提供事件類別與消息 Topic 的對應關係。
	 * </p>
	 *
	 * @param topicProperties 配置檔中讀取的 Topic 映射
	 * @return 事件類別 → Topic 的映射表
	 */
	@Bean
	public Map<Class<? extends BaseEvent>, String> eventTopicMapping(TopicProperties topicProperties) {
		
		// 手動註冊 send-mail 事件
		String sendMailTopic = topicProperties.getTopics().get("send-mail");
		if (sendMailTopic != null) {
			topicMapping.put(MailSendRequestedEvent.class, sendMailTopic);
			log.info("[EventMessageConfiguration] {} -> topic [{}]", MailSendRequestedEvent.class.getSimpleName(), sendMailTopic);
		} else {
			log.warn("[EventMessageConfiguration] 找不到 send-mail 對應的 topic");
		}

		log.info("[EventMessageConfiguration] 初始化完成，共 {} 個事件映射", topicMapping.size());
		return topicMapping;
	}

	/**
	 * 取得事件類別 → Topic 的映射表。
	 *
	 * @return 事件映射表
	 */
	public Map<Class<? extends BaseEvent>, String> getTopicMapping() {
		return topicMapping;
	}
}