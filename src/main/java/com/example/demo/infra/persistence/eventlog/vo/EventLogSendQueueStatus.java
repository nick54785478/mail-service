package com.example.demo.infra.persistence.eventlog.vo;

/**
 * 管理 EventLog 的 Event 狀態
 */
public enum EventLogSendQueueStatus {

	INITIAL(0), SENT(1), FAILED(2);

	private final int value;

	private EventLogSendQueueStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public boolean sameValueAs(EventLogSendQueueStatus other) {
		return other != null && this.equals(other);
	}
}