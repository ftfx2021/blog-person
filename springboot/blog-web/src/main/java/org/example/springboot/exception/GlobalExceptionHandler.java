package org.example.springboot.exception;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultCode;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理服务异常
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        log.error("服务异常: {}", e.getMessage(), e);
        // 避免直接暴露系统内部错误，返回友好的错误信息
        String userMessage = e.getMessage();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            userMessage = "服务暂时不可用，请稍后重试";
        }
        return Result.error(e.getCode(), userMessage);
    }

    /**
     * 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage(), e);
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), e.getMessage());
    }

    /**
     * 处理JWT Token过期异常
     */
    @ExceptionHandler(TokenExpiredException.class)
    public Result<?> handleTokenExpiredException(TokenExpiredException e) {
        log.warn("Token已过期: {}", e.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
    }

    /**
     * 处理JWT解码异常（Token格式错误）
     */
    @ExceptionHandler(JWTDecodeException.class)
    public Result<?> handleJWTDecodeException(JWTDecodeException e) {
        log.warn("Token格式错误: {}", e.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "Token格式错误，请重新登录");
    }

    /**
     * 处理JWT验证异常（Token无效）
     */
    @ExceptionHandler(JWTVerificationException.class)
    public Result<?> handleJWTVerificationException(JWTVerificationException e) {
        log.warn("Token验证失败: {}", e.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "Token验证失败，请重新登录");
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未处理的系统异常: {}", e.getMessage(), e);
        // 避免暴露系统内部错误信息给用户
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "服务暂时不可用，请稍后重试");
    }
} 