package org.example.springboot.http;


import com.google.gson.Gson;

import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;
import okhttp3.ResponseBody;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.model.ModelTarget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 校验工具
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class HttpResponseHelper {

    private static final Gson GSON = new Gson();

    /**
     *  用于错误场景下读取响应体（供应商通常在非 200 响应的 body 里返回错误详情）
     * @param body 请求体
     * @return 请求体字符串
     */
    public static String readBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return new String(body.bytes(), StandardCharsets.UTF_8);
    }

    /**
     * 解析响应请求体
     * @param body 响应请求体
     * @param label 供应商标签
     * @return 解析后的请求体
     */
    public static JsonObject parseJson(ResponseBody body, String label) throws IOException {
        if (body == null) {
            throw new ModelClientException(
                    label + " 响应为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        String content = body.string();
        return GSON.fromJson(content, JsonObject.class);
    }

    /**
     * 获取并对供应商配置校验
     * @param target 模型信息
     * @param label 标签
     * @return 如果正常就返回供应商信息
     */
    public static AIModelProperties.ProviderConfig requireProvider(
            ModelTarget target, String label) {
        if (target == null || target.provider() == null) {
            throw new IllegalStateException(label + " 提供商配置缺失");
        }
        return target.provider();
    }

    /**
     * API KEY校验
     * @param provider 供应商信息
     * @param providerLabel 标签
     */
    public static void requireApiKey(AIModelProperties.ProviderConfig provider, String providerLabel) {
        if (provider.getApiKey() == null || provider.getApiKey().isBlank()) {
            throw new IllegalStateException(providerLabel + " API密钥缺失");
        }
    }

    /**
     * 模型名称校验
     * @param target 模型信息
     * @param providerLabel 供应商标签（这里不是模型名称，旨在说明这个是哪个供应商的模型）
     * @return
     */
    public static String requireModel(ModelTarget target, String providerLabel) {
        if (target == null || target.candidate() == null
                || target.candidate().getModel() == null) {
            throw new IllegalStateException(providerLabel + " 模型名称缺失");
        }
        return target.candidate().getModel();
    }
}