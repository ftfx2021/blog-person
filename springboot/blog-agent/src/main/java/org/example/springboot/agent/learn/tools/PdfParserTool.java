package org.example.springboot.agent.learn.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习版：PDF 解析工具（基于 Spring AI PagePdfDocumentReader）
 */
@Slf4j
@Component
public class PdfParserTool {

    private static final int DEFAULT_MAX_TEXT_LENGTH = 8000;
    private static final int MIN_MAX_TEXT_LENGTH = 1000;
    private static final int MAX_MAX_TEXT_LENGTH = 50000;
    private static final int DEFAULT_MAX_PAGES = 20;
    private static final int MIN_MAX_PAGES = 1;
    private static final int MAX_MAX_PAGES = 200;

    @Value("${agent.learn.tools.pdf-parser.timeout-ms:10000}")
    private Integer timeoutMs;

    @Value("${agent.learn.tools.pdf-parser.max-text-length:8000}")
    private Integer defaultMaxTextLength;

    @Value("${agent.learn.tools.pdf-parser.max-pages:20}")
    private Integer defaultMaxPages;

    private final ObjectMapper objectMapper;

    public PdfParserTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 PDF，返回标题/页码/文本内容等结构化信息
     */
    @Tool(
            name = "parsePdf",
            description = "解析PDF内容，支持本地文件路径或http/https链接，返回页码和文本信息。",
            returnDirect = false
    )
    public String parsePdf(
            @ToolParam(description = "PDF来源：本地路径或http/https链接") String source,
            @ToolParam(description = "最大文本长度，默认8000，范围1000-50000", required = false) Integer maxTextLength,
            @ToolParam(description = "最大返回页数，默认20，范围1-200", required = false) Integer maxPages) {

        if (!StringUtils.hasText(source)) {
            return error("source 不能为空");
        }

        int finalMaxTextLength = normalizeMaxTextLength(maxTextLength);
        int finalMaxPages = normalizeMaxPages(maxPages);
        String rawSource = source.trim();

        try {
            Resource resource = resolveResource(rawSource);
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            List<Document> docs = reader.get();

            if (docs == null || docs.isEmpty()) {
                return error("PDF解析结果为空");
            }

            List<Map<String, Object>> pages = new ArrayList<>();
            StringBuilder combinedText = new StringBuilder();
            int countedPages = 0;

            for (Document doc : docs) {
                if (countedPages >= finalMaxPages) {
                    break;
                }

                String text = safeTrim(doc.getText());
                Map<String, Object> metadata = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
                Integer startPage = toInteger(metadata.get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER));
                Integer endPage = toInteger(metadata.get(PagePdfDocumentReader.METADATA_END_PAGE_NUMBER));

                Map<String, Object> pageItem = new LinkedHashMap<>();
                pageItem.put("startPage", startPage);
                pageItem.put("endPage", endPage);
                pageItem.put("text", text);
                pages.add(pageItem);

                if (combinedText.length() < finalMaxTextLength && StringUtils.hasText(text)) {
                    int remain = finalMaxTextLength - combinedText.length();
                    if (text.length() <= remain) {
                        combinedText.append(text).append("\n");
                    } else {
                        combinedText.append(text, 0, Math.max(remain, 0));
                    }
                }

                countedPages++;
            }

            String content = combinedText.toString().trim();
            if (content.length() > finalMaxTextLength) {
                content = content.substring(0, finalMaxTextLength);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("source", rawSource);
            result.put("sourceType", isHttpUrl(rawSource) ? "url" : "file");
            result.put("pageCount", pages.size());
            result.put("maxPagesApplied", finalMaxPages);
            result.put("maxTextLengthApplied", finalMaxTextLength);
            result.put("content", content);
            result.put("pages", pages);
            result.put("extractedAt", LocalDateTime.now().toString());
            return toJson(result);
        } catch (Exception e) {
            log.error("PDF解析失败: source={}, error={}", rawSource, e.getMessage(), e);
            return error("PDF解析失败: " + e.getMessage());
        }
    }

    private Resource resolveResource(String source) throws Exception {
        if (isHttpUrl(source)) {
            if (!isSafeUrl(source)) {
                throw new IllegalArgumentException("URL 不安全或不受支持，仅允许公网 http/https 地址");
            }
            return downloadPdfAsResource(source);
        }
        return resolveLocalPdf(source);
    }

    private Resource resolveLocalPdf(String source) throws Exception {
        Path path = Paths.get(source).normalize();
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("本地文件不存在: " + path);
        }
        if (!path.toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("仅支持 .pdf 文件");
        }
        return new FileSystemResource(path);
    }

    private Resource downloadPdfAsResource(String url) {
        RestTemplate restTemplate = createRestTemplate();
        byte[] bytes = restTemplate.getForObject(url, byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("下载PDF失败，内容为空");
        }
        String fileName = extractFileName(url);
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    private RestTemplate createRestTemplate() {
        int finalTimeout = timeoutMs != null && timeoutMs > 0 ? timeoutMs : 10000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(finalTimeout);
        factory.setReadTimeout(finalTimeout);
        return new RestTemplate(factory);
    }

    private boolean isHttpUrl(String source) {
        String lower = source.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
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

    private String extractFileName(String url) {
        try {
            String path = URI.create(url).getPath();
            if (!StringUtils.hasText(path)) {
                return "remote.pdf";
            }
            String fileName = Paths.get(path).getFileName().toString();
            return StringUtils.hasText(fileName) ? fileName : "remote.pdf";
        } catch (Exception e) {
            return "remote.pdf";
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

    private int normalizeMaxPages(Integer maxPages) {
        int fallback = defaultMaxPages != null ? defaultMaxPages : DEFAULT_MAX_PAGES;
        int value = maxPages == null ? fallback : maxPages;
        if (value < MIN_MAX_PAGES) {
            return MIN_MAX_PAGES;
        }
        if (value > MAX_MAX_PAGES) {
            return MAX_MAX_PAGES;
        }
        return value;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
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
