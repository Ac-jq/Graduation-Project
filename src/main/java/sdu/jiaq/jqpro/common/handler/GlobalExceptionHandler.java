package sdu.jiaq.jqpro.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sdu.jiaq.jqpro.common.enums.ResultCode;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.result.Result;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，负责把不同异常统一转换成 Result 响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception, HttpServletResponse response) {
        response.setStatus(exception.getHttpStatus());
        log.warn("业务异常: {}", exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                              HttpServletResponse response) {
        response.setStatus(ResultCode.VALIDATE_FAILED.getHttpStatus());
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception, HttpServletResponse response) {
        response.setStatus(ResultCode.VALIDATE_FAILED.getHttpStatus());
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception,
                                                           HttpServletResponse response) {
        response.setStatus(ResultCode.VALIDATE_FAILED.getHttpStatus());
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception,
                                                                      HttpServletResponse response) {
        response.setStatus(ResultCode.BAD_REQUEST.getHttpStatus());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), exception.getParameterName() + " 参数不能为空");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception,
                                                                     HttpServletResponse response) {
        response.setStatus(ResultCode.METHOD_NOT_ALLOWED.getHttpStatus());
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED.getCode(), exception.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException exception, HttpServletResponse response) {
        response.setStatus(ResultCode.UNAUTHORIZED.getHttpStatus());
        return Result.fail(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handlePermissionException(Exception exception, HttpServletResponse response) {
        response.setStatus(ResultCode.FORBIDDEN.getHttpStatus());
        return Result.fail(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception, HttpServletResponse response) {
        response.setStatus(ResultCode.SYSTEM_ERROR.getHttpStatus());
        log.error("系统异常", exception);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
