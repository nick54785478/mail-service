package com.example.demo.iface.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.MailApplicationService;
import com.example.demo.application.shared.command.PublishAndSendMailCommand;
import com.example.demo.iface.dto.MailSentResource;
import com.example.demo.iface.dto.PublishAndSendMailResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Tag(name = "郵件發送 API", description = "提供寄送郵件與業務整合相關功能")
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/mail")
public class SendMailController {

	private final MailApplicationService applicationService;

	@Operation(summary = "發送非同步郵件", description = "接收發信請求，並透過 Outbox Pattern 非同步拋轉以確保最終一致性。")
	@ApiResponse(responseCode = "200", description = "請求已成功收受", 
		content = @Content(schema = @Schema(implementation = MailSentResource.class)))
	@PostMapping("")
	public Mono<ResponseEntity<MailSentResource>> sendMail(@RequestBody PublishAndSendMailResource resource) {
		return Mono.fromCallable(() -> {
			// 直接映射為 Record 建構子以確保安全與極致效能，不依賴 ModelMapper
			PublishAndSendMailCommand command = new PublishAndSendMailCommand(
					resource.getEmail(),
					resource.getSubject(),
					resource.getContent(),
					resource.getTargetId()
			);
			applicationService.publishSentMailEvent(command);
			return new ResponseEntity<>(new MailSentResource("200", "寄信成功!"), HttpStatus.OK);
		}).subscribeOn(Schedulers.boundedElastic());
	}
}
