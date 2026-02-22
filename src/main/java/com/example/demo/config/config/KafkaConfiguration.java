package com.example.demo.config.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

import com.example.demo.infra.exception.handler.GlobalKafkaExceptionHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

	private final GlobalKafkaExceptionHandler globalKafkaExceptionHandler;

	private static final int MAX_ATTEMPTS = 5;
	private static final long INITIAL_RETRY_INTERVAL = 1000L;
	private static final double RETRY_MULTIPLIER = 2.0;
	private static final long MAX_RETRY_INTERVAL = 30000L;

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
			ConsumerFactory<Object, Object> consumerFactory, DefaultErrorHandler kafkaErrorHandler) {

		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
		factory.setCommonErrorHandler(kafkaErrorHandler);
		return factory;
	}

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

		ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(MAX_ATTEMPTS - 1);

		backoff.setInitialInterval(INITIAL_RETRY_INTERVAL);
		backoff.setMultiplier(RETRY_MULTIPLIER);
		backoff.setMaxInterval(MAX_RETRY_INTERVAL);

		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (eventRecord, ex) -> {

			// 這裡就是「最終失敗處理點」
			log.error("[Kafka DLT] topic={} partition={} offset={} cause={}", eventRecord.topic(), eventRecord.partition(),
					eventRecord.offset(), ex.getMessage());

			// 全域錯誤處理
			globalKafkaExceptionHandler.handle(eventRecord, ex);

			return new TopicPartition(eventRecord.topic() + ".DLT", eventRecord.partition());
		});

		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);

		handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);

		handler.setRetryListeners((eventRecord, ex, deliveryAttempt) -> {
			log.warn("[Kafka Retry] attempt={} topic={} partition={} offset={} cause={}", deliveryAttempt,
					eventRecord.topic(), eventRecord.partition(), eventRecord.offset(), ex.getMessage());
		});

		return handler;
	}
}