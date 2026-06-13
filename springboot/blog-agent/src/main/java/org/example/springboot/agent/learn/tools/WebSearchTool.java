package org.example.springboot.agent.learn.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 学习版 Web 搜索工具（多提供商可切换：serper / bing / searxng）
 */
@Slf4j
@Component
public class WebSearchTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 10;

    @Value("${agent.learn.tools.web-search.provider:serper}")
    private String provider;

    @Value("${agent.learn.tools.web-search.locale.gl:cn}")
    private String gl;

    @Value("${agent.learn.tools.web-search.locale.hl:zh-cn}")
    private String hl;

    @Value("${agent.learn.tools.web-search.serper.api-key:}")
    private String serperApiKey;

    @Value("${agent.learn.tools.web-search.serper.base-url:https://google.serper.dev/search}")
    private String serperBaseUrl;

    @Value("${agent.learn.tools.web-search.bing.api-key:}")
    private String bingApiKey;

    @Value("${agent.learn.tools.web-search.bing.base-url:https://api.bing.microsoft.com/v7.0/search}")
    private String bingBaseUrl;

    @Value("${agent.learn.tools.web-search.bing.mkt:zh-CN}")
    private String bingMarket;

    @Value("${agent.learn.tools.web-search.bing.set-lang:zh-Hans}")
    private String bingSetLanguage;

    @Value("${agent.learn.tools.web-search.searxng.base-url:http://127.0.0.1:8080/search}")
    private String searxngBaseUrl;

    @Value("${agent.learn.tools.web-search.searxng.api-key:}")
    private String searxngApiKey;

    @Value("${agent.learn.tools.web-search.searxng.api-key-header:X-API-Key}")
    private String searxngApiKeyHeader;

    @Value("${agent.learn.tools.web-search.searxng.categories:general}")
    private String searxngCategories;

    @Value("${agent.learn.tools.web-search.searxng.language:zh-CN}")
    private String searxngLanguage;

    @Value("${agent.learn.tools.web-search.timeout-ms:5000}")
    private Integer timeoutMs;

    private final ObjectMapper objectMapper;

    public WebSearchTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * web 搜索工具：返回标题、链接、摘要
     */
    @Tool(
            name = "webSearch",
            description = "执行网络搜索并返回结果列表（标题/链接/摘要）。适合查找最新网页信息。",
            returnDirect = false
    )
    public String webSearch(
            @ToolParam(description = "搜索关键词，例如：Spring AI 最新特性") String query,
            @ToolParam(description = "返回条数，默认5，范围1-10", required = false) Integer limit) {

        if (!StringUtils.hasText(query)) {
            return buildError("query 不能为空");
        }

        int finalLimit = normalizeLimit(limit);
        try {
            RestTemplate restTemplate = createRestTemplate();
            String queryText = query.trim();
            String normalizedProvider = provider == null ? "serper" : provider.trim().toLowerCase();
            return switch (normalizedProvider) {
                case "serper" -> searchBySerper(restTemplate, queryText, finalLimit);
                case "bing" -> searchByBing(restTemplate, queryText, finalLimit);
                case "searxng" -> searchBySearxng(restTemplate, queryText, finalLimit);
                default -> buildError("不支持的 provider: " + normalizedProvider + "，请使用 serper / bing / searxng");
            };
        } catch (Exception e) {
            log.error("webSearch 调用失败: {}", e.getMessage(), e);
            return buildError("webSearch 调用失败: " + e.getMessage());
        }
    }

    private String searchBySerper(RestTemplate restTemplate, String query, int limit) throws Exception {
        if (!StringUtils.hasText(serperApiKey)) {
            return buildError("未配置 Serper API Key，请设置 agent.learn.tools.web-search.serper.api-key 或环境变量 SERPER_API_KEY");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", serperApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("q", query);
        payload.put("num", limit);
        payload.put("gl", gl);
        payload.put("hl", hl);

        String payloadJson = objectMapper.writeValueAsString(payload);
        ResponseEntity<String> response = restTemplate.exchange(
                serperBaseUrl,
                HttpMethod.POST,
                new HttpEntity<>(payloadJson, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode organic = root.path("organic");
        ArrayNode items = objectMapper.createArrayNode();
        if (organic.isArray()) {
            int count = Math.min(limit, organic.size());
            for (int i = 0; i < count; i++) {
                JsonNode item = organic.get(i);
                addItem(items,
                        item.path("title").asText(""),
                        item.path("link").asText(""),
                        item.path("snippet").asText(""));
            }
        }
        return buildSuccess("serper", query, items);
    }

    private String searchByBing(RestTemplate restTemplate, String query, int limit) throws Exception {
        if (!StringUtils.hasText(bingApiKey)) {
            return buildError("未配置 Bing API Key，请设置 agent.learn.tools.web-search.bing.api-key 或环境变量 BING_SUBSCRIPTION_KEY");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(bingBaseUrl)
                .queryParam("q", query)
                .queryParam("count", limit)
                .queryParam("mkt", bingMarket)
                .queryParam("setLang", bingSetLanguage)
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", bingApiKey);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode values = root.path("webPages").path("value");
        ArrayNode items = objectMapper.createArrayNode();
        if (values.isArray()) {
            int count = Math.min(limit, values.size());
            for (int i = 0; i < count; i++) {
                JsonNode item = values.get(i);
                addItem(items,
                        item.path("name").asText(""),
                        item.path("url").asText(""),
                        item.path("snippet").asText(""));
            }
        }
        return buildSuccess("bing", query, items);
    }

    private String searchBySearxng(RestTemplate restTemplate, String query, int limit) throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl(searxngBaseUrl)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("categories", searxngCategories)
                .queryParam("language", searxngLanguage)
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(searxngApiKey) && StringUtils.hasText(searxngApiKeyHeader)) {
            headers.set(searxngApiKeyHeader, searxngApiKey);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode results = root.path("results");
        ArrayNode items = objectMapper.createArrayNode();
        if (results.isArray()) {
            int count = Math.min(limit, results.size());
            for (int i = 0; i < count; i++) {
                JsonNode item = results.get(i);
                addItem(items,
                        item.path("title").asText(""),
                        item.path("url").asText(""),
                        item.path("content").asText(""));
            }
        }
        return buildSuccess("searxng", query, items);
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int finalTimeout = timeoutMs != null && timeoutMs > 0 ? timeoutMs : 5000;
        factory.setConnectTimeout(finalTimeout);
        factory.setReadTimeout(finalTimeout);
        return new RestTemplate(factory);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    private void addItem(ArrayNode items, String title, String url, String snippet) {
        ObjectNode simple = objectMapper.createObjectNode();
        simple.put("title", title == null ? "" : title);
        simple.put("url", url == null ? "" : url);
        simple.put("snippet", snippet == null ? "" : snippet);
        items.add(simple);
    }

    private String buildSuccess(String providerName, String query, ArrayNode items) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("provider", providerName);
        result.put("query", query);
        result.put("count", items.size());
        result.set("items", items);
        return objectMapper.writeValueAsString(result);
    }

    private String buildError(String message) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("message", message);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"" + message + "\"}";
        }
    }
}
