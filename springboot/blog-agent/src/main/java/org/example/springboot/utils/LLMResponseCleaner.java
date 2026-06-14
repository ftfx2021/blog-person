package org.example.springboot.utils;

import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * LLM 输出清理工具类
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class LLMResponseCleaner {
    //匹配开头的围栏。^ 锚定行首，``` 匹配三个反引号，[\\w-]* 匹配可选的语言标识（json、xml、markdown、c++ 中的连字符等），\\s*\\n? 匹配尾部空白和可选换行
    private static final Pattern LEADING_CODE_FENCE = Pattern.compile("^```[\\w-]*\\s*\\n?");
    //匹配结尾的围栏。\\n? 匹配可选的前导换行，``` 匹配三个反引号，\\s*$ 匹配尾部空白并锚定行尾
    private static final Pattern TRAILING_CODE_FENCE = Pattern.compile("\\n?```\\s*$");

    /**
     * 移除 Markdown 代码块围栏（例如 ```json ... ```）
     */
    public static String stripMarkdownCodeFence(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim();
        cleaned = LEADING_CODE_FENCE.matcher(cleaned).replaceFirst("");
        cleaned = TRAILING_CODE_FENCE.matcher(cleaned).replaceFirst("");
        return cleaned.trim();
    }
}
