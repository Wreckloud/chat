package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiRole;
import com.wreckloud.wolfchat.ai.infra.mapper.WfAiRoleMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String LOBBY_SCENE = "lobby";
    private static final String PRIVATE_SCENE = "private";
    private static final String FORUM_SCENE = "forum";

    private final WfAiRoleMapper wfAiRoleMapper;
    private final AiConfig aiConfig;

    private volatile List<WfAiRole> cachedRoles = Collections.emptyList();
    private volatile long nextRefreshAtMillis = 0L;

    public AiRoleProfile pickRole(String scene) {
        if (!isRoleEnabled()) {
            return null;
        }
        List<WfAiRole> roles = filterSceneRoles(scene, loadRolesWithCache());
        if (roles.isEmpty()) {
            return null;
        }
        WfAiRole selected = weightedRandom(roles);
        if (selected == null) {
            return null;
        }
        return AiRoleProfile.builder()
                .roleCode(selected.getRoleCode())
                .roleName(selected.getRoleName())
                .personaPrompt(normalizeText(selected.getPersonaPrompt()))
                .stylePrompt(normalizeText(selected.getStylePrompt()))
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

    @Getter
    @Builder
    public static class AiRoleProfile {
        private final String roleCode;
        private final String roleName;
        private final String personaPrompt;
        private final String stylePrompt;
    }
}

