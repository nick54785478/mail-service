package com.example.demo.infra.persistence.outbox.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.infra.persistence.outbox.vo.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * OutboxMessage 事件發送歷史紀錄實體。
 *
 * <p>
 * 用於封存已成功發送 (SENT) 或確定失敗 (FAILED) 的 Outbox 紀錄。
 * 主表 OUTBOX_MESSAGE 會在資料轉入此歷史表後清除，藉此確保主表的極高查詢效能。
 * </p>
 */
@Getter
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "OUTBOX_MESSAGE_HISTORY")
@EntityListeners(AuditingEntityListener.class)
public class OutboxMessageHistory {

	/**
	 * 歷史紀錄的自動遞增流水號 (Primary Key)。
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 關聯至原本 Outbox 主表的唯一識別碼。
	 */
	@Column(name = "UUID")
	private String uuid;

	/**
	 * 觸發此事件的使用者 ID (若有)。
	 */
	@Column(name = "USER_ID")
	private String userId;

	/**
	 * 原始事件類型的完整類別名稱。
	 */
	@Column(name = "EVENT_CLASS_NAME")
	private String className;

	/**
	 * 事件最初發生的時間。
	 */
	@Column(name = "OCCURRED_AT")
	private Date occurredAt;

	/**
	 * 事件關聯的業務目標代碼 (可選)。
	 */
	@Column(name = "TARGET_ID")
	private String targetId;

	/**
	 * 事件序列化後的 JSON 內容。
	 */
	@Lob
	@Column(name = "BODY", columnDefinition = "TEXT")
	private String body;

	/**
	 * 發布的目標 Topic 或 Queue 名稱。
	 */
	@Column(name = "TOPIC")
	private String topic;

	/**
	 * 該事件在主表所累積的最終重試次數。
	 */
	@Column(name = "RETRY_COUNT")
	private Integer retryCount;

	/**
	 * 若歸檔狀態為失敗 (FAILED)，此欄位記錄最後一次發送失敗的原因或例外訊息。
	 */
	@Column(name = "FAIL_REASON")
	private String failReason;

	/**
	 * 歷史狀態 (通常為 SENT 代表發送成功，或 FAILED 代表超過最大重試次數)。
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "SEND_QUEUE_STATUS")
	private OutboxStatus status;

	/**
	 * 寫入歷史表的歸檔時間。
	 * (由 Spring Data JPA Auditing 自動帶入)
	 */
	@CreatedDate
	@Column(name = "ARCHIVED_AT")
	private Date archivedAt;

	/**
	 * 從主表的 {@link OutboxMessage} 轉換為歷史表實體。
	 *
	 * <p>
	 * 用於將待發送或重試中的主表紀錄，複製並建立為準備寫入歷史表的實體物件。
	 * 若有提供 {@code overrideStatus}，則狀態將以提供的值為主。
	 * </p>
	 *
	 * @param source         來源的主表紀錄 (OutboxMessage)
	 * @param overrideStatus 覆寫的歸檔狀態 (如 SENT 或 FAILED)。若傳入 null 則沿用來源狀態
	 * @return 建立完成的 OutboxMessageHistory 實體
	 */
	public static OutboxMessageHistory from(OutboxMessage source, OutboxStatus overrideStatus) {
		return OutboxMessageHistory.builder()
				.uuid(source.getUuid())
				.userId(source.getUserId())
				.className(source.getClassName())
				.occurredAt(source.getOccurredAt())
				.targetId(source.getTargetId())
				.body(source.getBody())
				.topic(source.getTopic())
				.retryCount(source.getRetryCount())
				.failReason(source.getFailReason())
				.status(overrideStatus != null ? overrideStatus : source.getStatus())
				.build();
	}
}
