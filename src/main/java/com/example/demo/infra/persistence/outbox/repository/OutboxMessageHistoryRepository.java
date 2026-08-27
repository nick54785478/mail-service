package com.example.demo.infra.persistence.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infra.persistence.outbox.entity.OutboxMessageHistory;

@Repository
public interface OutboxMessageHistoryRepository extends JpaRepository<OutboxMessageHistory, Long> {

}
