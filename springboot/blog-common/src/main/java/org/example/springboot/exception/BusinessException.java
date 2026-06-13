package org.example.springboot.exception;

import lombok.Getter;

/**
 * 业务异常类
 * @author system
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String code;
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.message = message;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
        this.message = message;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
} 