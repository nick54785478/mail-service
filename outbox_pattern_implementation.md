# Outbox Pattern 實作解析 (以 MailService 為例)

本文件整理了 `mail-service` 專案中針對 **Outbox Pattern (發件匣模式)** 的實作細節。Outbox Pattern 主要是為了解決「更新資料庫」與「發布事件到 Message Broker」這兩個動作之間的**分散式事務 (Distributed Transaction)** 問題，確保兩者能達到最終一致性 (Eventual Consistency)。

## 1. 核心設計概念

1. **同一個 Transaction**：業務邏輯處理完成後，不要直接把事件打到 Message Broker，而是將「要發送的事件」寫入資料庫的 `OUTBOX_MESSAGE` 表。這兩個動作被包在同一個資料庫 Transaction 中，保證了原子性 (Atomicity)。
2. **非同步轉發 (Relay)**：由一個獨立的背景排程 (Scheduler/Job) 定期去掃描 `OUTBOX_MESSAGE` 表中狀態為 `INITIAL` 的資料，並透過 `EventPublisher` 轉發到 Message Broker。
3. **高效儲存與封存**：為了保證輪詢效能，主表 `OUTBOX_MESSAGE` 只保留待處理或重試中的資料；成功發送或達到重試上限的資料會搬移到歷史表 `OUTBOX_MESSAGE_HISTORY` 並從主表刪除。

---

## 2. 實作流程與程式碼解析

以 `MailApplicationService` 為例，整個流程分為「寫入 Outbox」與「排程轉發」兩大階段：

### 階段一：業務操作與事件寫入 (Write to Outbox)

在 `MailApplicationService.java` 中，我們可以看到寄信事件是如何被寫入 Outbox 的：

```java
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class MailApplicationService {
	// ... 略 ...

	public void publishSentMailEvent(PublishAndSendMailCommand command) throws IOException {
		String content = this.generateMockEmail();

		// 1. 建立領域事件 (Domain Event)
		MailSendRequestedEvent mailSendRequestedEvent = new MailSendRequestedEvent(command.getEmail(), command.getSubject(), content);
		mailSendRequestedEvent.setTargetId(UUID.randomUUID().toString());
		mailSendRequestedEvent.setOutboxMessageUuid(UUID.randomUUID().toString());

		// 2. 決定 Topic
		String topic = topicResolver.resolveTopic(mailSendRequestedEvent);

		if (topic != null) {
			// 3. 透過 Port 將事件寫入 OutboxMessage，不直接發送到 Broker
			outboxMessageManager.generateOutboxMessage(topic, mailSendRequestedEvent);
		}
	}
}
```

> [!NOTE]
> 在這個階段，因為方法標註了 `@Transactional`，`outboxMessageManager.generateOutboxMessage()` 會與外層的任何業務操作共用同一個資料庫交易。只要寫入成功，事件資料就會被安全的保存在關聯式資料庫中。

接著看看 `OutboxManagerAdapter.java` 是如何實作落地：

```java
@Transactional(propagation = Propagation.REQUIRED)
@Override
public void generateOutboxMessage(String topic, BaseEvent event) {
	// 避免重複寫入
	Optional<OutboxMessage> optional = outboxMessageRepository.findByUuid(event.getOutboxMessageUuid());
	if (optional.isPresent()) {
		return;
	}

	// 序列化事件資料並寫入資料庫
	OutboxMessage outboxMessage = new OutboxMessage();
	CreateOutboxMessageCommand command = CreateOutboxMessageCommand.builder()
			.outboxMessageUuid(event.getOutboxMessageUuid())
			.topic(topic)
			.targetId(event.getTargetId())
			.className(event.getClass().getName())
			.body(eventDataTransformer.serialize(event))
			.userId("System")
			.build();
			
	outboxMessage.create(command);
	outboxMessageRepository.saveAndFlush(outboxMessage);
}
```

---

### 階段二：非同步排程轉發 (Async Relay)

背景排程負責定期找出尚未發送的訊息，轉發給 Message Broker。此專案使用 Quartz Job 作為觸發器：

- **排程入口**: `OutboxRelayJob.java` (Quartz 定時觸發)
- **核心邏輯**: `OutboxRelayApplicationService.java`

在 `OutboxRelayApplicationService` 的 `processOutboxRelay` 中實作了高併發場景下非常重要的一套機制：

```java
@Transactional
public void processOutboxRelay(int maxRetry, long delayMillis) {
	Date timeout = new Date(System.currentTimeMillis() - delayMillis);

	// 1. 撈取待發送資料：這裡使用了 JPA 的 SKIP LOCKED (避免多個節點同時撈到同一筆資料)
	List<OutboxMessage> outboxMessages = outboxMessageRepository
			.findTop500ByStatusAndOccurredAtBeforeOrderByOccurredAtAsc(OutboxStatus.INITIAL, timeout);

	// ... 略 ... 
	// 2. 判斷重試次數與組成發送指令
	for (OutboxMessage outboxMessage : outboxMessages) {
		if (outboxMessage.getRetryCount() >= maxRetry) {
			outboxMessage.fail("Retry limit exceeded");
			toArchive.add(OutboxMessageHistory.from(outboxMessage, null));
			toDelete.add(outboxMessage);
			continue;
		}
		commands.add(buildPublishCommand(outboxMessage));
		outboxMessage.increaseRetry();
	}

	// 3. 呼叫 EventPublisherPort 批次轉發至 Broker
	if (!commands.isEmpty()) {
		try {
			eventPublisherPort.republish(commands);
			// 4a. 成功發送：加入 toArchive (搬至歷史表) 及 toDelete (從主表刪除)
			for (OutboxMessage outboxMessage : outboxMessages) { ... }
		} catch (Exception ex) {
			// 4b. 發送失敗：僅留在 toUpdate，留待下次排程重試
			for (OutboxMessage outboxMessage : outboxMessages) { ... }
		}
	}

	// 5. 統一進行資料庫異動 (寫入歷史表、刪除主表紀錄、更新重試次數)
	outboxMessageRepository.saveAll(toUpdate);
	outboxMessageHistoryRepository.saveAll(toArchive);
	outboxMessageRepository.deleteAll(toDelete);
}
```

> [!TIP]
> **高效能設計關鍵**
> - **SKIP LOCKED**: 在 `findTop500ByStatus...` 底層使用了 `FOR UPDATE SKIP LOCKED` 的資料庫鎖機制。這允許在分散式微服務架構下，多個 Pod 的排程同時輪詢時，不會發生鎖競爭或重複發送同一筆訊息。
> - **主表與歷史表分離**: 將 `OUTBOX_MESSAGE` 當作 Queue 使用。成功處理完的訊息立刻移轉到 `OUTBOX_MESSAGE_HISTORY`。這確保了主表永遠保持在一個非常小的資料量，讓 `SELECT ... FOR UPDATE` 的查詢效能最大化。

---

## 3. 架構層級總結

在此專案的六角架構 (Ports & Adapters) 中，Outbox Pattern 被完美的抽象化：

- **Domain/Application**: 只知道 `OutboxManagerPort`，不關心底層是用哪一種資料庫儲存。
- **Infrastructure**: 
  - `OutboxManagerAdapter`: 負責將資料存入 DB 實體表。
  - `OutboxRelayApplicationService`: 負責排程業務的實際執行與狀態封存。
  - `OutboxRelayJob`: 將基礎設施層的排程 (Quartz) 連接到 Application Service。

透過這樣的設計，即便未來要抽換底層的資料庫或 Message Broker，都不會影響到 `MailApplicationService` 內部的業務邏輯，達成高內聚低耦合的優雅架構。
