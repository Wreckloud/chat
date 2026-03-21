package com.wreckloud.wolfchat.follow.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.application.service.UserAchievementService;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.message.application.service.ChatSystemNoticeService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.domain.enums.FollowStatus;
import com.wreckloud.wolfchat.follow.api.vo.FollowUserVO;
import com.wreckloud.wolfchat.follow.application.event.UserFollowedEvent;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import com.wreckloud.wolfchat.follow.infra.mapper.WfFollowMapper;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description 关注关系服务
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Service
@RequiredArgsConstructor
public class FollowService {
    private final WfFollowMapper wfFollowMapper;
    private final UserService userService;
    private final UserAchievementService userAchievementService;
    private final UserNoticeService userNoticeService;
    private final ConversationService conversationService;
    private final ChatSystemNoticeService chatSystemNoticeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 关注行者
     */
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BaseException(ErrorCode.FOLLOW_SELF);
        }

        userService.getEnabledByIdOrThrow(followeeId);

        WfFollow follow = findFollowRecord(followerId, followeeId);

        if (follow != null) {
            if (FollowStatus.FOLLOWING.equals(follow.getStatus())) {
                throw new BaseException(ErrorCode.FOLLOW_ALREADY);
            }
            updateFollowStatusById(follow.getId(), FollowStatus.FOLLOWING);
            userAchievementService.grantFirstFollowAchievement(followerId);
            userNoticeService.notifyFollowReceived(followeeId);
            notifyChatFollowEvents(followerId, followeeId);
            applicationEventPublisher.publishEvent(new UserFollowedEvent(followerId, followeeId));
            return;
        }

        WfFollow newFollow = new WfFollow();
        newFollow.setFollowerId(followerId);
        newFollow.setFolloweeId(followeeId);
        newFollow.setStatus(FollowStatus.FOLLOWING);
        int insertRows = wfFollowMapper.insert(newFollow);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        userAchievementService.grantFirstFollowAchievement(followerId);
        userNoticeService.notifyFollowReceived(followeeId);
        notifyChatFollowEvents(followerId, followeeId);
        applicationEventPublisher.publishEvent(new UserFollowedEvent(followerId, followeeId));
    }

    /**
     * 取消关注
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followeeId) {
        WfFollow follow = findActiveFollowRecord(followerId, followeeId);
        if (follow == null) {
            throw new BaseException(ErrorCode.FOLLOW_NOT_FOUND);
        }
        updateFollowStatusById(follow.getId(), FollowStatus.UNFOLLOWED);
    }

    /**
     * 获取关注列表
     */
    public List<FollowUserVO> getFollowing(Long userId) {
        List<WfFollow> following = listActiveFollowRecordsByFollower(userId);
        Set<Long> followerSet = getFollowerIds(userId);
        List<Long> userIds = following.stream()
                .map(WfFollow::getFolloweeId)
                .collect(Collectors.toList());
        return buildFollowUsers(userIds, followerSet);
    }

    /**
     * 获取粉丝列表
     */
    public List<FollowUserVO> getFollowers(Long userId) {
        List<WfFollow> followers = listActiveFollowRecordsByFollowee(userId);
        Set<Long> followingSet = getFollowingIds(userId);
        List<Long> userIds = followers.stream()
                .map(WfFollow::getFollowerId)
                .collect(Collectors.toList());
        return buildFollowUsers(userIds, followingSet);
    }

    /**
     * 获取互关列表
     */
    public List<FollowUserVO> getMutual(Long userId) {
        List<WfFollow> following = listActiveFollowRecordsByFollower(userId);
        Set<Long> followerSet = getFollowerIds(userId);
        List<Long> mutualUserIds = following.stream()
                .map(WfFollow::getFolloweeId)
                .filter(followerSet::contains)
                .collect(Collectors.toList());
        if (mutualUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, WfUser> userMap = userService.getUserMap(mutualUserIds);
        List<FollowUserVO> result = new ArrayList<>();
        for (Long id : mutualUserIds) {
            WfUser user = userMap.get(id);
            if (user != null) {
                result.add(toFollowUserVO(user, true));
            }
        }
        return result;
    }

    /**
     * 判断是否互相关注
     */
    public boolean isMutualFollow(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            return false;
        }
        boolean user1FollowsUser2 = findActiveFollowRecord(userId1, userId2) != null;
        boolean user2FollowsUser1 = findActiveFollowRecord(userId2, userId1) != null;
        return user1FollowsUser2 && user2FollowsUser1;
    }

    private WfFollow findFollowRecord(Long followerId, Long followeeId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, followerId)
                .eq(WfFollow::getFolloweeId, followeeId)
                .last("LIMIT 1");
        return wfFollowMapper.selectOne(queryWrapper);
    }

    private WfFollow findActiveFollowRecord(Long followerId, Long followeeId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, followerId)
                .eq(WfFollow::getFolloweeId, followeeId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .last("LIMIT 1");
        return wfFollowMapper.selectOne(queryWrapper);
    }

    private List<WfFollow> listActiveFollowRecordsByFollower(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .orderByDesc(WfFollow::getCreateTime);
        return wfFollowMapper.selectList(queryWrapper);
    }

    private List<WfFollow> listActiveFollowRecordsByFollowee(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFolloweeId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .orderByDesc(WfFollow::getCreateTime);
        return wfFollowMapper.selectList(queryWrapper);
    }

    private Set<Long> getFollowingIds(Long userId) {
        return listActiveFollowRecordsByFollower(userId).stream()
                .map(WfFollow::getFolloweeId)
                .collect(Collectors.toSet());
    }

    private Set<Long> getFollowerIds(Long userId) {
        return listActiveFollowRecordsByFollowee(userId).stream()
                .map(WfFollow::getFollowerId)
                .collect(Collectors.toSet());
    }

    private List<FollowUserVO> buildFollowUsers(List<Long> userIds, Set<Long> mutualBase) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, WfUser> userMap = userService.getUserMap(userIds);

        List<FollowUserVO> result = new ArrayList<>(userIds.size());
        for (Long id : userIds) {
            WfUser user = userMap.get(id);
            if (user != null) {
                boolean mutual = mutualBase.contains(user.getId());
                result.add(toFollowUserVO(user, mutual));
            }
        }
        return result;
    }

    private void updateFollowStatusById(Long followId, FollowStatus status) {
        LambdaUpdateWrapper<WfFollow> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfFollow::getId, followId)
                .set(WfFollow::getStatus, status);
        int updateRows = wfFollowMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private FollowUserVO toFollowUserVO(WfUser user, boolean mutual) {
        FollowUserVO vo = new FollowUserVO();
        vo.setUserId(user.getId());
        vo.setWolfNo(user.getWolfNo());
        vo.setNickname(user.getNickname());
        vo.setEquippedTitleName(user.getEquippedTitleName());
        vo.setEquippedTitleColor(user.getEquippedTitleColor());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setMutual(mutual);
        return vo;
    }

    private void notifyChatFollowEvents(Long followerId, Long followeeId) {
        Long conversationId = conversationService.findConversationId(followerId, followeeId);
        if (conversationId == null) {
            return;
        }
        chatSystemNoticeService.sendFollowReceivedNotice(conversationId, followeeId);
        if (isMutualFollow(followerId, followeeId)) {
            chatSystemNoticeService.sendMutualFollowNotice(conversationId, followerId, followeeId);
        }
    }
}
