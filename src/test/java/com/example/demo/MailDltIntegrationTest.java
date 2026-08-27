package com.example.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.application.port.MailSenderPort;
import com.example.demo.application.service.MailApplicationService;
import com.example.demo.application.shared.command.PublishAndSendMailCommand;

import jakarta.mail.MessagingException;

@SpringBootTest
class MailDltIntegrationTest {

	@Autowired
	private MailApplicationService mailApplicationService;

	// 使用 MockitoBean 來攔截實際寄信行為，模擬錯誤 (Spring Boot 3.4+)
	@MockitoBean
	private MailSenderPort mailSenderPort;

	@Test
	void testDltAlertIsTriggeredWhenMailFails() throws Exception {
		// 1. 設定 Mock：當寄送正常信件時，強制拋出異常
		doThrow(new MessagingException("模擬的 SMTP 連線失敗"))
			.when(mailSenderPort)
			.send(anyString(), eq("必定失敗的測試信"), anyString(), isNull(), isNull());

		// 2. 設定 Mock：當寄送 DLT 告警信時，允許通過
		doNothing()
			.when(mailSenderPort)
			.send(anyString(), eq("【系統告警】死信佇列 (DLT) 觸發通知"), anyString(), isNull(), isNull());

		// 3. 發布寄信任務
		PublishAndSendMailCommand command = new PublishAndSendMailCommand();
		command.setEmail("target@example.com");
		command.setSubject("必定失敗的測試信");
		command.setContent("<p>這是一封註定失敗的信件</p>");
		
		mailApplicationService.publishSentMailEvent(command);

		System.out.println("事件已發布，這會觸發 5 次 Retry，請耐心等待約 25 秒...");
		
		// 指數退避策略為：1s + 2s + 4s + 8s = 15 秒左右。再加上 Outbox 排程 (5秒)，我們等待 25 秒確保 DLT 觸發
		Thread.sleep(25000);

		// 4. 驗證是否成功觸發了告警信件發送 (可能以前的測試殘留訊息也會被消費，因此驗證至少觸發一次)
		verify(mailSenderPort, atLeastOnce()).send(
			eq("admin@your-company.com"), 
			eq("【系統告警】死信佇列 (DLT) 觸發通知"), 
			anyString(), 
			isNull(), 
			isNull()
		);
		
		System.out.println("整合測試結束：成功攔截到死信並寄出告警信！");
	}

}
