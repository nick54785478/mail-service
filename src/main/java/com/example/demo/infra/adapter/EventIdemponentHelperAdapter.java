package com.example.demo.infra.adapter;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.infra.persistence.idempotent.entity.EventIdempotentLog;
import com.example.demo.application.port.EventIdempotentHelperPort;
import com.example.demo.infra.event.shared.event.BaseEvent;
import com.example.demo.infra.persistence.idempotent.repository.EventIdempotentLogRepository;

import lombok.AllArgsConstructor;

/**
 * Event Idempotent Service 用於執行冪等機制的 Service，防止重複消費的副作用
 */
@Component
@AllArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 3600, rollbackFor = Exception.class)
class EventIdemponentHelperAdapter implements EventIdempotentHelperPort {

	EventIdempotentLogRepository repository;

	/**
	 * 執行 Event 的冪等機制
	 * 
	 * @param event
	 * @return boolean
	 */
	@Override
	public boolean handleIdempotency(BaseEvent event) {
		boolean result = false;
		List<EventIdempotentLog> logList = repository.findByEventTypeAndUniqueKey(event.getClass().getName(),
				event.getOutboxMessageUuid());
		// 若查無資料
		if (logList.isEmpty()) {
			repository.insert(event.getClass().getName(), event.getOutboxMessageUuid(), event.getTargetId());
			result = true;
		}
		return result;
	}

}
