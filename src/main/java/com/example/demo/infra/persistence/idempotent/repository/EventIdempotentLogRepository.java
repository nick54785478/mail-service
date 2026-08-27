package com.example.demo.infra.persistence.idempotent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.infra.persistence.idempotent.entity.EventIdempotentLog;
import com.example.demo.infra.persistence.idempotent.entity.EventIdempotentLog.EventIdempotentLogId;

import jakarta.persistence.LockModeType;

@Repository
public interface EventIdempotentLogRepository extends JpaRepository<EventIdempotentLog, EventIdempotentLogId> {

	@Modifying
	@Query(value = "INSERT INTO event_idempotent_log (event_type, unique_id) VALUES (:eventType, :uniqueKey)", nativeQuery = true)
	int insert(String eventType, String uniqueKey);

	@Modifying
	@Query(value = "INSERT INTO event_idempotent_log (event_type, unique_id, target_id, created_date) VALUES (:eventType, :uniqueKey, :targetId, NOW())", nativeQuery = true)
	int insert(String eventType, String uniqueKey, String targetId);

	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	List<EventIdempotentLog> findByEventTypeAndUniqueKey(String eventType, String uniqueKey);
}
