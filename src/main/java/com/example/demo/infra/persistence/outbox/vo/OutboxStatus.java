package com.example.demo.infra.persistence.outbox.vo;

/**
 * 管理 OutboxMessage 的 Event 狀態
 */
public enum OutboxStatus {

	INITIAL(0), SENT(1), FAILED(2);

	private final int value;

	private OutboxStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public boolean sameValueAs(OutboxStatus other) {
		return other != null && this.equals(other);
	}
}