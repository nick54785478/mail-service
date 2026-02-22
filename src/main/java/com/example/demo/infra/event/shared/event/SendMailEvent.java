package com.example.demo.infra.event.shared.event;

import com.example.demo.infra.annotation.EventBinding;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@EventBinding(value = "send-mail")
public class SendMailEvent extends BaseEvent {

	private String email; // 寄信人的 Email

	private String subject; // 標題

	private String content; // 內容

}
