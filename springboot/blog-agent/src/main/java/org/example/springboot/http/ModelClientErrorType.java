package org.example.springboot.http;

/*
    错误分类
 */
public enum  ModelClientErrorType {
    UNAUTHORIZED,//认证失败
    RATE_LIMITED,//频率超限
    SERVER_ERROR,//服务端错误
    CLIENT_ERROR,//客户端错误
    NETWORK_ERROR,
    INVALID_RESPONSE,
    PROVIDER_ERROR;

    public static ModelClientErrorType fromHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return UNAUTHORIZED;
        }
        if (status == 429) {
            return RATE_LIMITED;
        }
        if (status >= 500) {
            return SERVER_ERROR;
        }
        return CLIENT_ERROR;
    }
}
