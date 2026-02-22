package com.example.demo.iface.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PublishAndSendMailResource {

	private String email; // 寄信人的 Email

	private String subject; // 標題

	private String content; // 內容

}
