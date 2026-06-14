package org.example.springboot.http;

import org.example.springboot.config.AIModelProperties;
import org.example.springboot.emuns.ModelCapability;

import java.util.Map;

/**
 * 模型URL解析器
 */
public class ModelUrlResolver {
    /**
     * 主方法，候选配置了自己的 url 字段，直接返回，不走拼接。否则，用供应商的 url + endpoints 中对应能力的路径拼接成完整 URL。
     * @param provider 供应商
     * @param candidate 单一模型候选信息
     * @param capability 模型能力，区分聊天/嵌入/重排序模型
     * @return 完整的URL
     */
    public static String resolveUrl(
            AIModelProperties.ProviderConfig provider,
            AIModelProperties.ModelCandidate candidate,
            ModelCapability capability) {
        // 优先级一：候选级 URL 覆盖
        if (candidate != null && candidate.getUrl() != null && !candidate.getUrl().isBlank()) {
            return candidate.getUrl();
        }
        // 优先级二：供应商级 URL + 端点路径拼接
        if (provider == null || provider.getUrl() == null || provider.getUrl().isBlank()) {
            throw new IllegalStateException("Provider baseUrl 不存在");
        }

        Map<String, String> endpoints = provider.getEndpoints();
        String key = capability.getEndpointKey();
        String path = endpoints == null ? null : endpoints.get(key);
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Provider endpoint 不存在: " + key);
        }
        return joinUrl(provider.getUrl(), path);

    }

    /**
     * 拼接baseUrl和path，覆盖baseUrl末尾和path开头均有反斜杠、二者均无反斜杠、二者其中一个有反斜杠
     * @param baseUrl baseurl
     * @param path path
     * @return
     */
    private static String joinUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }
}
