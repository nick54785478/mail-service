package com.example.demo.infra.event.shared.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishEventCommand {

	private String topic; // Topic

	private String partitionIndex; // 分區

	private String event; // Event 資料 (json)
}
