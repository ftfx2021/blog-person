package org.example.springboot.Token;

import org.springframework.data.relational.core.sql.In;

public interface TokenCounterService {
    /**
     * 统计文本的 Token 数
     *
     * @param text 文本内容
     * @return Token 数（无法计算时返回 null）
     */
    Integer countTokens(String text);
}
