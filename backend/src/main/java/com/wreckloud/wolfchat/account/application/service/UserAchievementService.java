package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.api.vo.UserAchievementVO;
import com.wreckloud.wolfchat.account.api.vo.UserTitleVO;
import com.wreckloud.wolfchat.account.domain.entity.WfAchievement;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAchievement;
import com.wreckloud.wolfchat.account.infra.mapper.WfAchievementMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserAchievementMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description 用户成就与头衔服务
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Service
@RequiredArgsConstructor
public class UserAchievementService {
    public static final String ACHIEVEMENT_CODE_WOLF_CUB = "WOLF_CUB";
    public static final String ACHIEVEMENT_CODE_FIRST_POST = "FIRST_POST";
    public static final String ACHIEVEMENT_CODE_FIRST_REPLY = "FIRST_REPLY";
    public static final String ACHIEVEMENT_CODE_FIRST_FOLLOW = "FIRST_FOLLOW";

    private static final int USER_HOME_SHOWCASE_LIMIT = 3;

    private final WfAchievementMapper wfAchievementMapper;
    private final WfUserAchievementMapper wfUserAchievementMapper;
    private final WfUserMapper wfUserMapper;
    private final UserService userService;
    private final SessionUserService sessionUserService;
    private final UserNoticeService userNoticeService;

    /**
     * 获取当前用户成就列表
     */
    public List<UserAchievementVO> listMyAchievements(Long userId) {
        WfUser user = userService.getEnabledByIdOrThrow(userId);
        List<WfAchievement> achievements = listEnabledAchievements();
        if (achievements.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, WfUserAchievement> unlockedMap = listUserAchievementMap(userId);
        List<UserAchievementVO> list = new ArrayList<>(achievements.size());
        for (WfAchievement achievement : achievements) {
            WfUserAchievement unlocked = unlockedMap.get(achievement.getCode());
            UserAchievementVO vo = new UserAchievementVO();
            vo.setAchievementCode(achievement.getCode());
            vo.setName(achievement.getName());
            vo.setDescription(achievement.getDescription());
            vo.setTitleName(achievement.getTitleName());
            vo.setTitleColor(achievement.getTitleColor());
            vo.setUnlocked(unlocked != null);
            vo.setUnlockTime(unlocked == null ? null : unlocked.getUnlockTime());
            vo.setEquipped(achievement.getCode().equals(user.getEquippedTitleCode()));
            list.add(vo);
        }
        return list;
    }

    /**
     * 佩戴头衔
     */
    @Transactional(rollbackFor = Exception.class)
    public void equipTitle(Long userId, String achievementCode) {
        String normalizedCode = normalizeAchievementCode(achievementCode);
        userService.getEnabledByIdOrThrow(userId);
        WfAchievement achievement = getEnabledAchievementByCodeOrThrow(normalizedCode);
        if (!hasUnlockedAchievement(userId, normalizedCode)) {
            throw new BaseException(ErrorCode.ACHIEVEMENT_NOT_UNLOCKED);
        }
        updateEquippedTitle(userId, achievement.getCode(), achievement.getTitleName(), achievement.getTitleColor());
    }

    /**
     * 取消佩戴头衔
     */
    @Transactional(rollbackFor = Exception.class)
    public void unequipTitle(Long userId) {
        userService.getEnabledByIdOrThrow(userId);
        updateEquippedTitle(userId, null, null, null);
    }

    /**
     * 注册成功成就
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantRegisterAchievement(Long userId) {
        grantAchievement(userId, ACHIEVEMENT_CODE_WOLF_CUB, true);
    }

    /**
     * 首次发帖成就
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantFirstPostAchievement(Long userId) {
        grantAchievement(userId, ACHIEVEMENT_CODE_FIRST_POST, false);
    }

    /**
     * 首次回帖成就
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantFirstReplyAchievement(Long userId) {
        grantAchievement(userId, ACHIEVEMENT_CODE_FIRST_REPLY, false);
    }

    /**
     * 首次关注成就
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantFirstFollowAchievement(Long userId) {
        grantAchievement(userId, ACHIEVEMENT_CODE_FIRST_FOLLOW, false);
    }

    /**
     * 获取用户头衔橱窗（最多3个）
     */
    public List<UserTitleVO> listUserTitleShowcase(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        WfUser user = userService.getByIdOrThrow(userId);
        List<WfUserAchievement> unlockedList = listUserAchievementRecords(userId);
        if (unlockedList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, WfAchievement> achievementMap = listAchievementMapByCodes(
                unlockedList.stream().map(WfUserAchievement::getAchievementCode).collect(Collectors.toList())
        );
        if (achievementMap.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> showcaseCodes = new LinkedHashSet<>();
        if (StringUtils.hasText(user.getEquippedTitleCode()) && achievementMap.containsKey(user.getEquippedTitleCode())) {
            showcaseCodes.add(user.getEquippedTitleCode());
        }
        for (WfUserAchievement unlocked : unlockedList) {
            if (showcaseCodes.size() >= USER_HOME_SHOWCASE_LIMIT) {
                break;
            }
            String code = unlocked.getAchievementCode();
            if (achievementMap.containsKey(code)) {
                showcaseCodes.add(code);
            }
        }

        List<UserTitleVO> showcase = new ArrayList<>(USER_HOME_SHOWCASE_LIMIT);
        for (String code : showcaseCodes) {
            if (showcase.size() >= USER_HOME_SHOWCASE_LIMIT) {
                break;
            }
            WfAchievement achievement = achievementMap.get(code);
            if (achievement == null) {
                continue;
            }
            UserTitleVO vo = new UserTitleVO();
            vo.setAchievementCode(achievement.getCode());
            vo.setTitleName(achievement.getTitleName());
            vo.setTitleColor(achievement.getTitleColor());
            showcase.add(vo);
        }
        return showcase;
    }

    private void grantAchievement(Long userId, String achievementCode, boolean equipWhenEmpty) {
        userService.getEnabledByIdOrThrow(userId);
        WfAchievement achievement = getEnabledAchievementByCodeOrThrow(achievementCode);
        boolean unlockedNow = unlockAchievementIfAbsent(userId, achievementCode);
        if (unlockedNow) {
            userNoticeService.notifyAchievementUnlocked(userId,
                    achievement.getId(),
                    achievement.getName(),
                    achievement.getTitleName());
        }
        if (!equipWhenEmpty) {
            return;
        }

        WfUser user = wfUserMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        boolean shouldAutoEquip = !StringUtils.hasText(user.getEquippedTitleCode());
        if (!shouldAutoEquip) {
            return;
        }
        if (!unlockedNow && !hasUnlockedAchievement(userId, achievementCode)) {
            return;
        }
        updateEquippedTitle(userId, achievement.getCode(), achievement.getTitleName(), achievement.getTitleColor());
    }

    private List<WfAchievement> listEnabledAchievements() {
        LambdaQueryWrapper<WfAchievement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAchievement::getEnabled, true)
                .orderByAsc(WfAchievement::getSortNo)
                .orderByAsc(WfAchievement::getId);
        return wfAchievementMapper.selectList(queryWrapper);
    }

    private WfAchievement getEnabledAchievementByCodeOrThrow(String achievementCode) {
        LambdaQueryWrapper<WfAchievement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAchievement::getCode, achievementCode)
                .eq(WfAchievement::getEnabled, true)
                .last("LIMIT 1");
        WfAchievement achievement = wfAchievementMapper.selectOne(queryWrapper);
        if (achievement == null) {
            throw new BaseException(ErrorCode.ACHIEVEMENT_NOT_FOUND);
        }
        return achievement;
    }

    private String normalizeAchievementCode(String achievementCode) {
        if (!StringUtils.hasText(achievementCode)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        return achievementCode.trim().toUpperCase();
    }

    private Map<String, WfUserAchievement> listUserAchievementMap(Long userId) {
        List<WfUserAchievement> list = listUserAchievementRecords(userId);
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, WfUserAchievement> map = new HashMap<>();
        for (WfUserAchievement item : list) {
            map.put(item.getAchievementCode(), item);
        }
        return map;
    }

    private List<WfUserAchievement> listUserAchievementRecords(Long userId) {
        LambdaQueryWrapper<WfUserAchievement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAchievement::getUserId, userId)
                .orderByDesc(WfUserAchievement::getUnlockTime)
                .orderByDesc(WfUserAchievement::getId);
        return wfUserAchievementMapper.selectList(queryWrapper);
    }

    private boolean hasUnlockedAchievement(Long userId, String achievementCode) {
        LambdaQueryWrapper<WfUserAchievement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAchievement::getUserId, userId)
                .eq(WfUserAchievement::getAchievementCode, achievementCode)
                .last("LIMIT 1");
        return wfUserAchievementMapper.selectOne(queryWrapper) != null;
    }

    private boolean unlockAchievementIfAbsent(Long userId, String achievementCode) {
        WfUserAchievement record = new WfUserAchievement();
        record.setUserId(userId);
        record.setAchievementCode(achievementCode);
        record.setUnlockTime(LocalDateTime.now());
        try {
            return wfUserAchievementMapper.insert(record) == 1;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    private Map<String, WfAchievement> listAchievementMapByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> distinctCodes = codes.stream()
                .filter(StringUtils::hasText)
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .collect(Collectors.toList());
        if (distinctCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<WfAchievement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfAchievement::getCode, distinctCodes)
                .eq(WfAchievement::getEnabled, true);
        List<WfAchievement> achievements = wfAchievementMapper.selectList(queryWrapper);
        if (achievements.isEmpty()) {
            return Collections.emptyMap();
        }
        return achievements.stream().collect(Collectors.toMap(WfAchievement::getCode, item -> item, (left, right) -> left));
    }

    private void updateEquippedTitle(Long userId, String titleCode, String titleName, String titleColor) {
        LambdaUpdateWrapper<WfUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUser::getId, userId)
                .set(WfUser::getEquippedTitleCode, titleCode)
                .set(WfUser::getEquippedTitleName, titleName)
                .set(WfUser::getEquippedTitleColor, titleColor);
        int updateRows = wfUserMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        sessionUserService.invalidateUserCache(userId);
    }
}
