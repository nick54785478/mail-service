package com.example.demo.infra.persistence.outbox.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import com.example.demo.infra.persistence.outbox.entity.OutboxMessage;
import com.example.demo.infra.persistence.outbox.vo.OutboxStatus;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

	Optional<OutboxMessage> findByUuid(String uuid);

	List<OutboxMessage> findByStatusAndOccurredAtBefore(OutboxStatus status, Date time);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
	List<OutboxMessage> findTop500ByStatusAndOccurredAtBeforeOrderByOccurredAtAsc(OutboxStatus status, Date time);

}
