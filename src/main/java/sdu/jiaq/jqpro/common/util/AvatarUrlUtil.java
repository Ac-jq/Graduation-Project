package sdu.jiaq.jqpro.common.util;

import org.springframework.util.StringUtils;

/**
 * 头像访问路径工具，统一将数据库中的相对路径转换为前端可直接访问的完整 URL。
 */
public final class AvatarUrlUtil {

    private static final String BACKEND_ORIGIN = "http://127.0.0.1:8080";

    private AvatarUrlUtil() {
    }

    public static String toPublicUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return avatarUrl;
        }
        String trimmedAvatarUrl = avatarUrl.trim();
        if (trimmedAvatarUrl.startsWith("http://") || trimmedAvatarUrl.startsWith("https://")) {
            return trimmedAvatarUrl;
        }
        if (trimmedAvatarUrl.startsWith("/")) {
            return BACKEND_ORIGIN + trimmedAvatarUrl;
        }
        return BACKEND_ORIGIN + "/" + trimmedAvatarUrl;
    }
}
