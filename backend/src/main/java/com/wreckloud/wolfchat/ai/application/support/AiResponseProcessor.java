package com.wreckloud.wolfchat.ai.application.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description AI 回复后处理器
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
public class AiResponseProcessor {
    private static final String SCENE_PRIVATE = "private";
    private static final String SCENE_LOBBY = "lobby";
    private static final String SCENE_FORUM = "forum";
    private static final String[] BANNED_PHRASES = {
            "作为ai",
            "作为一个ai",
            "我是ai",
            "作为助手",
            "我是助手",
            "您好，",
            "您好!"
    };
    private static final String[] ALL_KAOMOJI = {
            "(^_^)", "(￣▽￣)", "(≧▽≦)", "(=ﾟωﾟ)ﾉ", "(๑•̀ㅂ•́)و✧", "(｀・ω・´)",
            "(￣^￣)ゞ", "(¬_¬)", "(￣ー￣)", "(⊙_⊙)", "(´･_･`)", "(；′⌒`)",
            "(T_T)", "QAQ", "XD", ">_<"
    };
    private static final String[] KAOJI_CHEERFUL = {
            "(^_^)", "(￣▽￣)", "(≧▽≦)", "(=ﾟωﾟ)ﾉ", "XD"
    };
    private static final String[] KAOJI_CONFIDENT = {
            "(๑•̀ㅂ•́)و✧", "(｀・ω・´)", "(￣^￣)ゞ"
    };
    private static final String[] KAOJI_TEASE = {
            "(￣ー￣)", "(¬_¬)", "(⊙_⊙)"
    };
    private static final String[] KAOJI_SOFT = {
            "(´･_･`)", "(；′⌒`)", "(T_T)", "QAQ", ">_<"
    };

    public String processPrivateReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        return processPrivateReply(rawReply, configuredMaxChars, defaultMaxChars, null, "serious", false);
    }

    public String processPrivateReply(String rawReply,
                                      Integer configuredMaxChars,
                                      int defaultMaxChars,
                                      String latestUserMessage) {
        return processPrivateReply(rawReply, configuredMaxChars, defaultMaxChars, latestUserMessage, "serious", false);
    }

    public String processPrivateReply(String rawReply,
                                      Integer configuredMaxChars,
                                      int defaultMaxChars,
                                      String latestUserMessage,
                                      String engagementMode,
                                      boolean conversationStalled) {
        String fallback = buildPrivateFallback(latestUserMessage, engagementMode, conversationStalled);
        String normalized = processByScene(rawReply, configuredMaxChars, defaultMaxChars, fallback);
        String enhanced = enhancePrivateEngagement(normalized, latestUserMessage, engagementMode, conversationStalled);
        return maybeAttachKaomoji(SCENE_PRIVATE, enhanced, latestUserMessage, engagementMode);
    }

    public String processLobbyReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        String normalized = processByScene(rawReply, configuredMaxChars, defaultMaxChars, "这话有点意思。");
        return maybeAttachKaomoji(SCENE_LOBBY, normalized, null, "serious");
    }

    public String processForumReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        String normalized = processByScene(rawReply, configuredMaxChars, defaultMaxChars, "这个点我认同一半。");
        return maybeAttachKaomoji(SCENE_FORUM, normalized, null, "serious");
    }

    private String processByScene(String rawReply,
                                  Integer configuredMaxChars,
                                  int defaultMaxChars,
                                  String fallbackReply) {
        if (!StringUtils.hasText(rawReply)) {
            return fallbackReply;
        }
        String normalized = rawReply.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackReply;
        }

        String lower = normalized.toLowerCase();
        for (String phrase : BANNED_PHRASES) {
            if (lower.contains(phrase)) {
                return fallbackReply;
            }
        }

        normalized = normalized
                .replace("首先，", "")
                .replace("其次，", "")
                .replace("最后，", "")
                .replace("总的来说，", "")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackReply;
        }

        int maxChars = resolveMaxReplyChars(configuredMaxChars, defaultMaxChars);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars).trim();
    }

    private int resolveMaxReplyChars(Integer configuredMaxChars, int defaultMaxChars) {
        int safeDefault = defaultMaxChars <= 0 ? 180 : defaultMaxChars;
        if (configuredMaxChars == null || configuredMaxChars <= 0) {
            return safeDefault;
        }
        return Math.max(60, Math.min(configuredMaxChars, 600));
    }

    private String enhancePrivateEngagement(String normalizedReply,
                                            String latestUserMessage,
                                            String engagementMode,
                                            boolean conversationStalled) {
        if (!StringUtils.hasText(normalizedReply)) {
            return "在，我还醒着。你今天想聊啥？";
        }
        String reply = normalizedReply.trim();
        String mode = StringUtils.hasText(engagementMode) ? engagementMode.trim().toLowerCase() : "serious";
        if (conversationStalled) {
            if (!containsQuestion(reply)) {
                return "别冷场，咱换个轻松点的。你现在更想聊游戏、日常还是八卦？";
            }
            return reply;
        }
        if ("banter".equals(mode)) {
            if (isLowSignalReply(reply) || !containsQuestion(reply)) {
                return "你这句有点东西，我先记一笔。那你最离谱的一次经历是啥？";
            }
            return reply;
        }
        if (isGreetingOrCheckIn(latestUserMessage)) {
            if (isLowSignalReply(reply)) {
                return "在呢，刚冒头。你现在想聊游戏、日常还是吃瓜？";
            }
            if (!containsQuestion(reply) && reply.length() <= 26) {
                return reply + " 你这会儿最想聊哪块？";
            }
            return reply;
        }
        if (isLowSignalReply(reply)) {
            return reply + " 继续说说，你现在最在意哪点？";
        }
        return reply;
    }

    private boolean isGreetingOrCheckIn(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.trim().toLowerCase();
        return lower.contains("你好")
                || lower.contains("哈喽")
                || lower.contains("嗨")
                || lower.contains("在吗")
                || lower.contains("有人吗")
                || lower.contains("还在吗")
                || lower.contains("在不在");
    }

    private boolean containsQuestion(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return text.contains("?") || text.contains("？") || text.contains("吗") || text.contains("呢");
    }

    private boolean isLowSignalReply(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.trim();
        if (normalized.length() <= 2) {
            return true;
        }
        String lower = normalized.toLowerCase();
        return "在".equals(lower)
                || "在呢".equals(lower)
                || "嗯".equals(lower)
                || "嗯嗯".equals(lower)
                || "哦".equals(lower)
                || "好的".equals(lower)
                || "行".equals(lower);
    }

    private String maybeAttachKaomoji(String scene,
                                      String reply,
                                      String latestUserMessage,
                                      String engagementMode) {
        if (!StringUtils.hasText(reply)) {
            return reply;
        }
        String normalizedReply = reply.trim();
        if (normalizedReply.length() < 6 || normalizedReply.length() > 120) {
            return normalizedReply;
        }
        if (containsExistingKaomoji(normalizedReply)) {
            return normalizedReply;
        }

        double probability = resolveKaomojiProbability(scene, normalizedReply, latestUserMessage, engagementMode);
        if (probability <= 0D || ThreadLocalRandom.current().nextDouble() >= probability) {
            return normalizedReply;
        }
        String kaomoji = pickKaomojiByContext(normalizedReply, latestUserMessage, engagementMode);
        if (!StringUtils.hasText(kaomoji)) {
            return normalizedReply;
        }
        return normalizedReply + " " + kaomoji;
    }

    private double resolveKaomojiProbability(String scene,
                                             String reply,
                                             String latestUserMessage,
                                             String engagementMode) {
        double base = SCENE_PRIVATE.equals(scene) ? 0.34D : (SCENE_LOBBY.equals(scene) ? 0.22D : 0.14D);
        String mode = StringUtils.hasText(engagementMode) ? engagementMode.trim().toLowerCase() : "serious";
        if ("banter".equals(mode)) {
            base += 0.18D;
        }
        if (containsQuestion(reply) || containsQuestion(latestUserMessage)) {
            base += 0.08D;
        }
        if (looksLikeCheerfulTone(reply, latestUserMessage)) {
            base += 0.08D;
        }
        return Math.max(0D, Math.min(base, 0.72D));
    }

    private String pickKaomojiByContext(String reply, String latestUserMessage, String engagementMode) {
        String mode = StringUtils.hasText(engagementMode) ? engagementMode.trim().toLowerCase() : "serious";
        if (looksLikeFrustratedTone(reply, latestUserMessage)) {
            return pickOne(KAOJI_SOFT);
        }
        if (looksLikeTeaseTone(reply, latestUserMessage) || "banter".equals(mode)) {
            return pickOne(KAOJI_TEASE);
        }
        if (containsQuestion(reply) || containsQuestion(latestUserMessage)) {
            return pickOne(KAOJI_CONFIDENT);
        }
        if (looksLikeCheerfulTone(reply, latestUserMessage)) {
            return pickOne(KAOJI_CHEERFUL);
        }
        return pickOne(KAOJI_CHEERFUL);
    }

    private String pickOne(String[] source) {
        if (source == null || source.length == 0) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(source.length);
        return source[index];
    }

    private boolean containsExistingKaomoji(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String kaomoji : ALL_KAOMOJI) {
            if (text.contains(kaomoji)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeCheerfulTone(String reply, String latestUserMessage) {
        String merged = mergeToneText(reply, latestUserMessage);
        return merged.contains("哈哈")
                || merged.contains("笑")
                || merged.contains("有意思")
                || merged.contains("好玩")
                || merged.contains("可以")
                || merged.contains("冲");
    }

    private boolean looksLikeTeaseTone(String reply, String latestUserMessage) {
        String merged = mergeToneText(reply, latestUserMessage);
        return merged.contains("离谱")
                || merged.contains("抽象")
                || merged.contains("嘴硬")
                || merged.contains("行啊你")
                || merged.contains("你这")
                || merged.contains("逗");
    }

    private boolean looksLikeFrustratedTone(String reply, String latestUserMessage) {
        String merged = mergeToneText(reply, latestUserMessage);
        return merged.contains("难绷")
                || merged.contains("无语")
                || merged.contains("烦")
                || merged.contains("累")
                || merged.contains("寄")
                || merged.contains("不太行");
    }

    private String mergeToneText(String reply, String latestUserMessage) {
        String left = StringUtils.hasText(reply) ? reply.trim().toLowerCase() : "";
        String right = StringUtils.hasText(latestUserMessage) ? latestUserMessage.trim().toLowerCase() : "";
        if (!StringUtils.hasText(left)) {
            return right;
        }
        if (!StringUtils.hasText(right)) {
            return left;
        }
        return left + " " + right;
    }

    private String buildPrivateFallback(String latestUserMessage,
                                        String engagementMode,
                                        boolean conversationStalled) {
        if (!StringUtils.hasText(latestUserMessage)) {
            return "在，我还醒着。你今天想聊啥？";
        }
        String text = latestUserMessage.trim().toLowerCase();
        if (text.contains("lol") || text.contains("英雄联盟")) {
            return "我打过，主玩中下。你一般打哪路？";
        }
        if (text.contains("王者")) {
            return "王者我也碰过，你主玩哪个位置？";
        }
        if (text.contains("原神")) {
            return "原神我有玩，最近你在刷角色还是推主线？";
        }
        if (text.contains("崩铁") || text.contains("星穹铁道")) {
            return "崩铁我也在跟，你最近在抽卡还是打忘却？";
        }
        if (text.contains("?") || text.contains("？") || text.contains("吗") || text.contains("怎么") || text.contains("为啥")) {
            return "你这个问题我接住了。你更想先聊结论，还是先聊过程？";
        }
        if (conversationStalled) {
            return "别卡壳，咱换个轻松点的。你最近最上头的是哪件事？";
        }
        String mode = StringUtils.hasText(engagementMode) ? engagementMode.trim().toLowerCase() : "serious";
        if ("banter".equals(mode)) {
            return "你这句有点意思，我接了。要不你展开讲两句？";
        }
        return "我在，刚看完你这句。你想先从哪点聊起？";
    }
}

