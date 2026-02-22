package com.example.demo.application.shared.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendMailCommand {

	private String email; // 寄信人的 Email

	private String subject; // 標題

	private String content; // 內容

}
