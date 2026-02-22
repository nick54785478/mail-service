package com.example.demo.config.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.config.properties.TopicProperties;
import com.example.demo.infra.annotation.EventBinding;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.BaseEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 系統事件消息配置類 (Event Message Configuration)。
 *
 * <p>
 * 本類別負責在系統啟動時，動態掃描指定 package 下所有標註了 {@link EventBinding} 的事件類別， 並將它們與
 * {@link TopicProperties} 中定義的實際消息 Topic 進行映射。 這個映射表會被注入到
 * {@link EventJsonCodec} 或其他事件傳輸元件，用於：
 * <ul>
 * <li>根據事件類別查找對應 Topic</li>
 * <li>支援事件序列化與反序列化</li>
 * <li>作為消息發送的路由依據</li>
 * </ul>
 * </p>
 *
 * <p>
 * 核心流程：
 * <ol>
 * <li>讀取配置屬性 {@code event.package.path}，指定事件類別所在的 package。</li>
 * <li>利用 Reflections 掃描該 package 中所有標註 {@link EventBinding} 的類別。</li>
 * <li>過濾未繼承 {@link BaseEvent} 的類別，並對有效事件類別：
 * <ul>
 * <li>讀取 {@link EventBinding#value()} 作為配置 key</li>
 * <li>查找 {@link TopicProperties#getTopics()} 中對應的實際 Topic</li>
 * <li>將事件類別與 Topic 映射並存入 {@link #topicMapping}</li>
 * </ul>
 * </li>
 * <li>初始化完成後，返回事件 → Topic 的映射表。</li>
 * </ol>
 * </p>
 *
 * <p>
 * 注意事項：
 * <ul>
 * <li>若事件類別未繼承 {@link BaseEvent}，將被忽略並輸出警告。</li>
 * <li>若 {@link TopicProperties} 中找不到對應的 Topic，將輸出警告並忽略該事件。</li>
 * <li>返回的 {@link #topicMapping} 可用於事件發送或序列化元件，確保事件正確路由。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TopicProperties.class)
public class EventMessageConfiguration {

	/**
	 * 要掃描的事件類別 package 路徑，例如 "com.example.event"
	 */
	@Value("${event.package.path}")
	private String packagePath;

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
		log.info("開始掃描事件 package: {}", packagePath);

		Reflections reflections = new Reflections(packagePath);
		Set<Class<?>> eventClasses = reflections.getTypesAnnotatedWith(EventBinding.class);

		if (eventClasses.isEmpty()) {
			log.warn("[EventMessageConfiguration] 沒有掃描到任何 @EventBinding 類");
			return topicMapping;
		}

		for (Class<?> clazz : eventClasses) {
			if (!BaseEvent.class.isAssignableFrom(clazz)) {
				log.warn("[EventMessageConfiguration] {} 未繼承 BaseEvent，忽略", clazz.getSimpleName());
				continue;
			}

			EventBinding ann = clazz.getAnnotation(EventBinding.class);
			String configKey = ann.value();
			String realTopic = topicProperties.getTopics().get(configKey);

			if (realTopic == null) {
				log.warn("[EventMessageConfiguration] {} 找不到對應 topic (key={})", clazz.getSimpleName(), configKey);
				continue;
			}

			@SuppressWarnings("unchecked")
			Class<? extends BaseEvent> eventClass = (Class<? extends BaseEvent>) clazz;
			topicMapping.put(eventClass, realTopic);
			log.info("[EventMessageConfiguration] {} -> topic [{}]", clazz.getSimpleName(), realTopic);
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