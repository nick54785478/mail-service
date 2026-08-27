package com.example.demo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.service.MailApplicationService;
import com.example.demo.application.shared.command.PublishAndSendMailCommand;

@SpringBootTest
class MailIntegrationTest {

	@Autowired
	private MailApplicationService mailApplicationService;

	@Test
	void testSendMailWithTemplateContent() throws Exception {
		// 1. 讀取 email-template.html 的內容 (模擬業務端傳來的 HTML 片段)
		String content;
		try (InputStream is = getClass().getResourceAsStream("/email/email-template.html")) {
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		// 為了避免多重 body 與 html 標籤重疊，我們也可以考慮只傳遞內部真正的內容，
		// 但為了簡化測試，我們直接把整個檔案內容丟進去，瀏覽器一樣可以正常渲染。

		// 2. 準備發信命令
		PublishAndSendMailCommand command = new PublishAndSendMailCommand();
		command.setEmail("test@example.com");
		command.setSubject("整合測試：外框與內容結合");
		command.setContent(content);
		// 模擬從外部微服務傳來的業務 ID
		command.setTargetId("ORDER-20260827-001");

		// 3. 呼叫服務發布事件 (存入 Outbox)
		mailApplicationService.publishSentMailEvent(command);

		// 4. 等待 5 秒鐘，讓 OutboxRelayJob (Quartz 排程) 撈起並發送到 Kafka，
		// 接著讓 MailSendRequestedEventHandler 消費並執行實際寄信動作。
		System.out.println("事件已發布，等待 Quartz 與 Kafka 處理中...");
		Thread.sleep(5000);
		System.out.println("整合測試結束，請至 Mailpit (http://localhost:8025) 檢查信件！");
	}

}
