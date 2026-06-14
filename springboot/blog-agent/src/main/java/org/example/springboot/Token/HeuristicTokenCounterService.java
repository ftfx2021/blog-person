package org.example.springboot.Token;


import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 轻量级token估算服务(Heuristic:启发式的)
 */
@Service
public class HeuristicTokenCounterService implements TokenCounterService{
    /*  字符类型        判断条件        估算规则
        ASCII          ch <= 0x7F   约4个字符一个token
        CJK            isCjk(ch)    约 1 个字符 ≈ 1 Token
        其它            以上都不是    约 2 个字符 ≈ 1 Token
        空白                        不计

        英文/ASCII：多个字符合并成 1 个 token（压缩率高）
        CJK = CJK = Chinese + Japanese + Korean 的首字母缩写，中日韩文字，在 Token 估算中需要单独处理，因为它们比英文消耗更多 token。
     */
    @Override
    public Integer countTokens(String text) {
        if(!StringUtils.isNotEmpty(text)){
            return 0;
        }
        
        int asciiCount=0;
        int cjkCount =0;
        int otherCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if(Character.isWhitespace(ch)){
                continue;
            }
            if(ch <= 0x7F){
                asciiCount++;
            } else if (isCjk(ch)) {
                cjkCount++;
            }else {
                otherCount++;
            }
        }

        int asciiTokens = (asciiCount + 3)/4;
        int otherTokens = (otherCount + 1) /2;
        int total = asciiTokens + cjkCount +otherTokens;
        return Math.max(total,1);
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
                || block == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }
}
