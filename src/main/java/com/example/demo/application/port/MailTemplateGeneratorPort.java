package com.example.demo.application.port;

import java.io.IOException;
import java.util.Map;

/**
 * 郵件模板生成器介面（Mail Template Generator Port）。
 *
 * <pre>
 * 本介面負責根據指定的模板檔案生成標準化 HTML 郵件內容， 並將模板中的參數替換為實際值。
 * 實現類別可使用 FreeMarker、Thymeleaf 等模板引擎。
 * </pre>
 */
public interface MailTemplateGeneratorPort {

	/**
	 * 生成標準 HTML 郵件內容。
	 *
	 * <p>
	 * 方法會根據指定的模板檔案（filePath + fileName）讀取內容， 並將模板中的變數替換為 {@code params} 提供的值，最終返回完整
	 * HTML 字串。
	 * </p>
	 *
	 * @param filePath 模板檔案所在目錄路徑
	 * @param fileName 模板檔案名稱（含副檔名，如 .html）
	 * @param params   模板參數 Map，Key 對應模板中的變數名稱，Value 為替換值
	 * @return 處理後的 HTML 字串，可直接作為郵件內容
	 * @throws IOException 若模板檔案不存在或讀取/解析失敗時拋出
	 */
	String generateStandardHtmlContent(String filePath, String fileName, Map<String, Object> params) throws IOException;
}