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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "UUID")
	private String uuid;

	@Column(name = "USER_ID")
	private String userId;

	@Column(name = "EVENT_CLASS_NAME")
	private String className;

	@Column(name = "OCCURRED_AT")
	private Date occurredAt;

	@Column(name = "TARGET_ID")
	private String targetId;

	@Lob
	@Column(name = "BODY", columnDefinition = "TEXT")
	private String body;

	@Column(name = "TOPIC")
	private String topic;

	@Column(name = "RETRY_COUNT")
	private Integer retryCount;

	@Column(name = "FAIL_REASON")
	private String failReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "SEND_QUEUE_STATUS")
	private OutboxStatus status;

	/**
	 * 寫入歷史表的時間
	 */
	@CreatedDate
	@Column(name = "ARCHIVED_AT")
	private Date archivedAt;

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
