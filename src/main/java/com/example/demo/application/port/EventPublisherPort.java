package com.example.demo.application.port;

import java.util.List;

import com.example.demo.application.shared.command.PublishEventCommand;

/**
 * 事件發布介面（Event Publisher Port）。
 *
 * <pre>
 * 本介面負責將事件發送到消息系統（如 Kafka、RabbitMQ、或其他 Queue）。
 * 實現類別可依據不同的消息中介（Message Broker）決定具體發布方式。
 * </pre>
 */
public interface EventPublisherPort {

	/**
	 * 發布事件。
	 *
	 * <pre>
	 * 將指定的事件資料封裝後，發送到對應 Topic。
	 * 通常會先序列化事件物件為 JSON，再傳送到消息系統。
	 * </pre>
	 *
	 * @param command 發布事件所需的資訊，包含：
	 *                <ul>
	 *                <li>{@link PublishEventCommand#topic()} 事件所屬的 Topic 名稱</li>
	 *                <li>{@link PublishEventCommand#partitionIndex()}
	 *                可選，指定分區索引</li>
	 *                <li>{@link PublishEventCommand#event()} JSON 字串形式的事件資料</li>
	 *                </ul>
	 */
	void publish(PublishEventCommand command);

	/**
	 * 批次發布事件。
	 *
	 * <pre>
	 * 將多筆事件資料一次性或依序發送到消息系統。
	 * 此方法通常用於 Outbox 轉發排程等需要批次處理的場景，以減少呼叫次數或優化效能。
	 * </pre>
	 *
	 * @param commands 欲發布的事件資訊列表
	 */
	void publish(List<PublishEventCommand> commands);
}
