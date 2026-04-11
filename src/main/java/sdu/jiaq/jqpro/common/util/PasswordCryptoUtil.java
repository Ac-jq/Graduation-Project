package sdu.jiaq.jqpro.common.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.HexFormat;

/**
 * 基于 Bouncy Castle 的密码摘要工具。
 * 当前使用 SHA3-256 + 随机盐实现密码摘要，满足本项目的本地开发与演示要求。
 */
public final class PasswordCryptoUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private PasswordCryptoUtil() {
    }

    public static String generateSalt() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HEX_FORMAT.formatHex(bytes);
    }

    public static String hashPassword(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256", BouncyCastleProvider.PROVIDER_NAME);
            byte[] value = (salt + ":" + rawPassword).getBytes(StandardCharsets.UTF_8);
            return HEX_FORMAT.formatHex(digest.digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException("密码摘要失败", exception);
        }
    }

    public static boolean matches(String rawPassword, String salt, String storedHash) {
        return hashPassword(rawPassword, salt).equalsIgnoreCase(storedHash);
    }
}
