## Event-Driven Mail Module

### Overview

本模組為一個基於 Hexagonal Architecture（六角型架構） 設計的事件驅動信件寄送系統。

** 設計目標：**


* 解耦 Application 與訊息中介系統（Kafka / RabbitMQ / 其他）


* 支援多型事件（Polymorphic Events）


* 支援事件日誌（Event Log）


* 支援事件冪等（Idempotency）


* 可替換消息基礎設施實作


* 易於測試與擴充

### Architecture Overview

本模組採用 Ports & Adapters（六角架構）

	Application Layer
	│
	├── MailApplicationService
	│
	├── EventTopicResolverPort
	├── EventPublisherPort
	├── EventLogManagerPort
	├── EventIdempotentHelperPort
	├── MailSenderPort
	├── MailTemplateGeneratorPort
	│
	Infrastructure Layer
	│
	├── EventTopicResolverAdapter
	├── EventPublisherAdapter
	├── EventLogManagerAdapter
	├── EventIdemponentHelperAdapter
	├── MailSenderAdapter
	└── MailTemplateGeneratorAdapter
	

### Core Components


**BaseEvent**

所有事件皆需繼承：

	/**
	 * Event 基礎實體類，此類包含一些通用的欄位，如: 訊息識別符、目標代碼。
	 * */
	@Data
	@SuperBuilder
	@MappedSuperclass
	@NoArgsConstructor
	@AllArgsConstructor
	public class BaseEvent {
	
	    /**
	     * 消息的唯一識別符
	     */
	    protected String eventLogUuid;
	
	    /**
	     * targetId
	     */
	    protected String targetId;
	
	}


**用途：**

* 統一事件結構

* 支援多型 JSON 序列化

* 作為事件體系的核心抽象

---

** @EventBinding **


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EventBinding {
	
		/**
		 * 事件綁定 key，用於識別事件類型。
		 *
		 * @return 事件 binding key，例如 {@code "send-mail"}
		 */
		String value();
	}

在 BaseEvent 的子類標註

	@EventBinding("send-mail")
	public class SendMailEvent extends BaseEvent

**用途：**

* 定義事件對應的「業務識別 Key」

* 系統啟動時用來建立事件 → Topic 映射

---


** TopicProperties **

對應設定檔

	messaging.topics.send-mail=topic.send-mail
	messaging.consumer-group.send-mail=group.send-mail

**用途：**

* 將事件 binding key 對應到實際消息 Topic

* 不限定 MQ，可用於任何消息系統

---

** EventMessageConfiguration **

啟動時：

* 掃描 event.package.path

* 找出所有 @EventBinding

* 與 TopicProperties 對應

* 建立：

	Map<Class<? extends BaseEvent>, String>
	
---

** EventTopicResolverPort **


用途：

* Application 不直接使用 Map

* 由 Adapter 負責解析 Topic

* 支援未來動態 Topic 策略

---

** EventJsonCodec **

負責：

* 事件序列化

* 事件反序列化

* Jackson 多型支援

JSON 範例：

	{
	  "type": "SendMailEvent",
	  "email": "test@mail.com",
	  "subject": "Hello",
	  "content": "..."
	}
	
註. 使用 BaseEventMixIn 控制 type 欄位。


---

** EventLogManagerPort **

用途：

* 記錄事件發送歷程

* 支援審計 / 重試 / 追蹤

---

** EventIdempotentHelperPort **

用途：

* 避免事件重複消費

* 提供冪等控制機制

---

** Application Service : MailApplicationService **

用於測試寄信功能

**流程：**

* 建立 SendMailEvent

* 透過 EventTopicResolver 解析 Topic

* 使用 EventJsonCodec 序列化

* 呼叫 EventPublisher 發布事件

**補充. Application Layer 不依賴：**

* Kafka

* RabbitMQ

* Spring Reflections

* 設定檔 Map


---

**EventRepublishJob**

**職責**

* 每分鐘執行一次

* 撈取：

>* status = INITIAL

>* 發生時間超過 5 分鐘

* 批次重新發布

* 更新 EventLog 狀態

* 控制最大重試次數

--- 

** ScheduleRegisterFactory **

負責將系統內部的排程註冊命令轉換為 Quartz 所需的 JobDetail 與 Trigger，並向 Scheduler 註冊任務。

**負責:**

* 封裝 Quartz API

* 統一 Job 註冊流程

* 自動處理既有 Job 的覆蓋

* 將排程定義從 Quartz API 中抽離

---

** JobScheduledRegistration **

用於進行 Quartz 排程設定(註冊) 

	@PostConstruct
	public void init() {
	    this.registerJob(
	        "EventRepublishJob",
	        "EventRepublishGroup",
	        "0 0/1 * * * ?",   // 每分鐘執行
	        EventRepublishJob.class
	    );
	}





### Event Flow

	MailApplicationService
	        │
	        ▼
	Create SendMailEvent
	        │
	        ▼
	Resolve Topic
	        │
	        ▼
	Serialize JSON
	        │
	        ▼	
	Create Event Log
	        │
	        ▼
	Publish Event
	        │
	        ▼
	Update Event Log
	        │
	        ▼
	(Consumer Side)
	Idempotency Check
	        │
	        ▼
	Application Service execution
	

### Updated Event Flow (含補償機制)

	MailApplicationService
	        │
	        ▼
	Create SendMailEvent
	        │
	        ▼
	Resolve Topic
	        │
	        ▼
	Serialize JSON
	        │
	        ▼	
	Create Event Log (INITIAL)
	        │
	        ▼
	Publish Event
	        │
	        ▼
	        ? ──► Success ──► Update Log (SENT)
	        │
	        ▼
	      Failure
	        │
	        ▼
	Quartz Republish Job
	        │
	        ▼
	Retry Publish
	        │
	        ▼
	Update EventLog



### Design Principles


**1. Infrastructure 隔離**

* Application 不知道：

* Topic 存在哪

* Map 如何建立

* 使用哪種消息中介

---

**2. 可替換 Message Broker**

若將來：

* Kafka → RabbitMQ

* RabbitMQ → Pulsar

* Topic 從 DB 取得

** >>> 只需更換 Adapter。**

---

**3. 支援擴充**

可輕易加入：

* 多租戶 Topic

* Topic 版本控制

* 事件版本升級

* Dead Letter Queue

* 重試機制

---

**4. 測試友善**

在單元測試中：

* 可 Mock EventPublisherPort

* 可 Mock EventTopicResolverPort

* 不需啟動任何消息系統

---

### Event Republish Mechanism (補償機制)

在分散式事件架構中，事件發送可能因以下原因失敗：

* Message Broker 暫時不可用

* 網路中斷

* 發送時發生例外

* Producer crash

為確保 最終一致性（Eventually Consistent），本模組提供：

> Quartz + EventLog 補償式重發布機制


### Quartz Scheduling

使用：
	Quartz Scheduler

負責：

>* 定時掃描未成功發送的事件

>* 執行補償式重發布

>* 清理過期分布式鎖



### Distributed Lock Strategy

在多節點部署下：

若多個實例同時執行 Republish，可能造成：

* 重複發送

* 競態條件

* 重複更新狀態

** >>> 使用： DB-based Distributed Lock**

**機制：**

1. 嘗試取得 lockKey = "event-republish"

2. 成功才執行 republish

3. 執行完畢後釋放鎖

4. 鎖有 timeout 避免死鎖


### Retry Strategy

EventLog 狀態流轉

	INITIAL  →  SENT
	      ↓
	    FAILED (超過重試次數)

| 條件 | 行為 |
| --- | --- |
| retryCount < MAX_RETRY  | 增加 retryCount 並重新發布 |
| retryCount ≥ MAX_RETRY | 標記 FAILED |
| 發送成功 | 標記 SENT |
| 發送失敗 | 保持 INITIAL，下次排程再處理 |

** 設計原則 **

不立即標記 FAILED ，因為：

>* Broker 可能只是暫時異常

>* 避免短暫錯誤導致永久失敗

這是一種：

** >>> 補償式重試（Compensating Retry）**



### Summary

本模組是一個：

>* 高內聚

>* 低耦合

>* 可替換消息基礎設施

>* 可測試

>* 可擴充

的事件驅動設計。

提供:

>* 事件驅動 Mail 發送

>* Hexagonal Architecture

>* Message Broker 可替換

>* Event Log 審計機制

>* Idempotency 支援

>* Quartz 補償式重發布

>* 分布式鎖保護

>* 最終一致性保障
