package com.example.demo.infra.persistence.eventlog.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infra.persistence.eventlog.entity.EventLog;
import com.example.demo.infra.persistence.eventlog.vo.EventLogSendQueueStatus;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

	Optional<EventLog> findByUuid(String uuid);

	List<EventLog> findByStatusAndOccurredAtBefore(EventLogSendQueueStatus status, Date time);

}
