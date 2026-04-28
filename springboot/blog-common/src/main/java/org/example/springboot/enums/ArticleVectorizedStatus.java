package org.example.springboot.enums;

import lombok.Data;
import lombok.Getter;


public enum ArticleVectorizedStatus {
    NO(0,"待向量"),
    ON_GOING(1,"正在向量"),
    SUCCESS(2,"成功"),
    ERROR(-1,"失败");
    @Getter
    private  final Integer Code;
    private final String Desc;
     ArticleVectorizedStatus(Integer code, String desc) {
        this.Code = code;
        this.Desc = desc;
    };


}
