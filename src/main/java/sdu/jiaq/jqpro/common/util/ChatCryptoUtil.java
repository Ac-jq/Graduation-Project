package sdu.jiaq.jqpro.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 聊天消息加解密工具。
 */
public final class ChatCryptoUtil {

    private static final String PREFIX = "enc::";
    private static final byte[] KEY = Arrays.copyOf(
            SecureUtil.sha256("jqpro-consult-chat-secret").getBytes(StandardCharsets.UTF_8), 16);
    private static final AES AES = SecureUtil.aes(KEY);

    private ChatCryptoUtil() {
    }

    public static String encrypt(String plainText) {
        return AES.encryptHex(StrUtil.nullToEmpty(plainText), StandardCharsets.UTF_8);
    }

    public static String decrypt(String cipherText) {
        return AES.decryptStr(cipherText, StandardCharsets.UTF_8);
    }

    /**
     * Prefix encrypted payloads so newer high-sensitivity fields can distinguish
     * ciphertext from historical plaintext rows without schema changes.
     */
    public static String encryptWithPrefix(String plainText) {
        return PREFIX + encrypt(plainText);
    }

    /**
     * Backward-compatible reader: prefixed rows are decrypted, historical
     * plaintext is returned as-is, and malformed ciphertext degrades safely.
     */
    public static String decryptCompat(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (!value.startsWith(PREFIX)) {
            return value;
        }
        try {
            return decrypt(value.substring(PREFIX.length()));
        } catch (Exception ex) {
            return value;
        }
    }
}
