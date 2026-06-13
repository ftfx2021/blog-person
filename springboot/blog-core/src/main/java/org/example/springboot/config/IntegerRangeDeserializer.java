package org.example.springboot.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.data.domain.Range;

import java.io.IOException;

/**
 * 用于ES范围Integer序列化配置
 */
public class IntegerRangeDeserializer extends JsonDeserializer<Range<Integer>> {

    @Override
    public Range<Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode gteNode = node.get("gte");
        JsonNode lteNode = node.get("lte");
        JsonNode gtNode = node.get("gt");
        JsonNode ltNode = node.get("lt");

        Integer lower = null, upper = null;
        boolean lowerInclusive = true, upperInclusive = true;

        if (gteNode != null && !gteNode.isNull()) { lower = gteNode.asInt(); lowerInclusive = true; }
        else if (gtNode != null && !gtNode.isNull()) { lower = gtNode.asInt(); lowerInclusive = false; }

        if (lteNode != null && !lteNode.isNull()) { upper = lteNode.asInt(); upperInclusive = true; }
        else if (ltNode != null && !ltNode.isNull()) { upper = ltNode.asInt(); upperInclusive = false; }

        if (lower == null || upper == null) {
            throw MismatchedInputException.from(p, Range.class,
                    "Range 需要同时提供下界与上界（使用 gte/lte 或 gt/lt）");
        }

        if (lowerInclusive && upperInclusive) return Range.closed(lower, upper);
        if (!lowerInclusive && upperInclusive) return Range.leftOpen(lower, upper);
        if (lowerInclusive && !upperInclusive) return Range.rightOpen(lower, upper);
        return Range.open(lower, upper);
    }
}