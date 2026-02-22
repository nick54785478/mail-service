package com.example.demo.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Json Parse 工具類
 */
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonParseUtil {

	@Autowired
	protected static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * 序列化物件 為 JSON
	 * 
	 * @param target 目標物件
	 * @return String 序列化後的 json 字串
	 */
	public static String serialize(Object target) {
		try {
			return mapper.writeValueAsString(target);
		} catch (JsonProcessingException e) {
			log.error("Occurred Json Processing Exception", e);
			return "";
		}
	}

	/**
	 * 反序列化 JSON 回 物件
	 * 
	 * @param target json 字串
	 * @param clazz  目標物件類型
	 * @return 物件
	 */
	public static <T> T unserialize(String target, Class<T> clazz) {
		try {
			return mapper.readValue(target, clazz);
		} catch (JsonMappingException e) {
			log.error("Occurred JsonMapping Exception", e);
			return null;
		} catch (JsonProcessingException e) {
			log.error("Occurred JsonProcessing Exception", e);
			return null;
		}

	}

	/**
	 * 反序列化 JSON 回 物件列表
	 * 
	 * @param target json 字串
	 * @param clazz  目標物件類型
	 * @return 物件列表
	 */
	public static <T> List<T> unserializeArrayOfObject(String target, Class<T> clazz) {
		try {
			return mapper.readValue(target, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
		} catch (JsonProcessingException e) {
			log.error("Occurred JsonProcessing Exception", e);
			return null;
		}
	}
}
