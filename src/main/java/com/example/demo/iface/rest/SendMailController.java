package com.example.demo.iface.rest;

import java.io.IOException;

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
import com.example.demo.util.BaseDataTransformer;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/mail")
public class SendMailController {

	private MailApplicationService applicationService;

	@PostMapping("")
	public ResponseEntity<MailSentResource> sendMail(@RequestBody PublishAndSendMailResource resource)
			throws IOException {
		PublishAndSendMailCommand command = BaseDataTransformer.transformData(resource,
				PublishAndSendMailCommand.class);
		applicationService.publishSentMailEvent(command);
		return new ResponseEntity<>(new MailSentResource("200", "寄信成功!"), HttpStatus.OK);
	}
}
