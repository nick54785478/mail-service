package com.example.demo.application.shared.command;

/**
 * 發布事件的 Command (應用層 DTO)
 *
 * 封裝發布事件所需的參數，例如主題名稱、分區索引與 JSON 格式的事件資料。
 */
public record PublishEventCommand(
	String topic,
	String partitionIndex,
	String event
) {}
