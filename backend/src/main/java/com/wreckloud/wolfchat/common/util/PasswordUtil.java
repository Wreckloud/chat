package com.wreckloud.wolfchat.common.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * @Description 密码工具类
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public class PasswordUtil {

    /**
     * 生成随机盐值
     *
     * @return 16位随机字符串
     */
    public static String generateSalt() {
        return RandomStringUtils.randomAlphanumeric(16);
    }

    /**
     * 密码加密（MD5 + 盐值）
     *
     * @param password 原始密码
     * @param salt     盐值
     * @return 加密后的密码
     */
    public static String encrypt(String password, String salt) {
        return DigestUtils.md5Hex(password + salt);
    }

    /**
     * 验证密码
     *
     * @param password       原始密码
     * @param salt           盐值
     * @param passwordHash   加密后的密码
     * @return true-密码正确，false-密码错误
     */
    public static boolean verify(String password, String salt, String passwordHash) {
        String encrypted = encrypt(password, salt);
        return encrypted.equals(passwordHash);
    }
}

