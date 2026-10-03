package com.example.demo.application.shared.event;

/**
 * Event 基礎實體介面，此類包含一些通用的欄位，如: 訊息識別符、目標代碼。
 */
public interface BaseEvent {

	/**
	 * 消息的唯一識別符
	 */
	String outboxMessageUuid();

	/**
	 * targetId
	 */
	String targetId();

}
