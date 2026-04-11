package sdu.jiaq.jqpro.common.util;

import cn.dev33.satoken.stp.StpUtil;
import sdu.jiaq.jqpro.common.exception.BusinessException;

/**
 * 登录态工具。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException("当前未登录");
        }
        return Long.parseLong(String.valueOf(loginId));
    }
}
