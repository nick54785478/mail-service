package com.example.demo.infra.exception.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalKafkaExceptionHandler {

	/**
	 * 處理 Kafka 消費失敗的 record
	 *
	 * @param consumerRecord Kafka 消費的原始 record
	 * @param ex             發生的例外
	 */
	public void handle(ConsumerRecord<?, ?> consumerRecord, Exception ex) {

		log.error("Kafka consume failed. topic={}, partition={}, offset={}", consumerRecord.topic(),
				consumerRecord.partition(), consumerRecord.offset(), ex);

	}

}
