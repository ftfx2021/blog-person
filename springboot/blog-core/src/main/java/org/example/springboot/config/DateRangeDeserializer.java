package org.example.springboot.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.data.domain.Range;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * 用于ES范围Date序列化配置
 */
public class DateRangeDeserializer extends JsonDeserializer<Range<Date>> {

    private static final String[] PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    @Override
    public Range<Date> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode gteNode = node.get("gte");
        JsonNode lteNode = node.get("lte");
        JsonNode gtNode = node.get("gt");
        JsonNode ltNode = node.get("lt");

        Date lower = null, upper = null;
        boolean lowerInclusive = true, upperInclusive = true;

        if (gteNode != null && !gteNode.isNull()) { lower = parseDateNode(gteNode, p); lowerInclusive = true; }
        else if (gtNode != null && !gtNode.isNull()) { lower = parseDateNode(gtNode, p); lowerInclusive = false; }

        if (lteNode != null && !lteNode.isNull()) { upper = parseDateNode(lteNode, p); upperInclusive = true; }
        else if (ltNode != null && !ltNode.isNull()) { upper = parseDateNode(ltNode, p); upperInclusive = false; }

        if (lower == null || upper == null) {
            throw MismatchedInputException.from(p, Range.class,
                    "Date Range 需要同时提供下界与上界（使用 gte/lte 或 gt/lt）");
        }

        if (lowerInclusive && upperInclusive) return Range.closed(lower, upper);
        if (!lowerInclusive && upperInclusive) return Range.leftOpen(lower, upper);
        if (lowerInclusive && !upperInclusive) return Range.rightOpen(lower, upper);
        return Range.open(lower, upper);
    }

    private Date parseDateNode(JsonNode node, JsonParser p) throws IOException {
        if (node.isNumber()) {
            // epoch millis
            return new Date(node.asLong());
        }
        if (node.isTextual()) {
            String text = node.asText().trim();
            for (String pattern : PATTERNS) {
                try {
                    return new SimpleDateFormat(pattern).parse(text);
                } catch (ParseException ignore) { }
            }
            throw MismatchedInputException.from(p, Date.class,
                    "无法解析日期: " + text + "，期望格式: yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd 或毫秒时间戳");
        }
        throw MismatchedInputException.from(p, Date.class, "不支持的日期类型: " + node);
    }
}