package com.example.demo.infra.event.shared.mixin;

import com.example.demo.infra.annotation.EventBinding;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.BaseEvent;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Jackson MixIn for {@link BaseEvent} 多型序列化/反序列化支援。
 *
 * <p>
 * 本類別不包含任何實際字段或行為，而是作為 Jackson 的 MixIn 用於：
 * <ul>
 * <li>在序列化或反序列化 {@link BaseEvent} 及其子類時，添加 {@code type} 標識字段。</li>
 * <li>支援多型 (polymorphic) JSON 處理，保證子類在序列化後包含類型資訊，反序列化時能正確還原子類。</li>
 * <li>配合 {@link EventJsonCodec} 使用，可動態註冊子類型，無需在 {@link BaseEvent} 本身加 Jackson
 * 注解。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 配置說明：
 * <ul>
 * <li>{@code use = JsonTypeInfo.Id.NAME}：使用類別名稱作為 type 值。</li>
 * <li>{@code include = JsonTypeInfo.As.PROPERTY}：將 type 以 JSON 屬性形式包含在輸出中。</li>
 * <li>{@code property = "type"}：type 欄位名稱為 "type"。</li>
 * <li>{@code visible = true}：反序列化時，type 欄位仍可被讀取到。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 * <li>此類僅作為 MixIn 使用，不能直接實例化。</li>
 * <li>實際事件類別請繼承 {@link BaseEvent} 並標註 {@link EventBinding}。</li>
 * </ul>
 * </p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
public class BaseEventMixIn {
}