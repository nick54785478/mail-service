package com.example.demo.application.domain.idempotent.aggregate;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.demo.application.domain.idempotent.aggregate.EventIdempotentLog.EventIdempotentLogId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EventLog 的 冪等表實體
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EventIdempotentLogId.class)
@Table(name = "EVENT_IDEMPOTENT_LOG")
@EntityListeners(AuditingEntityListener.class)
public class EventIdempotentLog {

	@Id
	@Column(name = "UNIQUE_KEY")
	private String uniqueKey; // 對應 EventLog 的 UUID

	@Id
	@Column(name = "EVENT_TYPE")
	private String eventType; // Event 類型

	@Column(name = "TARGET_ID")
	private String targetId; // 該事件目標的 UUID (如: 火車等)

	@CreatedDate
	@Column(name = "CREATED_DATE")
	private Date createdDate; // 建立時間

	public EventIdempotentLog(String eventType, String uniqueKey) {
		this.eventType = eventType;
		this.uniqueKey = uniqueKey;
	}

	/**
	 * Event IdempotentLog 的複合主鍵
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EventIdempotentLogId implements Serializable {
		private static final long serialVersionUID = 1L;

		private String eventType;

		private String uniqueKey;

		public int hashCode() {
			int result = 1;
			result = 31 * result + (this.eventType == null ? 0 : this.eventType.hashCode());
			result = 31 * result + (this.uniqueKey == null ? 0 : this.uniqueKey.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				EventIdempotentLogId other = (EventIdempotentLogId) obj;
				if (this.eventType == null) {
					if (other.eventType != null) {
						return false;
					}
				} else if (!this.eventType.equals(other.eventType)) {
					return false;
				}

				if (this.uniqueKey == null) {
					if (other.uniqueKey != null) {
						return false;
					}
				} else if (!this.uniqueKey.equals(other.uniqueKey)) {
					return false;
				}

				return true;
			}
		}

		public String toString() {
			return "EventIdempotentLogId(eventType=" + this.eventType + ", uniqueKey=" + this.uniqueKey + ")";
		}

	}

}
