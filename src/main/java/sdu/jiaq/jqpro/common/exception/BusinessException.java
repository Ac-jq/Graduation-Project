package sdu.jiaq.jqpro.common.exception;

import lombok.Getter;
import sdu.jiaq.jqpro.common.enums.ResultCode;

/**
 * 通用业务异常。
 * 所有业务错误统一抛出该异常，并交由全局异常处理器转换为标准响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final Integer httpStatus;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR, message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }
}
