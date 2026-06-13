package org.example.springboot.http;

import lombok.Getter;

/*
 * 结构化异常
 */
@Getter
public class ModelClientException extends RuntimeException {
    //错误分类
    private final ModelClientErrorType errorType;
    //HTTP 状态码，网络错误时为 null
    private final Integer statusCode;

    public ModelClientException(String message, ModelClientErrorType errorType,
                                Integer statusCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    public ModelClientException(String message, ModelClientErrorType errorType,
                                Integer statusCode) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }
}