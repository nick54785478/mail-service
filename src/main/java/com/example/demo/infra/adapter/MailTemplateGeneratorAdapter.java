package com.example.demo.infra.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.demo.application.port.MailTemplateGeneratorPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
class MailTemplateGeneratorAdapter implements MailTemplateGeneratorPort {

	private static final TemplateEngine templateEngine = new TemplateEngine();

	/**
	 * 讀取 Html 模板
	 * 
	 * @param path     檔案路徑
	 * @param fileName 檔案名稱
	 * @return html 字串
	 */
	public String readHtmlFile(String filePath, String fileName) throws IOException {
		// 建立類路徑資源物件
		InputStream inputStream = getResource(filePath, fileName);
		// 讀取檔案內容並轉換為字串
		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}

	/**
	 * 讀取 Html 模板
	 * 
	 * @param InputStream 資料流
	 * @return html 字串
	 */
	public String readHtmlFile(InputStream inputStream) throws IOException {
		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}

	/**
	 * 將 Html String parse 進 Thymleaf
	 * 
	 * @param html   - 字串
	 * @param params - 模板參數 Map
	 * @return 經處理後的 Html String
	 */
	public String parseHtmlString(String htmlString, Map<String, Object> params) {
		// 創建一個 Thymeleaf 上下文
		Context context = new Context();
		// 設定模板引擎上下文的變數
		context.setVariables(params);
		// 使用模板引擎處理 HTML 字串並替換變數
		return templateEngine.process(htmlString, context);
	}

	/**
	 * 建立標準 HTML 內容
	 * 
	 * @param filePath - 檔案路徑
	 * @param fileName - 檔案名稱
	 * @param params   - 模板參數 Map
	 * @return 經處理後的 Html String
	 */
	@Override
	public String generateStandardHtmlContent(String filePath, String fileName, Map<String, Object> params) {
		try {
			String htmlString = readHtmlFile(filePath, fileName);
			return parseHtmlString(htmlString, params);
		} catch (Exception e) {
			log.error("轉換範本發生錯誤，拋出例外", e);
			return null;
		}

	}

	/**
	 * 根據給定的地區語言（Locale），將多個標籤的多國語系訊息取出
	 * 
	 * @param locale - 地區
	 * @param data   - 資料 Map
	 * @return 經處理後的 Map
	 */
	public Map<String, Object> putHeaderForLanguage(Locale locale, Map<String, Object> data) {

		// 創建 ResourceBundle.Control 實例，使用 ResourceBundle 的默認格式
		ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);
		// 根據地區語言（Locale）和 ResourceBundle.Control 加載相應的多國語系資源檔案（messages）
		// 視情況使用，在 i18n 加上 messages_zh_TW.properties、messages_en_US.properties 等文件
		ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/messages", locale, control);

		// 獲取多國語系資源檔中的所有鍵
		Enumeration<String> keys = resourceBundle.getKeys();
		// 遍歷資源檔中的每個鍵，將其對應的值放入傳入的資料 Map 中
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object value = resourceBundle.getObject(key);
			data.put(key, value);
		}
		log.info("取得後端語系配置檔:{} ", data);
		return data;
	}

	/**
	 * 根據語系 取得 多語 資料
	 * 
	 * @param language - 語系
	 * @param data     - 語系資料 Map<key, value>
	 */
	public Map<String, Object> generateLocalizedContent(String language, Map<String, Object> data) {
		Locale locale = getLocale(language);
		return putHeaderForLanguage(locale, data);
	}

	// 根據輸入的語言字串，回傳相對應的地區語言（Locale）設定
	private static Locale getLocale(String language) {
		switch (StringUtils.defaultString(language)) {
		case "zh-tw":
			return Locale.TAIWAN;
		case "zh-cn":
			return Locale.CHINA;
		default:
			return Locale.US;
		}
	}

	/**
	 * 從 classpath 取得資料流
	 *
	 * @param filePath 檔案路徑（以 `/` 開頭）
	 * @param fileName 檔案名稱（可省略 .html 副檔名）
	 */
	public InputStream getResource(String filePath, String fileName) {
		String url = assemblePath(filePath, fileName);
		return MailTemplateGeneratorAdapter.class.getResourceAsStream(url);
	}

	/**
	 * 組合路徑，會自動清理多餘的斜線，忽略 null 與空字串。
	 *
	 * @param segments 路徑片段（不限數量）
	 * @return 組合後的路徑字串
	 */
	private String assemblePath(String... segments) {
		String joined = Arrays.stream(segments).filter(Objects::nonNull)
				.map(s -> s.replaceAll("^/+", "").replaceAll("/+$", "")).filter(s -> !s.isEmpty())
				.collect(Collectors.joining("/"));
		// 確保開頭為 /
		if (!joined.startsWith("/")) {
			joined = "/" + joined;
		}

		// 確保結尾為 .html
		if (!joined.endsWith(".html")) {
			joined += ".html";
		}
		return joined;
	}

}
