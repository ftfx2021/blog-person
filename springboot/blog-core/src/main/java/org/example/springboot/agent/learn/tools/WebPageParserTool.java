package org.example.springboot.agent.learn.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习版：网页解析工具（基于 Jsoup）
 */
@Slf4j
@Component
public class WebPageParserTool {

    private static final int DEFAULT_MAX_TEXT_LENGTH = 6000;
    private static final int MIN_MAX_TEXT_LENGTH = 500;
    private static final int MAX_MAX_TEXT_LENGTH = 20000;
    private static final int MAX_HEADINGS = 12;
    private static final int MAX_LINKS = 15;

    @Value("${agent.learn.tools.web-parser.timeout-ms:8000}")
    private Integer timeoutMs;

    @Value("${agent.learn.tools.web-parser.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36}")
    private String userAgent;

    @Value("${agent.learn.tools.web-parser.max-text-length:6000}")
    private Integer defaultMaxTextLength;

    private final ObjectMapper objectMapper;

    public WebPageParserTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析网页并返回结构化信息
     */
    @Tool(
            name = "parseWebPage",
            description = "解析网页URL，提取标题、摘要、正文文本、H1-H3标题和主要链接。仅支持 http/https。",
            returnDirect = false
    )
    public String parseWebPage(
            @ToolParam(description = "网页URL，必须是 http/https") String url,
            @ToolParam(description = "正文最大返回字符数，默认6000，范围500-20000", required = false) Integer maxTextLength) {

        if (!StringUtils.hasText(url)) {
            return error("url 不能为空");
        }

        String rawUrl = url.trim();
        if (!isSafeUrl(rawUrl)) {
            return error("URL 不安全或不受支持，仅允许公网 http/https 地址");
        }

        int finalMaxTextLength = normalizeMaxTextLength(maxTextLength);
        int finalTimeout = timeoutMs != null && timeoutMs > 0 ? timeoutMs : 8000;

        try {
            Connection connection = Jsoup.connect(rawUrl)
                    .timeout(finalTimeout)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .userAgent(userAgent);

            Connection.Response response = connection.execute();
            int statusCode = response.statusCode();
            if (statusCode >= 400) {
                return error("网页访问失败，HTTP状态码: " + statusCode);
            }

            Document doc = response.parse();

            String title = safeTrim(doc.title());
            String description = firstMeta(doc, "meta[name=description]", "meta[property=og:description]", "meta[name=twitter:description]");
            String publishTime = firstMeta(doc, "meta[property=article:published_time]", "meta[name=pubdate]", "meta[name=date]");
            String bodyText = extractBodyText(doc, finalMaxTextLength);
            List<String> headings = extractHeadings(doc);
            List<Map<String, String>> links = extractLinks(doc);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("url", rawUrl);
            result.put("finalUrl", response.url() == null ? rawUrl : response.url().toString());
            result.put("statusCode", statusCode);
            result.put("title", title);
            result.put("description", description);
            result.put("publishTime", publishTime);
            result.put("content", bodyText);
            result.put("headings", headings);
            result.put("links", links);
            result.put("extractedAt", LocalDateTime.now().toString());
            return toJson(result);
        } catch (Exception e) {
            log.error("网页解析失败: url={}, error={}", rawUrl, e.getMessage(), e);
            return error("网页解析失败: " + e.getMessage());
        }
    }

    private int normalizeMaxTextLength(Integer maxTextLength) {
        int fallback = defaultMaxTextLength != null ? defaultMaxTextLength : DEFAULT_MAX_TEXT_LENGTH;
        int value = maxTextLength == null ? fallback : maxTextLength;
        if (value < MIN_MAX_TEXT_LENGTH) {
            return MIN_MAX_TEXT_LENGTH;
        }
        if (value > MAX_MAX_TEXT_LENGTH) {
            return MAX_MAX_TEXT_LENGTH;
        }
        return value;
    }

    private String extractBodyText(Document doc, int maxTextLength) {
        if (doc.body() == null) {
            return "";
        }
        doc.select("script,style,noscript,svg,footer,nav").remove();
        String text = safeTrim(doc.body().text());
        if (text.length() <= maxTextLength) {
            return text;
        }
        return text.substring(0, maxTextLength) + "...";
    }

    private List<String> extractHeadings(Document doc) {
        List<String> headings = new ArrayList<>();
        for (Element heading : doc.select("h1, h2, h3")) {
            if (headings.size() >= MAX_HEADINGS) {
                break;
            }
            String text = safeTrim(heading.text());
            if (StringUtils.hasText(text)) {
                headings.add(text);
            }
        }
        return headings;
    }

    private List<Map<String, String>> extractLinks(Document doc) {
        List<Map<String, String>> links = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            if (links.size() >= MAX_LINKS) {
                break;
            }
            String href = safeTrim(link.absUrl("href"));
            String text = safeTrim(link.text());
            if (!StringUtils.hasText(href)) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("text", text);
            item.put("url", href);
            links.add(item);
        }
        return links;
    }

    private String firstMeta(Document doc, String... selectors) {
        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element == null) {
                continue;
            }
            String content = safeTrim(element.attr("content"));
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        return "";
    }

    private boolean isSafeUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            String lowerHost = host.toLowerCase();
            if ("localhost".equals(lowerHost) || lowerHost.endsWith(".local")) {
                return false;
            }

            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String safeTrim(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return toJson(result);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return "{\"success\":false,\"message\":\"序列化失败\"}";
        }
    }
}
