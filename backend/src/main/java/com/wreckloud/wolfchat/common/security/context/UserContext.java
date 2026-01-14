package com.wreckloud.wolfchat.common.security.context;

/**
 * @Description 用户上下文，用于存储当前登录行者的信息
 * @Author Wreckloud
 * @Date 2026-01-06
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> WOLF_NO = new ThreadLocal<>();

    /**
     * 设置行者ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取行者ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 设置狼藉号
     */
    public static void setWolfNo(String wolfNo) {
        WOLF_NO.set(wolfNo);
    }

    /**
     * 获取狼藉号
     */
    public static String getWolfNo() {
        return WOLF_NO.get();
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        USER_ID.remove();
        WOLF_NO.remove();
    }
}


