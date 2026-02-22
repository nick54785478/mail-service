package com.example.demo.application.domain.log.aggregate;

import java.util.Date;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.application.domain.log.aggregate.vo.EventLogSendQueueStatus;
import com.example.demo.application.domain.log.command.CreateEventLogCommand;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * EventLog 事件發送紀錄實體。
 *
 * <pre>
 * 此實體用於記錄系統內部事件（Event）的發送過程與狀態，並作為事件可靠投遞（Reliable Delivery）的追蹤依據。
 * 
 * 設計原則：
 *  。 此類別僅負責「紀錄」事件與狀態，不負責實際發送邏輯
 *  。 實際發送動作由 Application Service 或 Aspect 控制
 *  。 狀態變更僅能透過 publish() / fail() 進行
 * 
 * 狀態流轉：
 *   - INITIAL → SENT
 *   - INITIAL → FAILED
 * 
 * </pre>
 */
@Getter
@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "EVENT_LOG")
@EntityListeners(AuditingEntityListener.class)
public class EventLog {

	/**
	 * 主鍵 ID（資料庫識別）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 事件唯一識別碼（由外部建立並傳入）
	 */
	@Column(name = "UUID")
	private String uuid;

	/**
	 * 觸發事件的使用者帳號
	 */
	@Column(name = "USER_ID")
	private String userId;

	/**
	 * 事件類型（完整 Class Name）
	 */
	@Column(name = "EVENT_CLASS_NAME")
	private String className;

	/**
	 * 事件發生時間（由 Spring Data Auditing 自動填入）
	 */
	@CreatedDate
	@Column(name = "OCCURRED_AT")
	private Date occurredAt;

	/**
	 * 事件目標識別（例如單號、實體 ID 等）
	 */
	@Column(name = "TARGET_ID")
	private String targetId;

	/**
	 * 事件內容（通常為 JSON 字串）
	 */
	@Column(name = "BODY")
	private String body;

	/**
	 * 發送主題（例如 Kafka Topic、MQ Routing Key 等）
	 */
	@Column(name = "TOPIC")
	private String topic;

	/**
	 * 重試次數（僅統計用途，不影響狀態流轉）
	 */
	@Column(name = "RETRY_COUNT")
	private Integer retryCount;

	/**
	 * 發送失敗原因（若為 FAILED 狀態時紀錄）
	 */
	@Column(name = "FAIL_REASON")
	private String failReason;

	/**
	 * Event 發送狀態。
	 *
	 * <ul>
	 * <li>INITIAL：尚未成功發送</li>
	 * <li>SENT：已成功發送</li>
	 * <li>FAILED：發送失敗</li>
	 * </ul>
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "SEND_QUEUE_STATUS")
	private EventLogSendQueueStatus status;

	/**
	 * 在資料持久化前執行。
	 *
	 * <p>
	 * 若尚未設定狀態，預設為 INITIAL。 確保所有新建事件在未發送前皆為待發送狀態。
	 * </p>
	 */
	@PrePersist
	public void prePersist() {
		if (Objects.isNull(this.status)) {
			this.status = EventLogSendQueueStatus.INITIAL;
		}
	}

	/**
	 * 建立新的 EventLog。
	 *
	 * <p>
	 * 此方法僅負責填充事件資料，不負責改變狀態。 狀態將由 @PrePersist 設定為 INITIAL。
	 * </p>
	 *
	 * @param command 建立事件紀錄所需資料
	 */
	public void create(CreateEventLogCommand command) {
		this.uuid = command.getEventLogUuid();
		this.topic = command.getTopic();
		this.targetId = command.getTargetId();
		this.className = command.getClassName();
		this.body = command.getBody();
		this.userId = command.getUserId();
		this.retryCount = 0;
	}

	/**
	 * 標記事件已成功發送。
	 *
	 * <p>
	 * 僅在實際 publish 成功後呼叫。
	 * </p>
	 */
	public void publish() {
		this.status = EventLogSendQueueStatus.SENT;
	}

	/**
	 * 增加重試次數。
	 *
	 * <p>
	 * 僅做統計用途，不會改變事件狀態。 狀態流轉必須由 publish() 或 fail() 控制。
	 * </p>
	 */
	public void increaseRetry() {
		if (this.retryCount == null) {
			this.retryCount = 0;
		}
		this.retryCount++;
	}

	/**
	 * 標記事件發送失敗。
	 *
	 * <p>
	 * 失敗原因將被記錄，狀態轉為 FAILED。
	 * </p>
	 *
	 * @param reason 失敗原因說明
	 */
	public void fail(String reason) {
		this.failReason = reason;
		this.status = EventLogSendQueueStatus.FAILED;
	}
}