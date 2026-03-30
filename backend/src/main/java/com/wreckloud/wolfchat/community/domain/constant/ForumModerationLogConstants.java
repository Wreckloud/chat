package com.wreckloud.wolfchat.community.domain.constant;

/**
 * 版务日志常量，统一社区端与管理端的日志语义。
 */
public final class ForumModerationLogConstants {
    public static final String TARGET_THREAD = "THREAD";
    public static final String TARGET_REPLY = "REPLY";
    public static final String TARGET_LOBBY_MESSAGE = "LOBBY_MESSAGE";

    public static final String ACTION_LOCK_THREAD = "LOCK_THREAD";
    public static final String ACTION_UNLOCK_THREAD = "UNLOCK_THREAD";
    public static final String ACTION_STICKY_THREAD = "STICKY_THREAD";
    public static final String ACTION_UNSTICKY_THREAD = "UNSTICKY_THREAD";
    public static final String ACTION_ESSENCE_THREAD = "ESSENCE_THREAD";
    public static final String ACTION_UNESSENCE_THREAD = "UNESSENCE_THREAD";
    public static final String ACTION_DELETE_THREAD = "DELETE_THREAD";
    public static final String ACTION_DELETE_REPLY = "DELETE_REPLY";
    public static final String ACTION_RECALL_LOBBY_MESSAGE = "RECALL_LOBBY_MESSAGE";

    public static final String REASON_AUTHOR_OPERATION = "AUTHOR_OPERATION";
    public static final String REASON_ADMIN_OPERATION = "ADMIN_OPERATION";

    private ForumModerationLogConstants() {
    }
}
