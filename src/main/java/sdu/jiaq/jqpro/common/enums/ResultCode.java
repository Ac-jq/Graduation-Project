package sdu.jiaq.jqpro.common.enums;

import lombok.Getter;

/**
 * 统一响应状态码枚举。
 * code 用于前后端业务约定，httpStatus 用于设置 HTTP 响应状态。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, 200, "操作成功"),
    BAD_REQUEST(400, 400, "请求参数错误"),
    UNAUTHORIZED(401, 401, "未登录或登录已失效"),
    FORBIDDEN(403, 403, "无权访问"),
    NOT_FOUND(404, 404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, 405, "请求方法不支持"),
    VALIDATE_FAILED(422, 422, "请求参数校验失败"),
    BUSINESS_ERROR(600, 400, "业务处理失败"),
    SYSTEM_ERROR(500, 500, "系统繁忙，请稍后再试");

    private final int code;
    private final int httpStatus;
    private final String message;

    ResultCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
