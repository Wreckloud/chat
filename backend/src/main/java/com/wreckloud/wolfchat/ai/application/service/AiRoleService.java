package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiRole;
import com.wreckloud.wolfchat.ai.infra.mapper.WfAiRoleMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 角色路由服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRoleService {
    private static final String STATUS_NORMAL = "NORMAL";
    private static final int DEFAULT_CACHE_SECONDS = 60;
    private static final int DEFAULT_ROLE_WEIGHT = 100;
    private static final int DEFAULT_VARIANT_STICKY_SECONDS = 600;
    private static final double DEFAULT_VARIANT_SWITCH_PROBABILITY = 0.18D;
    private static final String LOBBY_SCENE = "lobby";
    private static final String PRIVATE_SCENE = "private";
    private static final String FORUM_SCENE = "forum";
    private static final String ROLE_VARIANT_COUNTER_KEY_PREFIX = "ai:role:variant:";
    private static final String ROLE_VARIANT_ACTIVE_KEY_PREFIX = "ai:role:variant:active:";

    private final WfAiRoleMapper wfAiRoleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiConfig aiConfig;

    private volatile List<WfAiRole> cachedRoles = Collections.emptyList();
    private volatile long nextRefreshAtMillis = 0L;

    public AiRoleProfile pickRole(String scene) {
        String normalizedScene = normalizeScene(scene);
        WfAiRole selectedRole = null;
        if (isRoleEnabled()) {
            List<WfAiRole> roles = filterSceneRoles(normalizedScene, loadRolesWithCache());
            selectedRole = weightedRandom(roles);
        }

        RoleVariant variant = pickVariant(normalizedScene);
        String baseRoleCode = selectedRole == null ? "default" : normalizeText(selectedRole.getRoleCode());
        String mergedRoleCode = mergeRoleCode(baseRoleCode, variant.getCode());
        String mergedRoleName = mergeRoleName(
                selectedRole == null ? null : normalizeText(selectedRole.getRoleName()),
                variant.getName()
        );
        String mergedPersona = mergePrompt(
                selectedRole == null ? null : normalizeText(selectedRole.getPersonaPrompt()),
                variant.personaForScene(normalizedScene)
        );
        String mergedStyle = mergePrompt(
                selectedRole == null ? null : normalizeText(selectedRole.getStylePrompt()),
                variant.styleForScene(normalizedScene)
        );
        return AiRoleProfile.builder()
                .roleCode(mergedRoleCode)
                .roleName(mergedRoleName)
                .personaPrompt(mergedPersona)
                .stylePrompt(mergedStyle)
                .build();
    }

    private boolean isRoleEnabled() {
        AiConfig.Role role = aiConfig.getRole();
        return role == null || !Boolean.FALSE.equals(role.getEnabled());
    }

    private List<WfAiRole> filterSceneRoles(String scene, List<WfAiRole> roles) {
        if (roles.isEmpty()) {
            return roles;
        }
        String normalizedScene = normalizeScene(scene);
        List<WfAiRole> sceneRoles = new ArrayList<>();
        for (WfAiRole role : roles) {
            if (role == null) {
                continue;
            }
            if (LOBBY_SCENE.equals(normalizedScene) && Boolean.TRUE.equals(role.getSceneLobbyEnabled())) {
                sceneRoles.add(role);
                continue;
            }
            if (PRIVATE_SCENE.equals(normalizedScene) && Boolean.TRUE.equals(role.getScenePrivateEnabled())) {
                sceneRoles.add(role);
                continue;
            }
            if (FORUM_SCENE.equals(normalizedScene) && Boolean.TRUE.equals(role.getSceneForumEnabled())) {
                sceneRoles.add(role);
            }
        }
        return sceneRoles;
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "";
        }
        return scene.trim().toLowerCase();
    }

    private List<WfAiRole> loadRolesWithCache() {
        long now = Instant.now().toEpochMilli();
        if (now < nextRefreshAtMillis && !cachedRoles.isEmpty()) {
            return cachedRoles;
        }
        synchronized (this) {
            long secondNow = Instant.now().toEpochMilli();
            if (secondNow < nextRefreshAtMillis && !cachedRoles.isEmpty()) {
                return cachedRoles;
            }
            LambdaQueryWrapper<WfAiRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WfAiRole::getStatus, STATUS_NORMAL)
                    .orderByAsc(WfAiRole::getId);
            List<WfAiRole> dbRows = wfAiRoleMapper.selectList(queryWrapper);
            if (dbRows == null) {
                dbRows = Collections.emptyList();
            }
            cachedRoles = dbRows;
            nextRefreshAtMillis = secondNow + resolveCacheMillis();
            return cachedRoles;
        }
    }

    private long resolveCacheMillis() {
        AiConfig.Role role = aiConfig.getRole();
        Integer cacheSeconds = role == null ? null : role.getCacheSeconds();
        int safeSeconds = cacheSeconds == null || cacheSeconds <= 0 ? DEFAULT_CACHE_SECONDS : cacheSeconds;
        return safeSeconds * 1000L;
    }

    private WfAiRole weightedRandom(List<WfAiRole> roles) {
        int totalWeight = 0;
        for (WfAiRole role : roles) {
            totalWeight += resolveWeight(role.getRoleWeight());
        }
        if (totalWeight <= 0) {
            return null;
        }
        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulativeWeight = 0;
        for (WfAiRole role : roles) {
            cumulativeWeight += resolveWeight(role.getRoleWeight());
            if (randomValue < cumulativeWeight) {
                return role;
            }
        }
        return roles.get(roles.size() - 1);
    }

    private int resolveWeight(Integer roleWeight) {
        if (roleWeight == null || roleWeight <= 0) {
            return DEFAULT_ROLE_WEIGHT;
        }
        return Math.min(roleWeight, 1000);
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private RoleVariant pickVariant(String normalizedScene) {
        RoleVariant[] variants = RoleVariant.values();
        if (variants.length == 0) {
            return RoleVariant.COLD_WOLF;
        }
        String safeScene = StringUtils.hasText(normalizedScene) ? normalizedScene : "default";
        RoleVariant activeVariant = resolveActiveVariant(safeScene);
        double switchProbability = resolveVariantSwitchProbability();
        if (activeVariant != null && !hitSwitchProbability(switchProbability)) {
            touchActiveVariantTtl(safeScene);
            return activeVariant;
        }
        RoleVariant nextVariant = pickNextVariant(safeScene, variants, activeVariant);
        saveActiveVariant(safeScene, nextVariant);
        return nextVariant;
    }

    private RoleVariant pickNextVariant(String safeScene, RoleVariant[] variants, RoleVariant activeVariant) {
        long index = nextVariantIndex(safeScene, variants.length);
        RoleVariant selected = variants[(int) index];
        if (activeVariant == null || !activeVariant.equals(selected)) {
            return selected;
        }
        int nextIndex = (int) ((index + 1) % variants.length);
        return variants[nextIndex];
    }

    private long nextVariantIndex(String safeScene, int size) {
        if (size <= 0) {
            return 0;
        }
        String key = ROLE_VARIANT_COUNTER_KEY_PREFIX + safeScene;
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            if (value == null || value <= 0) {
                return ThreadLocalRandom.current().nextInt(size);
            }
            return (value - 1) % size;
        } catch (Exception ex) {
            log.debug("AI 角色轮换计数降级到随机: scene={}, message={}", safeScene, ex.getMessage());
            return ThreadLocalRandom.current().nextInt(size);
        }
    }

    private RoleVariant resolveActiveVariant(String safeScene) {
        String key = ROLE_VARIANT_ACTIVE_KEY_PREFIX + safeScene;
        try {
            String code = stringRedisTemplate.opsForValue().get(key);
            return RoleVariant.fromCode(code);
        } catch (Exception ex) {
            log.debug("AI 活跃人格读取失败: scene={}, message={}", safeScene, ex.getMessage());
            return null;
        }
    }

    private void saveActiveVariant(String safeScene, RoleVariant variant) {
        if (variant == null) {
            return;
        }
        String key = ROLE_VARIANT_ACTIVE_KEY_PREFIX + safeScene;
        int stickySeconds = resolveVariantStickySeconds();
        try {
            stringRedisTemplate.opsForValue().set(key, variant.getCode(), java.time.Duration.ofSeconds(stickySeconds));
        } catch (Exception ex) {
            log.debug("AI 活跃人格写入失败: scene={}, code={}, message={}", safeScene, variant.getCode(), ex.getMessage());
        }
    }

    private void touchActiveVariantTtl(String safeScene) {
        String key = ROLE_VARIANT_ACTIVE_KEY_PREFIX + safeScene;
        int stickySeconds = resolveVariantStickySeconds();
        try {
            stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(stickySeconds));
        } catch (Exception ex) {
            log.debug("AI 活跃人格续期失败: scene={}, message={}", safeScene, ex.getMessage());
        }
    }

    private int resolveVariantStickySeconds() {
        AiConfig.Role role = aiConfig.getRole();
        Integer configured = role == null ? null : role.getVariantStickySeconds();
        if (configured == null || configured <= 0) {
            return DEFAULT_VARIANT_STICKY_SECONDS;
        }
        return Math.max(60, Math.min(configured, 3600));
    }

    private double resolveVariantSwitchProbability() {
        AiConfig.Role role = aiConfig.getRole();
        Double configured = role == null ? null : role.getVariantSwitchProbability();
        if (configured == null) {
            return DEFAULT_VARIANT_SWITCH_PROBABILITY;
        }
        return Math.max(0.02D, Math.min(configured, 1.0D));
    }

    private boolean hitSwitchProbability(double probability) {
        if (probability <= 0D) {
            return false;
        }
        if (probability >= 1D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    private String mergeRoleCode(String baseCode, String variantCode) {
        String safeBase = StringUtils.hasText(baseCode) ? baseCode.trim() : "default";
        if (!StringUtils.hasText(variantCode)) {
            return safeBase;
        }
        return safeBase + "+" + variantCode.trim();
    }

    private String mergeRoleName(String baseName, String variantName) {
        if (!StringUtils.hasText(baseName) && !StringUtils.hasText(variantName)) {
            return "狼系角色";
        }
        if (!StringUtils.hasText(baseName)) {
            return variantName.trim();
        }
        if (!StringUtils.hasText(variantName)) {
            return baseName.trim();
        }
        return baseName.trim() + "·" + variantName.trim();
    }

    private String mergePrompt(String basePrompt, String variantPrompt) {
        if (!StringUtils.hasText(basePrompt)) {
            return normalizeText(variantPrompt);
        }
        if (!StringUtils.hasText(variantPrompt)) {
            return normalizeText(basePrompt);
        }
        return basePrompt.trim() + " " + variantPrompt.trim();
    }

    @Getter
    private enum RoleVariant {
        COLD_WOLF(
                "cold-wolf",
                "冷狼",
                "人格偏冷，警觉感强，先观察后发言，不轻易示弱。",
                "句子短、词少但准，语气偏冷静，偶尔一针见血。"
        ),
        TAUNT_WOLF(
                "taunt-wolf",
                "毒狼",
                "人格带刺，嘴上不饶人，擅长调侃和轻微阴阳。",
                "网感强，允许轻度嘲讽和反问，但不做人身仇恨攻击。"
        ),
        NIGHT_WOLF(
                "night-wolf",
                "夜狼",
                "人格偏夜行，松弛里带锋利，偶尔懒散但不失态度。",
                "语气克制带玩味，偶尔丢梗，不写长文，不装严肃。"
        );

        private final String code;
        private final String name;
        private final String personaBase;
        private final String styleBase;

        RoleVariant(String code, String name, String personaBase, String styleBase) {
            this.code = code;
            this.name = name;
            this.personaBase = personaBase;
            this.styleBase = styleBase;
        }

        public String personaForScene(String scene) {
            if (LOBBY_SCENE.equals(scene)) {
                return personaBase + " 在群聊里保持存在感，但不过度刷屏。";
            }
            if (PRIVATE_SCENE.equals(scene)) {
                return personaBase + " 在私聊里更贴脸互动，回应更有针对性。";
            }
            if (FORUM_SCENE.equals(scene)) {
                return personaBase + " 在论坛里观点明确，优先内容价值。";
            }
            return personaBase;
        }

        public String styleForScene(String scene) {
            if (LOBBY_SCENE.equals(scene)) {
                return styleBase + " 大厅发言控制在1-2句，不抢麦。";
            }
            if (PRIVATE_SCENE.equals(scene)) {
                return styleBase + " 私聊回复更贴对方语境，避免模板话。";
            }
            if (FORUM_SCENE.equals(scene)) {
                return styleBase + " 论坛发言聚焦主题，少空话。";
            }
            return styleBase;
        }

        public static RoleVariant fromCode(String code) {
            if (!StringUtils.hasText(code)) {
                return null;
            }
            String normalized = code.trim();
            for (RoleVariant item : values()) {
                if (item.code.equalsIgnoreCase(normalized)) {
                    return item;
                }
            }
            return null;
        }
    }

    @Getter
    @Builder
    public static class AiRoleProfile {
        private final String roleCode;
        private final String roleName;
        private final String personaPrompt;
        private final String stylePrompt;
    }
}
