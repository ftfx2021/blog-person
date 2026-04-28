package org.example.springboot.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML内容处理服务
 * 负责处理文章HTML内容，添加锚点ID，生成大纲映射
 */
@Slf4j
@Service
public class HtmlContentProcessor {

    /**
     * 处理HTML内容，为标题添加锚点ID
     * @param htmlContent 原始HTML内容
     * @return 处理后的HTML内容
     */
    public String processHtmlContentWithAnchors(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements headings = doc.select("h1, h2, h3"); // 处理一级、二级和三级标题
            
            int index = 1;
            for (Element heading : headings) {
                String anchorId = "heading-" + index;
                heading.attr("id", anchorId);
                index++;
            }
            
            // 返回body内的HTML内容
            return doc.body().html();
            
        } catch (Exception e) {
            log.error("处理HTML内容添加锚点失败: {}", e.getMessage(), e);
            return htmlContent; // 出错时返回原内容
        }
    }

    /**
     * 从HTML内容提取大纲并生成锚点映射
     * @param htmlContent HTML内容
     * @return 大纲项列表（包含锚点ID）
     */
    public List<OutlineItemWithAnchor> extractOutlineWithAnchors(String htmlContent) {
        List<OutlineItemWithAnchor> outline = new ArrayList<>();
        
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return outline;
        }
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements headings = doc.select("h1, h2, h3"); // 处理一级、二级和三级标题
            
            int index = 1;
            for (Element heading : headings) {
                String anchorId = "heading-" + index;
                heading.attr("id", anchorId); // 同时添加ID
                
                OutlineItemWithAnchor item = new OutlineItemWithAnchor();
                item.setLevel(Integer.parseInt(heading.tagName().substring(1))); // h1->1, h2->2
                String title = heading.text().trim();
                
                // 限制标题长度不超过20个字符
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }
                
                item.setTitle(title);
                item.setAnchorId(anchorId);
                
                outline.add(item);
                index++;
            }
            
            log.info("从HTML内容提取大纲成功，共{}个标题", outline.size());
            
        } catch (Exception e) {
            log.error("从HTML内容提取大纲失败: {}", e.getMessage(), e);
        }
        
        return outline;
    }

    /**
     * 同步大纲JSON与HTML内容的锚点ID
     * 确保大纲数据中的锚点ID与HTML内容中的标题ID一致
     * @param outlineJson 大纲JSON字符串
     * @param htmlContent HTML内容
     * @return 更新后的大纲JSON
     */
    public String syncOutlineWithHtmlAnchors(String outlineJson, String htmlContent) {
        try {
            // 从HTML提取标题和锚点
            List<OutlineItemWithAnchor> htmlOutline = extractOutlineWithAnchors(htmlContent);
            
            if (htmlOutline.isEmpty()) {
                return outlineJson;
            }
            
            // 构建新的大纲JSON
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("[\n");
            
            for (int i = 0; i < htmlOutline.size(); i++) {
                OutlineItemWithAnchor item = htmlOutline.get(i);
                if (i > 0) {
                    jsonBuilder.append(",\n");
                }
                jsonBuilder.append("  {\n");
                jsonBuilder.append("    \"level\": ").append(item.getLevel()).append(",\n");
                jsonBuilder.append("    \"title\": \"").append(escapeJson(item.getTitle())).append("\",\n");
                jsonBuilder.append("    \"anchorId\": \"").append(item.getAnchorId()).append("\"\n");
                jsonBuilder.append("  }");
            }
            
            jsonBuilder.append("\n]");
            
            return jsonBuilder.toString();
            
        } catch (Exception e) {
            log.error("同步大纲与HTML锚点失败: {}", e.getMessage(), e);
            return outlineJson; // 出错时返回原大纲
        }
    }

    /**
     * JSON字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 大纲项（包含锚点ID）
     */
    public static class OutlineItemWithAnchor {
        private Integer level;
        private String title;
        private String anchorId;

        // getter/setter
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAnchorId() { return anchorId; }
        public void setAnchorId(String anchorId) { this.anchorId = anchorId; }
    }
}
